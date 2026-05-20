// FILE: app/src/main/java/com/hvl/wallet/WalletManager.kt
// TÁC DỤNG: Đây là "bộ não" của ví Bitcoin - quản lý tất cả việc tạo ví, gửi, nhận, đa ví
// PHIÊN BẢN: 2.3 - Full comment chi tiết

package com.hvl.wallet

// --- IMPORT THƯ VIỆN ANDROID ---
import android.content.Context // Cho phép truy cập file hệ thống
import android.content.SharedPreferences // Lưu cài đặt nhỏ (tên ví đang dùng)

// --- IMPORT THƯ VIỆN BITCOINJ ---
import org.bitcoinj.core.Address // Đại diện địa chỉ Bitcoin
import org.bitcoinj.core.Coin // Đại diện số tiền BTC
import org.bitcoinj.core.NetworkParameters // Tham số mạng (mainnet/testnet)
import org.bitcoinj.kits.WalletAppKit // Bộ công cụ khởi tạo ví nhanh
import org.bitcoinj.params.MainNetParams // Mạng Bitcoin thật
import org.bitcoinj.params.TestNet3Params // Mạng Bitcoin test
import org.bitcoinj.script.Script // Định nghĩa loại địa chỉ (bc1q, 1...)
import org.bitcoinj.wallet.DeterministicSeed // Seed 12 từ
import org.bitcoinj.wallet.SendRequest // Yêu cầu gửi tiền
import org.bitcoinj.wallet.Wallet // Đối tượng ví chính
import java.io.File // Thao tác file

// CLASS QUẢN LÝ VÍ
class WalletManager(private val context: Context) {

    // SharedPreferences: nơi lưu tên ví đang dùng, có bật testnet không
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    
    // WalletAppKit: quản lý blockchain, đồng bộ, lưu ví tự động
    private var walletKit: WalletAppKit? = null

    // Hàm kiểm tra có đang dùng testnet không
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    
    // Lấy tham số mạng: testnet thì dùng tiền ảo, mainnet thì tiền thật
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    
    // Lấy tên ví hiện tại, mặc định là "wallet1" nếu chưa có
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // HÀM KHỞI ĐỘNG VÍ - PHẢI GỌI TRƯỚC KHI DÙNG
    fun start() {
        val walletName = getCurrentWalletName() // Lấy tên ví cần mở
        val walletDir = File(context.filesDir, "bitcoin") // Thư mục lưu ví
        if (!walletDir.exists()) walletDir.mkdirs() // Tạo thư mục nếu chưa có

        // Tạo WalletAppKit
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        walletKit?.setAutoSave(true) // Tự lưu ví mỗi 5 giây
        walletKit?.setBlockingStartup(false) // Không chặn UI khi khởi động
        walletKit?.startAsync() // Chạy bất đồng bộ
        walletKit?.awaitRunning() // Đợi ví sẵn sàng

        // ÉP VÍ DÙNG ĐỊA CHỈ BC1Q (SegWit Native) - PHÍ RẺ HƠN 40%
        try {
            val wallet = walletKit?.wallet()
            // upgradeToDeterministic: nâng cấp ví để sinh địa chỉ bc1q thay vì 1...
            wallet?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {
            // Nếu lỗi thì bỏ qua, ví cũ vẫn dùng được
        }
    }

    // Lấy đối tượng Wallet (private để tránh gọi từ ngoài)
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // TẠO VÍ MỚI VỚI TÊN CHỈ ĐỊNH
    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply() // Lưu tên ví mới làm ví hiện tại
        walletKit?.stopAsync() // Dừng ví cũ để tránh xung đột
        start() // Khởi động lại với ví mới
    }

    // IMPORT VÍ TỪ SEED 12 TỪ
    fun importFromSeed(name: String, seedPhrase: String) {
        // Tạo đối tượng seed từ 12 từ
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        val walletDir = File(context.filesDir, "bitcoin")
        walletKit?.stopAsync()
        walletKit = WalletAppKit(getParams(), walletDir, name)
        walletKit?.restoreWalletFromSeed(seed) // Khôi phục ví từ seed
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    // XÓA VÍ VĨNH VIỄN
    fun deleteWallet(name: String) {
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        if (file.exists()) file.delete() // Xóa file .wallet
    }

    // LIỆT KÊ TẤT CẢ VÍ ĐÃ TẠO
    fun listWallets(): List<String> {
        val dir = File(context.filesDir, "bitcoin")
        // Lọc file kết thúc bằng .wallet và bỏ đuôi
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    // BẬT/TẮT TESTNET
    fun setTestnet(useTestnet: Boolean) {
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // LẤY ĐỊA CHỈ NHẬN HIỆN TẠI (sẽ là bc1q...)
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    
    // TẠO ĐỊA CHỈ MỚI (mỗi lần nhận nên dùng địa chỉ mới để bảo mật)
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    
    // LẤY SỐ DƯ ƯỚC TÍNH
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    
    // LẤY SEED 12 TỪ ĐỂ SAO LƯU
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // LẤY LỊCH SỬ GIAO DỊCH
    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        // Lấy 20 giao dịch gần nhất, sắp xếp theo thời gian
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString() // Số BTC nhận/gửi
            val date = java.util.Date(tx.updateTime.time) // Thời gian
            "$date: $value" // Format: ngày + số tiền
        }
    }

    // GỬI BITCOIN
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            val target = Address.fromString(getParams(), toAddress) // Chuyển string thành địa chỉ
            val amount = Coin.parseCoin(amountBtc) // Chuyển "0.001" thành Coin
            val req = SendRequest.to(target, amount) // Tạo yêu cầu gửi
            req.feePerKb = feePerKb // Đặt phí tùy chỉnh
            val result = wallet.sendCoins(req) // Gửi
            result.broadcastComplete.get() // Đợi broadcast lên mạng
            "Đã gửi! TX: ${result.tx.txId}" // Trả về mã giao dịch
        } catch (e: Exception) {
            "Lỗi: ${e.message}" // Bắt lỗi (sai địa chỉ, không đủ tiền...)
        }
    }

    // ĐỔI TÊN VÍ - RENAME FILE THẬT
    fun renameWallet(oldName: String, newName: String): Boolean {
        return try {
            val dir = File(context.filesDir, "bitcoin")
            val oldFile = File(dir, "$oldName.wallet")
            val newFile = File(dir, "$newName.wallet")
            if (oldFile.exists()) {
                val success = oldFile.renameTo(newFile) // Đổi tên file
                // Nếu đang dùng ví cũ, cập nhật tên mới
                if (getCurrentWalletName() == oldName) {
                    prefs.edit().putString("current_wallet", newName).apply()
                }
                success
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // CHỌN VÍ ĐANG DÙNG
    fun switchWallet(name: String) {
        // Chỉ cần lưu tên, MainActivity sẽ restart và load lại
        prefs.edit().putString("current_wallet", name).apply()
    }

    // DỪNG VÍ KHI THOÁT APP
    fun stop() {
        walletKit?.stopAsync() // Dừng bất đồng bộ
        walletKit?.awaitTerminated() // Đợi dừng hẳn
    }
}