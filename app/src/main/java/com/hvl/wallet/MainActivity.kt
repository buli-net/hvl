// FILE: MainActivity.kt - MÀN HÌNH CHÍNH VỚI MẬT KHẨU, GIÁ, LỊCH SỬ
package com.hvl.wallet
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hvl.wallet.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wm: WalletManager
    private lateinit var passHelper: PasswordHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        wm = WalletManager(this)
        passHelper = PasswordHelper(this)
        
        // KIỂM TRA MẬT KHẨU
        if (passHelper.isPasswordSet()) {
            val input = EditText(this)
            input.hint = "Nhập mật khẩu"
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
                .setView(input).setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    if (passHelper.checkPassword(input.text.toString())) {
                        initWallet()
                    } else finish()
                }.show()
        } else {
            // Lần đầu đặt mật khẩu
            val input = EditText(this)
            input.hint = "Tạo mật khẩu mới"
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
                .setView(input).setPositiveButton("Lưu") { _, _ ->
                    passHelper.setPassword(input.text.toString())
                    initWallet()
                }.show()
        }
    }
    
    private fun initWallet() {
        // KIỂM TRA CÓ VÍ CHƯA
        if (wm.listWallets().isEmpty()) {
            startActivity(Intent(this, WalletSetupActivity::class.java))
            finish()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            wm.start()
            val price = PriceHelper.getBtcPrice()
            withContext(Dispatchers.Main) {
                binding.addressText.text = wm.getCurrentAddress()
                binding.balanceText.text = wm.getBalance()
                binding.priceText.text = "BTC: $${"%.0f".format(price)}"
                // HIỂN THỊ LỊCH SỬ
                val history = wm.getTransactionHistory()
                binding.historyList.adapter = ArrayAdapter(this@MainActivity, 
                    android.R.layout.simple_list_item_1, history)
            }
        }
        
        binding.sendBtn.setOnClickListener { 
            startActivity(Intent(this, SendActivity::class.java)) 
        }
        binding.manageBtn.setOnClickListener {
            startActivity(Intent(this, ManageWalletsActivity::class.java))
        }
        binding.receiveBtn.setOnClickListener {
            // Hiện QR như cũ
        }
        binding.seedBtn.setOnClickListener {
            val seed = wm.getSeedPhrase()
            AlertDialog.Builder(this).setTitle("Seed 12 từ")
                .setMessage(seed).show()
        }
    }
}