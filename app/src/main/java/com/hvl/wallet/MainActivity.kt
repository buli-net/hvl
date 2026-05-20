package com.hvl.wallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private var wm: WalletManager? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }
        scroll.addView(layout)

        val priceText = TextView(this).apply { textSize = 18f; setTextColor(Color.BLACK); text = "BTC: $0" }
        val balanceText = TextView(this).apply { textSize = 26f; setTextColor(Color.BLACK); setPadding(0,20,0,0); text = "Đang tải..." }
        val addressText = TextView(this).apply { textSize = 14f; setTextColor(Color.BLACK); setPadding(0,20,0,0); text = "..." }

        fun btn(t: String) = Button(this).apply { text = t }
        val receiveBtn = btn("Nhận")
        val sendBtn = btn("Gửi")
        val newAddressBtn = btn("Tạo địa chỉ mới")  // <--- NÚT GỐC
        val manageBtn = btn("Quản lý ví")
        val seedBtn = btn("Seed")
        val historyList = ListView(this)

        layout.addView(priceText)
        layout.addView(balanceText)
        layout.addView(addressText)
        layout.addView(receiveBtn)
        layout.addView(sendBtn)
        layout.addView(newAddressBtn)
        layout.addView(manageBtn)
        layout.addView(seedBtn)
        layout.addView(historyList, LinearLayout.LayoutParams(-1, 600))

        setContentView(scroll)

        try {
            wm = WalletManager(this)
            wm?.start()
            addressText.text = wm?.getCurrentAddress()
            balanceText.text = wm?.getBalance()
            historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                wm?.getTransactionHistory() ?: listOf("Chưa có giao dịch"))
        } catch (e: Exception) {
            addressText.text = "Lỗi: ${e.message}"
        }

        scope.launch {
            try {
                val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
                priceText.text = "BTC: $${"%,.0f".format(price)}"
            } catch (_: Exception) {}
        }

        receiveBtn.setOnClickListener { startActivity(Intent(this, ReceiveActivity::class.java)) }
        sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        newAddressBtn.setOnClickListener { 
            wm?.createNewAddress()
            addressText.text = wm?.getCurrentAddress()
            Toast.makeText(this, "Đã tạo địa chỉ mới", Toast.LENGTH_SHORT).show()
        }
        manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        seedBtn.setOnClickListener { Toast.makeText(this, wm?.getSeedPhrase() ?: "No seed", Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm?.stop()
        scope.cancel()
    }
}