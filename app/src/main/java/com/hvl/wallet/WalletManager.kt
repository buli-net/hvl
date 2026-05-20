// FILE: WalletManager.kt
// TÁC DỤNG: Quản lý ví Bitcoin MAINNET thật bằng bitcoinj 0.16.2

package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(private val context: Context) {

    private lateinit var walletKit: WalletAppKit
    // MAINNET = mạng Bitcoin thật
    private val params: NetworkParameters = MainNetParams.get()

    // Khởi tạo ví
    fun start() {
        val walletDir = File(context.filesDir, "bitcoin")
        if (!walletDir.exists()) walletDir.mkdirs()

        walletKit = WalletAppKit(params, walletDir, "hvl-wallet")
        walletKit.setAutoSave(true)
        walletKit.setBlockingStartup(false)
        walletKit.startAsync()
        walletKit.awaitRunning()

        // BỎ DÒNG setAllowSpendingUnconfirmedTransactions
        // vì bitcoinj 0.16.2 đã xóa hàm này - ví vẫn nhận và gửi bình thường
    }

    // Lấy ví
    private fun getWallet(): Wallet? {
        return if (::walletKit.isInitialized) walletKit.wallet() else null
    }

    // Địa chỉ hiện tại (dùng lại)
    fun getCurrentAddress(): String {
        return getWallet()?.currentReceiveAddress()?.toString() ?: "Đang khởi tạo..."
    }

    // Tạo địa chỉ mới
    fun getNewAddress(): String {
        return getWallet()?.freshReceiveAddress()?.toString() ?: getCurrentAddress()
    }

    // Số dư
    fun getBalance(): String {
        return getWallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toFriendlyString() ?: "0 BTC"
    }

    // Dừng ví
    fun stop() {
        if (::walletKit.isInitialized) {
            walletKit.stopAsync()
            walletKit.awaitTerminated()
        }
    }
}