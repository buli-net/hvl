// FILE: MainActivity.kt
// TÁC DỤNG: Màn hình chính hiển thị địa chỉ ví BTC mainnet

package com.hvl.wallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hvl.wallet.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    // SỬA LỖI: chỉ truyền 1 tham số context
    private lateinit var walletManager: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo ví
        walletManager = WalletManager(this)

        // Chạy ví trên background
        lifecycleScope.launch(Dispatchers.IO) {
            walletManager.start()
            withContext(Dispatchers.Main) {
                // Hiển thị địa chỉ hiện tại
                binding.addressText.text = walletManager.getCurrentAddress()
                // Hiển thị số dư
                binding.balanceText.text = walletManager.getBalance()
            }
        }

        // Nút tạo địa chỉ mới
        binding.newAddressBtn.setOnClickListener {
            binding.addressText.text = walletManager.getNewAddress()
        }

        // Nút refresh
        binding.refreshBtn.setOnClickListener {
            binding.addressText.text = walletManager.getCurrentAddress()
            binding.balanceText.text = walletManager.getBalance()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        walletManager.stop()
    }
}