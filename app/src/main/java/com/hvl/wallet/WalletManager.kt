// FILE: app/src/main/java/com/hvl/wallet/WalletManager.kt
// TÁC DỤNG: Quản lý toàn bộ ví Bitcoin - tạo ví, import, gửi nhận, đa ví, testnet
// PHIÊN BẢN: 2.1 - Thêm renameWallet và switchWallet, fix bc1q

package com.hvl.wallet

import android.content.Context
import import android.content.SharedPreferences
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

    // SharedPreferences lưu tên ví đang dùng và chế độ testnet
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    
    // WalletAppKit quản lý blockchain và ví
    private var walletKit: WalletAppKit? = null

    // Kiểm tra có đang ở chế độ testnet không
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    
    // Lấy tham số mạng: MainNet hoặc TestNet3
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    
    // Lấy tên ví hiện tại đang dùng
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // KHỞI ĐỘNG VÍ - hàm quan trọng nhất
    fun start() {
        val walletName = getCurrentWalletName()
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs() // Tạo thư mục nếu chưa có

        // Tạo WalletAppKit
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        walletKit?.setAutoSave(true) // Tự động lưu ví
        walletKit?.setBlockingStartup(false) // Không chặn UI
        walletKit?.startAsync() // Chạy bất đồng bộ
        walletKit?.awaitRunning() // Đợi ví sẵn sàng

        // ÉP VÍ DÙNG ĐỊA CHỈ SEGWIT BC1Q (phí rẻ hơn 40%)
        try {
            val wallet = walletKit?.wallet()
            // Nâng cấp ví lên định dạng P2WPKH (bc1q)
            wallet?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {
            // Bỏ qua nếu lỗi (ví cũ)
        }
    }

    // Lấy đối tượng Wallet hiện tại
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // TẠO VÍ MỚI
    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply() // Lưu tên ví mới
        walletKit?.stopAsync() // Dừng ví cũ
        start() // Khởi động lại với ví mới
    }

    // IMPORT VÍ TỪ SEED 12 TỪ
    fun importFromSeed(name: String, seedPhrase: String) {
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        val walletDir = File(context.filesDir, "bitcoin")
        walletKit?.stopAsync()
        walletKit = WalletAppKit(getParams(), walletDir, name)
        walletKit?.restoreWalletFromSeed(seed) // Khôi phục từ seed
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    // XÓA VÍ
    fun deleteWallet(name: String) {
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        if (file.exists()) file.delete()
    }

    // LIỆT KÊ TẤT CẢ VÍ
    fun listWallets(): List<String> {
        val dir = File(context.filesDir, "bitcoin")
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    // BẬT/TẮT TESTNET
    fun setTestnet(useTestnet: Boolean) {
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // LẤY ĐỊA CHỈ HIỆN TẠI (bc1q...)
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    
    // TẠO ĐỊA CHỈ MỚI
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    
    // LẤY SỐ DƯ
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    
    // LẤY SEED 12 TỪ
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // LẤY LỊCH SỬ GIAO DỊCH
    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

    // GỬI BITCOIN
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            val target = Address.fromString(getParams(), toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val req = SendRequest.to(target, amount)
            req.feePerKb = feePerKb // Đặt phí tùy chỉnh
            val result = wallet.sendCoins(req)
            result.broadcastComplete.get() // Đợi broadcast
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    // FIX LỖI 2: ĐỔI TÊN VÍ - rename file thật
    fun renameWallet(oldName: String, newName: String): Boolean {
        return try {
            val dir = File(context.filesDir, "bitcoin")
            val oldFile = File(dir, "$oldName.wallet")
            val newFile = File(dir, "$newName.wallet")
            if (oldFile.exists()) {
                val success = oldFile.renameTo(newFile)
                // Nếu đang dùng ví cũ, chuyển sang tên mới
                if (getCurrentWalletName() == oldName) {
                    prefs.edit().putString("current_wallet", newName).apply()
                }
                success
            } else false
        } catch (e: Exception) { false }
    }

    // FIX LỖI 3: CHỌN VÍ ĐANG DÙNG
    fun switchWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply()
    }

    // DỪNG VÍ
    fun stop() {
        walletKit?.stopAsync()
        walletKit?.awaitTerminated()
    }
}