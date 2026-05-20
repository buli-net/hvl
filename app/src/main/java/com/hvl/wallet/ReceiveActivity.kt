package com.hvl.wallet

// Import các lớp cần thiết
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

// Activity hiển thị địa chỉ và QR để nhận BTC
class ReceiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tạo layout dọc căn giữa
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF000000.toInt())
        }

        // Lấy ví hiện tại
        val wm = WalletManager(this)
        val address = wm.getCurrentAddress()

        // TextView hiển thị địa chỉ
        val addressView = TextView(this).apply {
            text = address
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setTextIsSelectable(true)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        // ImageView chứa QR
        val qrView = ImageView(this)
        try {
            // Tạo QR từ địa chỉ
            val encoder = BarcodeEncoder()
            val bitmap = encoder.encodeBitmap(address, BarcodeFormat.QR_CODE, 700, 700)
            qrView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Thêm view vào layout
        layout.addView(addressView)
        layout.addView(qrView)
        setContentView(layout)
    }
}