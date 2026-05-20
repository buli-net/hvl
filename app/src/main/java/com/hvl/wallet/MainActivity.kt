package com.hvl.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var wm: WalletManager? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var priceText: TextView
    private lateinit var balanceText: TextView
    private lateinit var addressText: TextView
    private lateinit var historyList: ListView
    private lateinit var receiveBtn: Button
    private lateinit var sendBtn: Button
    private lateinit var manageBtn: Button
    private lateinit var seedBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // QUAN TRỌNG: không dùng binding nữa
        setContentView(R.layout.activity_main)

        priceText = findViewById(R.id.priceText)
        balanceText = findViewById(R.id.balanceText)
        addressText = findViewById(R.id.addressText)
        historyList = findViewById(R.id.historyList)
        receiveBtn = findViewById(R.id.receiveBtn)
        sendBtn = findViewById(R.id.sendBtn)
        manageBtn = findViewById(R.id.manageBtn)
        seedBtn = findViewById(R.id.seedBtn)

        // Test hiển thị trước
        priceText.text = "BTC: $0"
        balanceText.text = "TEST 0.123 BTC"
        addressText.text = "bc1q test address"
        historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("Tx1", "Tx2"))

        // Thử khởi tạo wallet sau (nếu lỗi vẫn thấy UI)
        try {
            wm = WalletManager(this)
            wm?.start()
            addressText.text = wm?.getCurrentAddress()
            balanceText.text = wm?.getBalance()
        } catch (e: Exception) {
            Toast.makeText(this, "Wallet lỗi: ${e.message}", Toast.LENGTH_LONG).show()
        }

        receiveBtn.setOnClickListener { startActivity(Intent(this, ReceiveActivity::class.java)) }
        sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        seedBtn.setOnClickListener { Toast.makeText(this, wm?.getSeedPhrase() ?: "No seed", Toast.LENGTH_LONG).show() }

        scope.launch {
            try {
                val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
                priceText.text = "BTC: $${"%,.0f".format(price)}"
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm?.stop()
        scope.cancel()
    }
}