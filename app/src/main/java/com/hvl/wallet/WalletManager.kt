// FILE: WalletManager.kt
// TÁC DỤNG: Quản lý ví Bitcoin

// Khai báo package
package com.hvl.wallet

// Import Context để truy cập file hệ thống
import android.content.Context
// Import SharedPreferences để lưu cài đặt
import android.content.SharedPreferences
// Import Address
import org.bitcoinj.core.Address
// Import Coin
import org.bitcoinj.core.Coin
// Import NetworkParameters
import org.bitcoinj.core.NetworkParameters
// Import WalletAppKit
import org.bitcoinj.kits.WalletAppKit
// Import MainNet
import org.bitcoinj.params.MainNetParams
// Import TestNet
import org.bitcoinj.params.TestNet3Params
// Import Script
import org.bitcoinj.script.Script
// Import DeterministicSeed
import org.bitcoinj.wallet.DeterministicSeed
// Import SendRequest
import org.bitcoinj.wallet.SendRequest
// Import Wallet
import org.bitcoinj.wallet.Wallet
// Import File
import java.io.File

// Khai báo class chính
class WalletManager(private val context: Context) {

    // Tạo SharedPreferences
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    // Biến giữ ví
    private var walletKit: WalletAppKit? = null

    // Kiểm tra testnet
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    // Lấy params mạng
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    // Lấy tên ví hiện tại
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // Hàm start ví
    fun start() {
        // Lấy tên ví
        val walletName = getCurrentWalletName()
        // Tạo thư mục
        val walletDir = File(context.filesDir, "bitcoin")
        // Tạo nếu chưa có
        if (!walletDir.exists()) walletDir.mkdirs()
        // Khởi tạo kit
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        // Bật autosave
        walletKit?.setAutoSave(true)
        // Không chặn UI
        walletKit?.setBlockingStartup(false)
        // Chạy async
        walletKit?.startAsync()
        // Đợi chạy
        walletKit?.awaitRunning()
        // Thử upgrade
        try {
            // Ép bc1q
            walletKit?.wallet()?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {
            // Bỏ qua lỗi
        }
    }

    // Lấy ví
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // Tạo ví mới
    fun createNewWallet(name: String) {
        // Lưu tên
        prefs.edit().putString("current_wallet", name).apply()
        // Dừng ví cũ
        walletKit?.stopAsync()
        // Start lại
        start()
    }

    // Import từ seed
    fun importFromSeed(name: String, seedPhrase: String) {
        // Tạo seed
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        // Thư mục
        val walletDir = File(context.filesDir, "bitcoin")
        // Dừng cũ
        walletKit?.stopAsync()
        // Tạo mới
        walletKit = WalletAppKit(getParams(), walletDir, name)
        // Khôi phục
        walletKit?.restoreWalletFromSeed(seed)
        // Lưu tên
        prefs.edit().putString("current_wallet", name).apply()
        // Start
        start()
    }

    // Xóa ví
    fun deleteWallet(name: String) {
        // File ví
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        // Xóa nếu có
        if (file.exists()) file.delete()
        // Xóa địa chỉ lưu
        prefs.edit().remove("last_address_$name").apply()
    }

    // Liệt kê ví
    fun listWallets(): List<String> {
        // Thư mục
        val dir = File(context.filesDir, "bitcoin")
        // Lọc file
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    // Set testnet
    fun setTestnet(useTestnet: Boolean) {
        // Lưu
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // Lấy địa chỉ hiện tại
    fun getCurrentAddress(): String {
        // Tên ví
        val walletName = getCurrentWalletName()
        // Đọc địa chỉ đã lưu
        val savedAddress = prefs.getString("last_address_$walletName", null)
        // Trả về
        return savedAddress ?: getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    }
    
    // Tạo địa chỉ mới
    fun getNewAddress(): String {
        // Lấy ví
        val wallet = getWallet() ?: return getCurrentAddress()
        // Tạo mới
        val newAddr = wallet.freshReceiveAddress().toString()
        // Tên ví
        val walletName = getCurrentWalletName()
        // Lưu
        prefs.edit().putString("last_address_$walletName", newAddr).apply()
        // Trả về
        return newAddr
    }
    
    // Lấy số dư
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    // Lấy seed
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // Lịch sử
    fun getTransactionHistory(): List<String> {
        // Lấy ví
        val wallet = getWallet() ?: return emptyList()
        // Map 20 tx
        return wallet.getTransactionsByTime().take(20).map { tx ->
            // Lấy value
            val value = tx.getValue(wallet).toFriendlyString()
            // Trả chuỗi
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

    // Gửi coin
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            // Lấy ví
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            // Parse địa chỉ
            val target = Address.fromString(getParams(), toAddress)
            // Parse tiền
            val amount = Coin.parseCoin(amountBtc)
            // Tạo request
            val req = SendRequest.to(target, amount)
            // Set phí
            req.feePerKb = feePerKb
            // Gửi
            val result = wallet.sendCoins(req)
            // Đợi
            result.broadcastComplete.get()
            // Trả txid
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e: Exception) {
            // Lỗi
            "Lỗi: ${e.message}"
        }
    }

    // Đổi tên
    fun renameWallet(oldName: String, newName: String): Boolean {
        return try {
            // Thư mục
            val dir = File(context.filesDir, "bitcoin")
            // File cũ
            val oldFile = File(dir, "$oldName.wallet")
            // File mới
            val newFile = File(dir, "$newName.wallet")
            // Nếu tồn tại
            if (oldFile.exists()) {
                // Đổi tên
                val ok = oldFile.renameTo(newFile)
                // Lấy địa chỉ cũ
                val savedAddr = prefs.getString("last_address_$oldName", null)
                // Nếu có
                if (savedAddr != null) {
                    // Chuyển
                    prefs.edit().putString("last_address_$newName", savedAddr).remove("last_address_$oldName").apply()
                }
                // Nếu đang dùng
                if (getCurrentWalletName() == oldName) {
                    // Cập nhật
                    prefs.edit().putString("current_wallet", newName).apply()
                }
                // Trả
                ok
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Chọn ví
    fun switchWallet(name: String) {
        // Lưu
        prefs.edit().putString("current_wallet", name).apply()
    }

    // Dừng
    fun stop() {
        // Dừng
        walletKit?.stopAsync()
        // Đợi
        walletKit?.awaitTerminated()
    }
}