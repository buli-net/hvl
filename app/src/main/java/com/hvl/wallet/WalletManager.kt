package com.hvl.wallet

import android.content.Context
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.util.concurrent.TimeUnit

class WalletManager(private val context: Context) {
    private var params: NetworkParameters = MainNetParams.get()
    private val walletDir = context.filesDir
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

    fun getCurrentAddress() = wallet.currentReceiveAddress().toString()
    fun getBalance() = wallet.balance.toFriendlyString()
    fun getTransactionHistory() = wallet.getTransactionsByTime().take(20).map {
        "${it.updateTime} : ${it.getValue(wallet).toFriendlyString()}"
    }
    fun getSeedPhrase() = wallet.keyChainSeed?.mnemonicCode?.joinToString(" ") ?: ""
    fun sendBitcoin(to: String, amount: String): String = try {
        val tx = wallet.sendCoins(kit.peerGroup(), Address.fromString(params, to), Coin.parseCoin(amount))
        tx.broadcastComplete.get(30, TimeUnit.SECONDS)
        tx.tx.txId.toString()
    } catch (e: Exception) { "Lỗi: ${e.message}" }

    fun setTestnet(isTest: Boolean) {
        params = if (isTest) TestNet3Params.get() else MainNetParams.get()
    }
    fun listWallets() = walletDir.listFiles()?.filter { it.name.endsWith(".wallet") }?.map { it.nameWithoutExtension } ?: listOf(currentName)
    fun switchWallet(name: String) { currentName = name; stop(); start() }
    fun createNewWallet(name: String) { currentName = name; stop(); start() }
    fun importFromSeed(seed: String, name: String) { currentName = name; stop(); start() }
    fun renameWallet(old: String, new: String): Boolean {
        val a = File(walletDir, "$old.wallet").renameTo(File(walletDir, "$new.wallet"))
        val b = File(walletDir, "$old.spvchain").renameTo(File(walletDir, "$new.spvchain"))
        return a && b
    }
    fun deleteWallet(name: String) {
        File(walletDir, "$name.wallet").delete()
        File(walletDir, "$name.spvchain").delete()
    }
    fun getCurrentWalletName() = currentName
    fun getAddressList() = listOf(getCurrentAddress())
}