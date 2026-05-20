package com.hvl.wallet

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class SendActivity : AppCompatActivity() {
    // Tạo scope coroutine
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tạo layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
            setBackgroundColor(0xFF000000.toInt())
        }

        // Ô nhập địa chỉ
        val addressInput = EditText(this).apply {
            hint = "Địa chỉ bc1q..." // Gợi ý
            setHintTextColor(0xFFAAAAAA.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        // Ô nhập số lượng
        val amountInput = EditText(this).apply {
            hint = "Số lượng BTC"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL // Bàn phím số
            setHintTextColor(0xFFAAAAAA.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        // Nút gửi
        val sendBtn = Button(this).apply {
            text = "Gửi"
            setBackgroundColor(0xFFB39DDB.toInt())
        }

        // Xử lý click
        sendBtn.setOnClickListener {
            val to = addressInput.text.toString() // Lấy địa chỉ
            val amount = amountInput.text.toString() // Lấy số lượng
            // Kiểm tra rỗng
            if (to.isBlank() || amount.isBlank()) {
                Toast.makeText(this, "Nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Chạy coroutine
            scope.launch {
                val wm = WalletManager(this@SendActivity)
                // Gửi trên thread IO
                val txid = withContext(Dispatchers.IO) {
                    wm.sendBitcoin(to, amount)
                }
                // Hiện TXID
                Toast.makeText(this@SendActivity, "TXID: $txid", Toast.LENGTH_LONG).show()
                finish() // Đóng màn hình
            }
        }

        // Thêm view
        layout.addView(addressInput)
        layout.addView(amountInput)
        layout.addView(sendBtn)
        setContentView(layout)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Hủy coroutine
    }
}