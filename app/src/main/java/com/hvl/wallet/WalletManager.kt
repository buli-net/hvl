// FILE: WalletManager.kt
// TÁC DỤNG: Quản lý ví Bitcoin MAINNET thật bằng thư viện bitcoinj

package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(private val context: Context) {

    private lateinit var walletKit: WalletAppKit
    // DÙNG MAINNET - đây là mạng Bitcoin thật, không phải testnet
    private val params: NetworkParameters = MainNetParams.get()

    // Khởi tạo và đồng bộ ví
    fun start() {
        // Thư mục lưu ví trong bộ nhớ app
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()

        // Tạo WalletAppKit với tên file "hvl-wallet"
        walletKit = WalletAppKit(params, walletDir, "hvl-wallet")
        walletKit.setAutoSave(true)
        walletKit.setBlockingStartup(false)
        walletKit.startAsync()
        walletKit.awaitRunning()

        // SỬA LỖI Ở ĐÂY - dòng 30
        // bitcoinj 0.16.2 không còn hàm allowSpendingUnconfirmedTransactions()
        // Phải dùng setAllowSpendingUnconfirmedTransactions(true)
        walletKit.wallet()?.setAllowSpendingUnconfirmedTransactions(true)
    }

    // Lấy đối tượng ví
    fun getWallet(): Wallet? {
        return if (::walletKit.isInitialized) walletKit.wallet() else null
    }

    // Lấy địa chỉ nhận BTC (bắt đầu bằng bc1...)
    fun getReceiveAddress(): String {
        return getWallet()?.currentReceiveAddress()?.toString() ?: "Đang khởi tạo..."
    }

    // Dừng ví khi thoát app
    fun stop() {
        if (::walletKit.isInitialized) {
            walletKit.stopAsync()
            walletKit.awaitTerminated()
        }
    }
}