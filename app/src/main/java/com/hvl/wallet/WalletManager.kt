// FILE: app/src/main/java/com/hvl/wallet/WalletManager.kt
// TÁC DỤNG: Quản lý ví Bitcoin - tạo, import, gửi, nhận, đa ví, bc1q
// FIX: Thêm đủ dấu } để 3 hàm cuối compile được

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

    // Lưu cài đặt ví
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    private var walletKit: WalletAppKit? = null

    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // Khởi động ví
    fun start() {
        val walletName = getCurrentWalletName()
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        walletKit?.setAutoSave(true)
        walletKit?.setBlockingStartup(false)
        walletKit?.startAsync()
        walletKit?.awaitRunning()
        // Ép dùng bc1q
        try {
            walletKit?.wallet()?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {}
    }

    private fun getWallet(): Wallet? = walletKit?.wallet()

    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply()
        walletKit?.stopAsync()
        start()
    }

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

    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

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

    // ĐỔI TÊN VÍ
    fun renameWallet(oldName: String, newName: String): Boolean {
        return try {
            val dir = File(context.filesDir, "bitcoin")
            val oldFile = File(dir, "$oldName.wallet")
            val newFile = File(dir, "$newName.wallet")
            if (oldFile.exists()) {
                val ok = oldFile.renameTo(newFile)
                if (getCurrentWalletName() == oldName) {
                    prefs.edit().putString("current_wallet", newName).apply()
                }
                ok
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // CHỌN VÍ
    fun switchWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply()
    }

    // DỪNG VÍ
    fun stop() {
        walletKit?.stopAsync()
        walletKit?.awaitTerminated()
    }
} // <-- Đủ dấu đóng class