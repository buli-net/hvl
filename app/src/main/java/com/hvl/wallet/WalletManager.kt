package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.util.concurrent.TimeUnit

class WalletManager(private val context: Context) {

    // --- BIẾN ---
    private var params = MainNetParams.get() // Mặc định mainnet
    private val walletDir = context.filesDir
    private lateinit var kit: WalletAppKit
    private lateinit var wallet: Wallet
    private var currentWalletName = "hvl-wallet"

    // --- KHỞI ĐỘNG ---
    fun start() {
        kit = object : WalletAppKit(params, walletDir, currentWalletName) {
            override fun onSetupCompleted() {
                wallet = wallet()
            }
        }
        kit.setAutoSave(true)
        kit.startAsync()
        kit.awaitRunning()
        wallet = kit.wallet()
    }

    fun stop() {
        if (::kit.isInitialized) {
            kit.stopAsync()
            kit.awaitTerminated()
        }
    }

    // --- CÁC HÀM CƠ BẢN MÀ MainActivity CẦN ---
    fun getCurrentAddress(): String {
        return wallet.currentReceiveAddress().toString()
    }

    fun getBalance(): String {
        val bal = wallet.balance.toFriendlyString()
        return bal
    }

    fun getTransactionHistory(): List<String> {
        return wallet.getTransactionsByTime().take(20).map {
            "${it.updateTime} : ${it.getValue(wallet).toFriendlyString()}"
        }
    }

    fun getSeedPhrase(): String {
        val seed: DeterministicSeed? = wallet.keyChainSeed
        return seed?.mnemonicCode?.joinToString(" ")?: ""
    }

    // --- HÀM GỬI - ĐÃ FIX LỖI txId ---
    fun sendBitcoin(toAddress: String, amountBtc: String): String {
        return try {
            val target = Address.fromString(params, toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val result = wallet.sendCoins(kit.peerGroup(), target, amount)
            result.broadcastComplete.get(30, TimeUnit.SECONDS)
            // SỬA Ở ĐÂY: dùng result.tx.txId thay vì result.txId
            result.tx.txId.toString()
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    // --- CÁC HÀM MÀ ManageWalletsActivity & WalletSetupActivity ĐANG GỌI ---
    // (thêm stub để build được, bạn sẽ phát triển sau)

    fun setTestnet(isTest: Boolean) {
        // Đổi mạng, phải restart app mới có hiệu lực
        params = if (isTest) TestNet3Params.get() else MainNetParams.get()
    }

    fun createNewWallet(name: String) {
        currentWalletName = name
        // Tạo ví mới (đơn giản: xóa kit cũ và start lại)
        if (::kit.isInitialized) stop()
        start()
    }

    fun importFromSeed(seedPhrase: String, name: String = "imported") {
        // Stub: thực tế cần tạo DeterministicSeed từ mnemonic
        currentWalletName = name
        if (::kit.isInitialized) stop()
        start()
    }

    fun listWallets(): List<String> {
        // Trả về danh sách file wallet trong thư mục
        return walletDir.listFiles()
           ?.filter { it.name.endsWith(".wallet") }
           ?.map { it.nameWithoutExtension }
           ?: listOf(currentWalletName)
    }

    fun getCurrentWalletName(): String = currentWalletName

    fun switchWallet(name: String) {
        currentWalletName = name
        if (::kit.isInitialized) stop()
        start()
    }

    fun renameWallet(oldName: String, newName: String) {
        // Stub đơn giản
        val oldFile = File(walletDir, "$oldName.wallet")
        val newFile = File(walletDir, "$newName.wallet")
        if (oldFile.exists()) oldFile.renameTo(newFile)
    }

    fun getAddressList(): List<String> {
        // Trả về 1 địa chỉ hiện tại (có thể mở rộng)
        return listOf(getCurrentAddress())
    }

    fun deleteWallet(name: String) {
        File(walletDir, "$name.wallet").delete()
        File(walletDir, "$name.spvchain").delete()
    }
}