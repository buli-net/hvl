package com.hvl.wallet

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Không dùng XML nữa, tạo view bằng code
        val tv = TextView(this)
        tv.text = "APP CHẠY RỒI - NẾU THẤY CHỮ NÀY LÀ OK"
        tv.textSize = 24f
        tv.setTextColor(Color.BLACK)
        tv.setBackgroundColor(Color.YELLOW)
        tv.setPadding(50, 200, 50, 200)
        
        setContentView(tv)
    }
}