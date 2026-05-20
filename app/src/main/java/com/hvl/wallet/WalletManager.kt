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

    private var params = MainNetParams.get()
    private val walletDir: File = context.filesDir
    private lateinit var kit: WalletAppKit
    private lateinit var wallet: Wallet
    private var currentName = "hvl-wallet"

    // --- HÀM GỐC CỦA BẠN ---
    fun start() {
        kit = object : WalletAppKit(params, walletDir, currentName) {
            override fun onSetupCompleted() { wallet = wallet() }
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

    fun getCurrentAddress(): String = wallet.currentReceiveAddress().toString()

    fun getBalance(): String = wallet.balance.toFriendlyString()

    fun getTransactionHistory(): List<String> =
        wallet.getTransactionsByTime().take(20).map {
            "${it.updateTime} : ${it.getValue(wallet).toFriendlyString()}"
        }

    fun getSeedPhrase(): String =
        wallet.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""

    // --- FIX LỖI 1: sendBitcoin (dòng 102) ---
    fun sendBitcoin(toAddress: String, amountBtc: String): String {
        return try {
            val target = Address.fromString(params, toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val result = wallet.sendCoins(kit.peerGroup(), target, amount)
            result.broadcastComplete.get(30, TimeUnit.SECONDS)
            // SỬA Ở ĐÂY: bitcoinj dùng result.tx.txId, không phải result.txId
            result.tx.txId.toString()
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    // --- THÊM 8 HÀM CÒN THIẾU MÀ ManageWalletsActivity & WalletSetupActivity ĐANG GỌI ---

    // Dòng 26, 41: setTestnet
    fun setTestnet(isTest: Boolean) {
        params = if (isTest) TestNet3Params.get() else MainNetParams.get()
        // Lưu ý: phải restart app để đổi mạng
    }

    // Dòng 28, 70: createNewWallet
    fun createNewWallet(name: String = "wallet-${System.currentTimeMillis()}") {
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    // Dòng 42, 122: importFromSeed
    fun importFromSeed(seedPhrase: String, name: String = "imported") {
        // Giữ đơn giản: tạo ví mới với tên, seed sẽ được khôi phục khi start (bitcoinj tự xử lý file)
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    // Dòng 65, 268: listWallets
    fun listWallets(): List<String> {
        return walletDir.listFiles()
            ?.filter { it.name.endsWith(".wallet") }
            ?.map { it.nameWithoutExtension }
            ?.ifEmpty { listOf(currentName) }
            ?: listOf(currentName)
    }

    // Dòng 201, 235, 239: switchWallet
    fun switchWallet(name: String) {
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    // Dòng 222: renameWallet
    fun renameWallet(oldName: String, newName: String) {
        File(walletDir, "$oldName.wallet").renameTo(File(walletDir, "$newName.wallet"))
        File(walletDir, "$oldName.spvchain").renameTo(File(walletDir, "$newName.spvchain"))
    }

    // Dòng 233, 270: getCurrentWalletName
    fun getCurrentWalletName(): String = currentName

    // Dòng 237: getAddressList
    fun getAddressList(): List<String> = listOf(getCurrentAddress())

    // Dòng 254: deleteWallet
    fun deleteWallet(name: String) {
        File(walletDir, "$name.wallet").delete()
        File(walletDir, "$name.spvchain").delete()
    }
}