// Package của app
package com.hvl.wallet

// Import Context để truy cập file
import android.content.Context
// Import SharedPreferences để lưu cài đặt
import android.content.SharedPreferences
// Import Address bitcoinj
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
// Import Script để ép bc1q
import org.bitcoinj.script.Script
// Import DeterministicSeed
import org.bitcoinj.wallet.DeterministicSeed
// Import SendRequest
import org.bitcoinj.wallet.SendRequest
// Import Wallet
import org.bitcoinj.wallet.Wallet
// Import File
import java.io.File

// Class quản lý ví
class WalletManager(private val context: Context) {

    // Tạo SharedPreferences tên wallets
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    // Biến giữ WalletAppKit
    private var walletKit: WalletAppKit? = null

    // Hàm kiểm tra testnet
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    // Hàm lấy params mạng
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    // Hàm lấy tên ví hiện tại
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1")?: "wallet1"

    // Hàm khởi động ví
    fun start() {
        // Lấy tên ví
        val walletName = getCurrentWalletName()
        // Tạo thư mục bitcoin
        val walletDir = File(context.filesDir, "bitcoin")
        // Nếu chưa có thì tạo
        if (!walletDir.exists()) walletDir.mkdirs()
        // Khởi tạo WalletAppKit
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        // Bật autosave
        walletKit?.setAutoSave(true)
        // Không chặn UI
        walletKit?.setBlockingStartup(false)
        // Chạy async
        walletKit?.startAsync()
        // Đợi chạy xong
        walletKit?.awaitRunning()
        // Thử ép địa chỉ bc1q
        try {
            walletKit?.wallet()?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {}
        // Lấy địa chỉ hiện tại
        val addr = getWallet()?.currentReceiveAddress()?.toString()
        // Nếu có địa chỉ
        if (addr!= null) {
            // Key lưu danh sách địa chỉ
            val key = "addresses_$walletName"
            // Lấy set cũ
            val oldSet = prefs.getStringSet(key, mutableSetOf())?: mutableSetOf()
            // Tạo set mới
            val newSet = oldSet.toMutableSet()
            // Thêm địa chỉ
            newSet.add(addr)
            // Lưu lại
            prefs.edit().putStringSet(key, newSet).apply()
        }
    }

    // Hàm lấy ví
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // Hàm tạo ví mới
    fun createNewWallet(name: String) {
        // Lưu tên ví mới
        prefs.edit().putString("current_wallet", name).apply()
        // Dừng ví cũ
        walletKit?.stopAsync()
        // Start lại
        start()
    }

    // Hàm import từ seed
    fun importFromSeed(name: String, seedPhrase: String) {
        // Tạo seed từ 12 từ
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        // Thư mục ví
        val walletDir = File(context.filesDir, "bitcoin")
        // Dừng ví cũ
        walletKit?.stopAsync()
        // Tạo kit mới
        walletKit = WalletAppKit(getParams(), walletDir, name)
        // Khôi phục từ seed
        walletKit?.restoreWalletFromSeed(seed)
        // Lưu tên
        prefs.edit().putString("current_wallet", name).apply()
        // Start
        start()
    }

    // Hàm xóa ví
    fun deleteWallet(name: String) {
        // File ví
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        // Nếu tồn tại thì xóa
        if (file.exists()) file.delete()
        // Xóa danh sách địa chỉ
        prefs.edit().remove("addresses_$name").apply()
    }

    // Hàm liệt kê ví
    fun listWallets(): List<String> {
        // Thư mục
        val dir = File(context.filesDir, "bitcoin")
        // Lọc file.wallet
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
         ?.map { it.name.replace(".wallet", "") }?: emptyList()
    }

    // Hàm set testnet
    fun setTestnet(useTestnet: Boolean) {
        // Lưu boolean
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // Hàm lấy địa chỉ hiện tại
    fun getCurrentAddress(): String {
        // Trả về địa chỉ hoặc text mặc định
        return getWallet()?.currentReceiveAddress()?.toString()?: "Chưa có ví"
    }

    // Hàm lấy danh sách địa chỉ
    fun getAddressList(): List<String> {
        // Lấy tên ví
        val walletName = getCurrentWalletName()
        // Key
        val key = "addresses_$walletName"
        // Lấy set
        val set = prefs.getStringSet(key, emptySet())?: emptySet()
        // Trả list
        return set.toList()
    }

    // Hàm lấy số dư
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString()?: "0 BTC"
    // Hàm lấy seed
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ")?: ""

    // Hàm lấy lịch sử
    fun getTransactionHistory(): List<String> {
        // Lấy ví
        val wallet = getWallet()?: return emptyList()
        // Lấy 20 giao dịch
        return wallet.getTransactionsByTime().take(20).map { tx ->
            // Lấy giá trị
            val value = tx.getValue(wallet).toFriendlyString()
            // Trả chuỗi ngày + value
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

    // Hàm gửi coin - ĐÃ SỬA LỖI Ở ĐÂY
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        // Thử gửi
        return try {
            // Lấy ví
            val wallet = getWallet()?: return "Ví chưa sẵn sàng"
            // Parse địa chỉ
            val target = Address.fromString(getParams(), toAddress)
            // Parse số tiền
            val amount = Coin.parseCoin(amountBtc)
            // Tạo request
            val req = SendRequest.to(target, amount)
            // Set phí
            req.feePerKb = feePerKb
            // Gửi
            val result = wallet.sendCoins(req)
            // Đợi broadcast
            result.broadcastComplete.get()
            // SỬA: dùng result.tx.txId thay vì result.txId
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e: Exception) {
            // Trả lỗi
            "Lỗi: ${e.message}"
        }
    }

    // Hàm đổi tên ví
    fun renameWallet(oldName: String, newName: String): Boolean {
        // Thử
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
                val oldAddrs = prefs.getStringSet("addresses_$oldName", null)
                // Nếu có
                if (oldAddrs!= null) {
                    // Chuyển sang tên mới
                    prefs.edit().putStringSet("addresses_$newName", oldAddrs).remove("addresses_$oldName").apply()
                }
                // Nếu đang dùng ví cũ
                if (getCurrentWalletName() == oldName) {
                    // Cập nhật tên
                    prefs.edit().putString("current_wallet", newName).apply()
                }
                // Trả kết quả
                ok
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Hàm chuyển ví
    fun switchWallet(name: String) {
        // Lưu tên
        prefs.edit().putString("current_wallet", name).apply()
    }

    // Hàm dừng ví
    fun stop() {
        // Dừng async
        walletKit?.stopAsync()
        // Đợi dừng
        walletKit?.awaitTerminated()
    }
}