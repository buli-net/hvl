// FILE: WalletSetupActivity.kt
// TÁC DỤNG: Tạo ví mới hoặc import seed
package com.hvl.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hvl.wallet.databinding.ActivityWalletSetupBinding

class WalletSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWalletSetupBinding
    private lateinit var wm: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalletSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        wm = WalletManager(this)
        
        // Nút tạo ví mới
        binding.createBtn.setOnClickListener {
            // Lưu chế độ testnet nếu được chọn
            wm.setTestnet(binding.testnetCheck.isChecked)
            // Tạo ví với tên mặc định
            wm.createNewWallet("wallet1")
            // Quay về màn hình chính
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        
        // Nút import seed
        binding.importBtn.setOnClickListener {
            val input = EditText(this)
            input.hint = "Dán 12 từ cách nhau khoảng trắng"
            AlertDialog.Builder(this).setTitle("Nhập Seed")
                .setView(input)
                .setPositiveButton("Import") { _, _ ->
                    wm.setTestnet(binding.testnetCheck.isChecked)
                    wm.importFromSeed("imported", input.text.toString())
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }.show()
        }
    }
}