package com.hvl.wallet

// --- IMPORT ---
import android.content.Context // Lấy context ứng dụng
import org.bitcoinj.core.* // Thư viện BitcoinJ core
import org.bitcoinj.kits.WalletAppKit // Kit khởi tạo ví nhanh
import org.bitcoinj.params.MainNetParams // Tham số mạng Bitcoin mainnet
import org.bitcoinj.wallet.DeterministicSeed // Seed phrase
import org.bitcoinj.wallet.Wallet // Lớp ví
import java.io.File // Làm việc với file
import java.util.concurrent.TimeUnit // Chờ giao dịch

// Class quản lý toàn bộ ví
class WalletManager(private val context: Context) {

    // Tham số mạng mainnet
    private val params = MainNetParams.get()

    // Thư mục lưu ví
    private val walletDir = context.filesDir

    // Kit quản lý ví
    private lateinit var kit: WalletAppKit

    // Biến ví
    private lateinit var wallet: Wallet

    // Hàm khởi động ví
    fun start() {
        // Tạo WalletAppKit với tên file "hvl-wallet"
        kit = object : WalletAppKit(params, walletDir, "hvl-wallet") {
            override fun onSetupCompleted() {
                // Khi setup xong, gán ví
                wallet = wallet()
            }
        }
        // Cho phép tự động lưu
        kit.setAutoSave(true)
        // Bắt đầu đồng bộ blockchain
        kit.startAsync()
        // Chờ sẵn sàng
        kit.awaitRunning()
        // Gán ví sau khi chạy
        wallet = kit.wallet()
    }

    // Hàm dừng ví
    fun stop() {
        if (::kit.isInitialized) {
            kit.stopAsync() // Dừng bất đồng bộ
            kit.awaitTerminated() // Chờ dừng hẳn
        }
    }

    // Lấy địa chỉ hiện tại (dạng bech32)
    fun getCurrentAddress(): String {
        // Lấy địa chỉ nhận mới nhất
        val address = wallet.currentReceiveAddress()
        return address.toString() // Trả về chuỗi
    }

    // Lấy số dư
    fun getBalance(): String {
        // Lấy balance dạng Coin
        val balance = wallet.balance
        // Chuyển sang BTC và format
        return String.format("%.8f BTC", balance.toFriendlyString().replace(" BTC", "").toDouble())
    }

    // Lấy lịch sử giao dịch
    fun getTransactionHistory(): List<String> {
        // Lấy tất cả giao dịch
        val txs = wallet.getTransactionsByTime()
        // Chuyển thành list string
        return txs.map { tx ->
            val value = tx.getValue(wallet).toFriendlyString()
            val time = tx.updateTime?.toString() ?: "pending"
            "$time : $value"
        }.take(20) // Chỉ lấy 20 cái mới nhất
    }

    // Lấy seed phrase (12 từ)
    fun getSeedPhrase(): String {
        // Lấy seed từ ví
        val seed: DeterministicSeed? = wallet.keyChainSeed
        // Trả về mnemonic, nối bằng dấu cách
        return seed?.mnemonicCode?.joinToString(" ") ?: "Không có seed"
    }

    // HÀM GỬI BITCOIN - đây là hàm bị thiếu gây lỗi
    fun sendBitcoin(toAddress: String, amountBtc: String): String {
        return try {
            // Parse địa chỉ nhận
            val target = Address.fromString(params, toAddress)
            // Parse số lượng BTC thành Coin
            val amount = Coin.parseCoin(amountBtc)
            // Tạo yêu cầu gửi
            val result = wallet.sendCoins(kit.peerGroup(), target, amount)
            // Chờ broadcast hoàn tất
            result.broadcastComplete.get(30, TimeUnit.SECONDS)
            // Trả về txid
            result.txId.toString()
        } catch (e: Exception) {
            // Nếu lỗi, trả về thông báo
            "Lỗi: ${e.message}"
        }
    }
}