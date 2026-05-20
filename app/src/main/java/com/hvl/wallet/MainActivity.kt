package com.hvl.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var wm: WalletManager? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val priceText = findViewById<TextView>(R.id.priceText)
        val balanceText = findViewById<TextView>(R.id.balanceText)
        val addressText = findViewById<TextView>(R.id.addressText)
        val historyList = findViewById<ListView>(R.id.historyList)
        val receiveBtn = findViewById<Button>(R.id.receiveBtn)
        val sendBtn = findViewById<Button>(R.id.sendBtn)
        val manageBtn = findViewById<Button>(R.id.manageBtn)
        val seedBtn = findViewById<Button>(R.id.seedBtn)

        // Hiện UI trước, load wallet sau
        priceText.text = "BTC: $0"
        balanceText.text = "Đang tải..."
        addressText.text = "..."

        try {
            wm = WalletManager(this)
            wm?.start()
            addressText.text = wm?.getCurrentAddress() ?: "No address"
            balanceText.text = wm?.getBalance() ?: "0 BTC"
            historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, 
                wm?.getTransactionHistory() ?: listOf("Chưa có giao dịch"))
        } catch (e: Exception) {
            addressText.text = "Lỗi wallet"
            balanceText.text = "0 BTC"
            Toast.makeText(this, "Wallet: ${e.message}", Toast.LENGTH_LONG).show()
        }

        scope.launch {
            try {
                val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
                priceText.text = "BTC: $${"%,.0f".format(price)}"
            } catch (_: Exception) {}
        }

        receiveBtn.setOnClickListener { startActivity(Intent(this, ReceiveActivity::class.java)) }
        sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        seedBtn.setOnClickListener { 
            Toast.makeText(this, wm?.getSeedPhrase() ?: "No seed", Toast.LENGTH_LONG).show() 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm?.stop()
        scope.cancel()
    }
}