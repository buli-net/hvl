// FILE: WalletManager.kt
// TÁC DỤNG: Bộ não ví Bitcoin MAINNET - tạo khóa, địa chỉ bc1, quản lý số dư

package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.Wallet
import java.io.File

class WalletManager(context: Context, walletFileName: String) {

    // BƯỚC 1: Chọn mạng BITCOIN MAINNET (tiền thật!)
    // Đổi thành TestNet3Params.get() nếu bạn muốn test
    private val params: NetworkParameters = MainNetParams.get()

    // BƯỚC 2: Thư mục lưu ví trong app
    private val walletDir: File = context.filesDir

    // BƯỚC 3: WalletAppKit tự động đồng bộ blockchain
    private val kit: WalletAppKit

    init {
        // Khởi tạo kit - đây là cách chuẩn của bitcoinj
        kit = object : WalletAppKit(params, walletDir, walletFileName) {
            override fun onSetupCompleted() {
                // Khi ví đã sẵn sàng
                wallet().allowSpendingUnconfirmedTransactions()
            }
        }

        // Cấu hình cho mainnet thật
        kit.setAutoSave(true)  // Tự lưu ví
        kit.setBlockingStartup(false) // Không chặn UI
        kit.startAsync() // Chạy ngầm để đồng bộ
    }

    // Lấy địa chỉ hiện tại dạng bech32 (bc1...)
    fun getCurrentAddress(): String {
        val wallet: Wallet = kit.wallet()
        // bitcoinj 0.16 tự động tạo địa chỉ SegWit bc1
        return wallet.currentReceiveAddress().toString()
    }

    // Tạo địa chỉ mới
    fun getNewAddress(): String {
        val wallet: Wallet = kit.wallet()
        // Tạo key mới -> địa chỉ mới
        val newAddress = wallet.freshReceiveAddress()
        return newAddress.toString()
    }

    // Lấy số dư BTC
    fun getBalance(): Double {
        val wallet: Wallet = kit.wallet()
        // Chuyển từ satoshi sang BTC
        return wallet.balance.value.toDouble() / 100_000_000.0
    }

    // Dừng kit khi đóng app (tiết kiệm pin)
    fun stop() {
        kit.stopAsync()
    }
}