package com.hvl.wallet

import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat // Định dạng mã vạch
import com.journeyapps.barcodescanner.BarcodeEncoder // Tạo QR

class ReceiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Gọi cha

        // Tạo LinearLayout dọc
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Xếp dọc
            gravity = Gravity.CENTER // Căn giữa
            setPadding(32, 32, 32, 32) // Đệm
            setBackgroundColor(0xFF000000.toInt()) // Nền đen
        }

        // Lấy ví
        val wm = WalletManager(this)
        // Lấy địa chỉ hiện tại
        val address = wm.getCurrentAddress()

        // TextView hiển thị địa chỉ
        val addressView = TextView(this).apply {
            text = address // Gán địa chỉ
            textSize = 14f // Cỡ chữ
            setTextColor(0xFFFFFFFF.toInt()) // Màu trắng
            setTextIsSelectable(true) // Cho phép copy
            gravity = Gravity.CENTER // Căn giữa
            setPadding(0, 0, 0, 32) // Cách dưới
        }

        // ImageView để chứa QR
        val qrView = ImageView(this)
        try {
            // Tạo encoder
            val encoder = BarcodeEncoder()
            // Tạo bitmap QR 700x700
            val bitmap = encoder.encodeBitmap(address, BarcodeFormat.QR_CODE, 700, 700)
            // Gán ảnh
            qrView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace() // In lỗi nếu có
        }

        // Thêm view vào layout
        layout.addView(addressView)
        layout.addView(qrView)
        // Gán layout cho activity
        setContentView(layout)
    }
}