// FILE: app/src/main/java/com/hvl/wallet/WalletManager.kt
// TÁC DỤNG: Quản lý toàn bộ ví Bitcoin - tạo ví, import, gửi nhận, đa ví, testnet
// PHIÊN BẢN: 2.2 - Fix lỗi import dư chữ

package com.hvl.wallet

// Import các thư viện Android
import android.content.Context
import android.content.SharedPreferences

// Import các thư viện BitcoinJ
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

// CLASS CHÍNH QUẢN LÝ VÍ
class WalletManager(private val context: Context) {

    // SharedPreferences để lưu tên ví đang dùng và chế độ testnet/mainnet
    private val prefs: SharedPreferences = context.getSharedPreferences("wallets", Context.MODE_PRIVATE)
    
    // WalletAppKit là đối tượng quản lý blockchain và ví của BitcoinJ
    private var walletKit: WalletAppKit? = null

    // Hàm kiểm tra có đang ở chế độ testnet không (testnet = tiền ảo để test)
    private fun isTestnet(): Boolean = prefs.getBoolean("is_testnet", false)
    
    // Lấy tham số mạng: nếu testnet thì dùng TestNet3, không thì MainNet (tiền thật)
    private fun getParams(): NetworkParameters = if (isTestnet()) TestNet3Params.get() else MainNetParams.get()
    
    // Lấy tên ví hiện tại đang dùng, mặc định là "wallet1"
    fun getCurrentWalletName(): String = prefs.getString("current_wallet", "wallet1") ?: "wallet1"

    // HÀM KHỞI ĐỘNG VÍ - quan trọng nhất
    fun start() {
        val walletName = getCurrentWalletName()
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs() // Tạo thư mục nếu chưa có

        // Tạo WalletAppKit với tên ví và thư mục
        walletKit = WalletAppKit(getParams(), walletDir, walletName)
        walletKit?.setAutoSave(true) // Tự động lưu ví mỗi khi thay đổi
        walletKit?.setBlockingStartup(false) // Không chặn giao diện
        walletKit?.startAsync() // Chạy bất đồng bộ
        walletKit?.awaitRunning() // Đợi ví sẵn sàng

        // ÉP VÍ DÙNG ĐỊA CHỈ SEGWIT BC1Q (phí rẻ hơn 40% so với địa chỉ 1...)
        try {
            val wallet = walletKit?.wallet()
            // Nâng cấp ví lên định dạng P2WPKH để sinh địa chỉ bc1q
            wallet?.upgradeToDeterministic(Script.ScriptType.P2WPKH, null)
        } catch (e: Exception) {
            // Nếu lỗi thì bỏ qua (ví cũ có thể không upgrade được)
        }
    }

    // Lấy đối tượng Wallet hiện tại
    private fun getWallet(): Wallet? = walletKit?.wallet()

    // TẠO VÍ MỚI VỚI TÊN CHỈ ĐỊNH
    fun createNewWallet(name: String) {
        prefs.edit().putString("current_wallet", name).apply() // Lưu tên ví mới
        walletKit?.stopAsync() // Dừng ví cũ
        start() // Khởi động lại với ví mới
    }

    // IMPORT VÍ TỪ SEED 12 TỪ
    fun importFromSeed(name: String, seedPhrase: String) {
        // Tạo seed từ 12 từ
        val seed = DeterministicSeed(seedPhrase, null, "", System.currentTimeMillis() / 1000)
        val walletDir = File(context.filesDir, "bitcoin")
        walletKit?.stopAsync()
        walletKit = WalletAppKit(getParams(), walletDir, name)
        walletKit?.restoreWalletFromSeed(seed) // Khôi phục ví từ seed
        prefs.edit().putString("current_wallet", name).apply()
        start()
    }

    // XÓA VÍ (xóa file .wallet)
    fun deleteWallet(name: String) {
        val file = File(context.filesDir, "bitcoin/$name.wallet")
        if (file.exists()) file.delete()
    }

    // LIỆT KÊ TẤT CẢ VÍ ĐÃ TẠO
    fun listWallets(): List<String> {
        val dir = File(context.filesDir, "bitcoin")
        return dir.listFiles()?.filter { it.name.endsWith(".wallet") }
            ?.map { it.name.replace(".wallet", "") } ?: emptyList()
    }

    // BẬT/TẮT CHẾ ĐỘ TESTNET
    fun setTestnet(useTestnet: Boolean) {
        prefs.edit().putBoolean("is_testnet", useTestnet).apply()
    }

    // LẤY ĐỊA CHỈ HIỆN TẠI (sẽ là bc1q...)
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Chưa có ví"
    
    // TẠO ĐỊA CHỈ MỚI
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    
    // LẤY SỐ DƯ ƯỚC TÍNH
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    
    // LẤY SEED 12 TỪ ĐỂ SAO LƯU
    fun getSeedPhrase(): String = getWallet()?.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // LẤY LỊCH SỬ GIAO DỊCH (20 giao dịch gần nhất)
    fun getTransactionHistory(): List<String> {
        val wallet = getWallet() ?: return emptyList()
        return wallet.getTransactionsByTime().take(20).map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            "${java.util.Date(tx.updateTime.time)}: $value"
        }
    }

    // GỬI BITCOIN ĐẾN ĐỊA CHỈ KHÁC
    fun sendCoins(toAddress: String, amountBtc: String, feePerKb: Coin): String {
        return try {
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            val target = Address.fromString(getParams(), toAddress) // Chuyển text thành địa chỉ
            val amount = Coin.parseCoin(amountBtc) // Chuyển BTC string thành Coin
            val req = SendRequest.to(target, amount)
            req.feePerKb = feePerKb // Đặt phí tùy chỉnh
            val result = wallet.sendCoins(req)
            result.broadcastComplete.get() // Đợi gửi lên mạng
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e