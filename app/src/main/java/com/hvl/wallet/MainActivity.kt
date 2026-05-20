package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.util.concurrent.TimeUnit

class WalletManager(private val context: Context) {

    // FIX 1: dùng NetworkParameters để chứa được cả Mainnet và Testnet
    private var params: NetworkParameters = MainNetParams.get()
    private val walletDir: File = context.filesDir
    private lateinit var kit: WalletAppKit
    private lateinit var wallet: Wallet
    private var currentName = "hvl-wallet"

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

    fun sendBitcoin(toAddress: String, amountBtc: String): String {
        return try {
            val target = Address.fromString(params, toAddress)
            val amount = Coin.parseCoin(amountBtc)
            val result = wallet.sendCoins(kit.peerGroup(), target, amount)
            result.broadcastComplete.get(30, TimeUnit.SECONDS)
            result.tx.txId.toString()
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    fun setTestnet(isTest: Boolean) {
        params = if (isTest) TestNet3Params.get() else MainNetParams.get()
    }

    fun createNewWallet(name: String = "wallet-${System.currentTimeMillis()}") {
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    fun importFromSeed(seedPhrase: String, name: String = "imported") {
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    fun listWallets(): List<String> {
        return walletDir.listFiles()
            ?.filter { it.name.endsWith(".wallet") }
            ?.map { it.nameWithoutExtension }
            ?.ifEmpty { listOf(currentName) }
            ?: listOf(currentName)
    }

    fun switchWallet(name: String) {
        currentName = name
        if (::kit.isInitialized) stop()
        start()
    }

    // FIX 2: trả về Boolean để khớp với ManageWalletsActivity.kt dòng 222
    fun renameWallet(oldName: String, newName: String): Boolean {
        val oldFile = File(walletDir, "$oldName.wallet")
        val newFile = File(walletDir, "$newName.wallet")
        val ok1 = oldFile.renameTo(newFile)
        val ok2 = File(walletDir, "$oldName.spvchain").renameTo(File(walletDir, "$newName.spvchain"))
        return ok1 && ok2
    }

    fun getCurrentWalletName(): String = currentName

    fun getAddressList(): List<String> = listOf(getCurrentAddress())

    fun deleteWallet(name: String) {
        File(walletDir, "$name.wallet").delete()
        File(walletDir, "$name.spvchain").delete()
    }
}