// FILE: WalletManager.kt
// TÁC DỤNG: Quản lý toàn bộ ví Bitcoin mainnet bằng bitcoinj

package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(private val context: Context) {

    // Biến lưu WalletAppKit (bộ công cụ ví của bitcoinj)
    private lateinit var walletKit: WalletAppKit
    // Chọn mạng MAINNET - đây là mạng Bitcoin thật, có giá trị
    private val params: NetworkParameters = MainNetParams.get()

    // Hàm khởi động ví - chạy lần đầu khi mở app
    fun start() {
        // Tạo thư mục lưu ví trong bộ nhớ app
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()

        // Khởi tạo WalletAppKit với tên file "hvl-wallet"
        walletKit = WalletAppKit(params, walletDir, "hvl-wallet")
        // Tự động lưu ví mỗi khi có thay đổi
        walletKit.setAutoSave(true)
        // Không chặn UI khi khởi động
        walletKit.setBlockingStartup(false)
        // Bắt đầu đồng bộ blockchain
        walletKit.startAsync()
        // Chờ cho ví sẵn sàng
        walletKit.awaitRunning()
    }

    // Lấy đối tượng Wallet, kiểm tra đã khởi tạo chưa
    private fun getWallet(): Wallet? = if (::walletKit.isInitialized) walletKit.wallet() else null

    // Lấy địa chỉ hiện tại để nhận tiền (dùng lại nhiều lần)
    fun getCurrentAddress(): String = getWallet()?.currentReceiveAddress()?.toString() ?: "Đang khởi tạo..."

    // Tạo địa chỉ mới hoàn toàn (nên dùng mỗi lần nhận để tăng bảo mật)
    fun getNewAddress(): String = getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()

    // Lấy số dư ước tính
    fun getBalance(): String = getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"

    // XUẤT SEED 12 TỪ - Đây là chìa khóa khôi phục ví, PHẢI GIỮ BÍ MẬT
    fun getSeedPhrase(): String {
        // Lấy seed từ ví
        val seed = getWallet()?.keyChainSeed
        // Chuyển 12 từ thành chuỗi cách nhau bằng dấu cách
        return seed?.mnemonicCode?.joinToString(" ") ?: "Chưa tạo ví"
    }

    // GỬI BITCOIN - cần địa chỉ nhận và số BTC
    fun sendCoins(toAddress: String, amountBtc: String): String {
        return try {
            // Lấy ví
            val wallet = getWallet() ?: return "Ví chưa sẵn sàng"
            // Chuyển chuỗi địa chỉ thành đối tượng Address
            val target = Address.fromString(params, toAddress)
            // Chuyển chuỗi BTC thành Coin (đơn vị của bitcoinj)
            val amount = Coin.parseCoin(amountBtc)
            // Tạo yêu cầu gửi
            val req = SendRequest.to(target, amount)
            // Gửi và chờ broadcast lên mạng
            val result = wallet.sendCoins(req)
            result.broadcastComplete.get()
            // Trả về ID giao dịch
            "Đã gửi! TX: ${result.tx.txId}"
        } catch (e: Exception) {
            // Bắt lỗi nếu địa chỉ sai hoặc không đủ tiền
            "Lỗi: ${e.message}"
        }
    }

    // Dừng ví khi thoát app để tránh lỗi file
    fun stop() {
        if (::walletKit.isInitialized) {
            walletKit.stopAsync()
            walletKit.awaitTerminated()
        }
    }
}