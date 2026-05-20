// FILE: PasswordHelper.kt
// TÁC DỤNG: Lưu và kiểm tra mật khẩu app

package com.hvl.wallet

import android.content.Context
import android.content.SharedPreferences

class PasswordHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("security", Context.MODE_PRIVATE)
    
    // Lưu mật khẩu (mã hóa đơn giản)
    fun setPassword(password: String) {
        prefs.edit().putString("app_pass", password.hashCode().toString()).apply()
    }
    
    // Kiểm tra mật khẩu
    fun checkPassword(password: String): Boolean {
        val saved = prefs.getString("app_pass", null) ?: return true // chưa đặt pass thì cho qua
        return saved == password.hashCode().toString()
    }
    
    // Kiểm tra đã đặt pass chưa
    fun isPasswordSet(): Boolean = prefs.contains("app_pass")
}