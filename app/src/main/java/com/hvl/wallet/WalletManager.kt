// FILE: app/src/main/java/com/hvl/wallet/WalletManager.kt
// TÁC DỤNG: Quản lý ví, hỗ trợ bc1q (SegWit), testnet, đa ví
// SỬA LỖI: bỏ setOutputScriptType không tồn tại, dùng cách đúng của bitcoinj 0.16.2

package com.hvl.wallet

import android.content.Context
import android.content.SharedPreferences
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(private val context: Context) {

    // Lưu cài đặt ví đang dùng
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    private var walletKit: WalletAppKit? = null

    // Kiểm tra có dùng testnet không
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    
    // Lấy tham số mạng
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    
    // Lấy tên ví hiện tại
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // KHỞI ĐỘNG VÍ
    fun start() {
        val walletName = getCurrentWalletName()
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()

        // Tạo WalletAppKit - bitcoinj 0.16.2 tự tạo địa chỉ SegWit nếu ví mới
        walletKit = object : WalletAppKit(getParams(), walletDir, walletName) {
            override fun onSetupCompleted() {
                // FIX: bitcoinj 0.16 không có setOutputScriptType, thay bằng cách thêm keychain mới
                // Nếu ví đã tồn tại địa chỉ legacy, nó vẫn dùng legacy
                // Ví MỚI sẽ tự động dùng P2WPKH (bc1q) từ version 0.16
                if (wallet().currentReceiveAddress().isP2PKH) {
                    // Ép tạo địa chỉ SegWit bằng cách upgrade
                    wallet().upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
                }
            }
        }
        walletKit?.setAutoSave(true)
        walletKit?.setBlockingStartup(false)
        walletKit?.startAsync()
        walletKit?.awaitRunning()
    }

    private fun getWallet(): Wallet? = walletKit?.wallet()

    // Tạo ví mới
    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply()
        // Xóa kit cũ nếu có
        walletKit?.stopAsync()
        start()
    }

    // Import từ seed
    fun importFromSeed(name: String, seedPhrase: String) {
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        val walletDir = File(context.filesDir, "bitcoin")
        walletKit?.stopAsync()
        walletKit = WalletAppKit(getParams(), walletDir, name)
        walletKit?.restoreWalletFromSeed(seed)
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    fun deleteWallet(name: String) {
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        if (file.exists()) file.delete()
    }

    fun listWallets(): List<String> {
        val dir = File(context.filesDir, "bitcoin")
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    fun setTestnet(useTestnet: Boolean) {
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // Lấy địa chỉ - sẽ trả về bc1q nếu ví mới
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // Lịch sử
    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

    // Gửi BTC
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            val target = Address.fromString(getParams(), toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val req = SendRequest.to(target, amount)
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