// FILE: WalletManager.kt
// TÁC DỤNG: Quản lý NHIỀU ví, hỗ trợ bc1q (SegWit), testnet/mainnet, lịch sử

package com.hvl.wallet

import android.content.Context
import android.content.SharedPreferences
import org.bitcoinj.core.*
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.util.*

class WalletManager(private val context: Context) {

    // Lưu cài đặt: ví đang dùng, mạng nào
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    
    // Biến lưu ví hiện tại
    private var walletKit: WalletAppKit? = null
    
    // Lấy mạng hiện tại: true = testnet, false = mainnet
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    
    // Lấy tham số mạng - QUAN TRỌNG: đổi sang testnet khi luyện tập
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    
    // Lấy tên ví đang dùng
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // KHỞI ĐỘNG VÍ - tạo ví bc1q SegWit
    fun start() {
        val walletName = getCurrentWalletName()
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()

        // Tạo WalletAppKit với ScriptType.P2WPKH = địa chỉ bc1q
        walletKit = object : WalletAppKit(getParams(), walletDir, walletName) {
            override fun onSetupCompleted() {
                // ÉP VÍ DÙNG BECH32 (bc1q) - phí rẻ hơn 40%
                wallet().setOutputScriptType(Script.ScriptType.P2WPKH)
            }
        }
        walletKit?.setAutoSave(true)
        walletKit?.setBlockingStartup(false)
        walletKit?.startAsync()
        walletKit?.awaitRunning()
    }

    // Lấy ví hiện tại
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // TẠO VÍ MỚI
    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    // IMPORT VÍ TỪ SEED 12 TỪ
    fun importFromSeed(name: String, seedPhrase: String) {
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        val walletDir = File(context.filesDir, "bitcoin")
        walletKit = WalletAppKit(getParams(), walletDir, name)
        walletKit?.restoreWalletFromSeed(seed)
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    // XÓA VÍ
    fun deleteWallet(name: String) {
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        if (file.exists()) file.delete()
    }

    // LẤY DANH SÁCH VÍ
    fun listWallets(): List<String> {
        val dir = File(context.filesDir, "bitcoin")
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    // ĐỔI MẠNG TESTNET/MAINNET
    fun setTestnet(useTestnet: Boolean) {
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // CÁC HÀM CŨ - giữ nguyên
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // LỊCH SỬ GIAO DỊCH
    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        // Lấy 20 giao dịch gần nhất
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            val time = Date(tx.updateTime.time)
            "${time}: $value"
        }
    }

    // GỬI BTC VỚI PHÍ TÙY CHỈNH
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            val target = Address.fromString(getParams(), toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val req = SendRequest.to(target, amount)
            // ĐẶT PHÍ TÙY CHỈNH
            req.feePerKb = feePerKb
            val result = wallet.sendCoins(req)
            result.broadcastComplete.get()
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    fun stop() {
        walletKit?.stopAsync()
        walletKit?.awaitTerminated()
    }
}