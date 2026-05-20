// FILE: MainActivity.kt
// TÁC DỤNG: Giao diện chính, hiển thị địa chỉ bc1... và số dư mainnet

package com.hvl.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hvl.wallet.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // ViewBinding để truy cập tvAddress, tvBalance...
    private lateinit var binding: ActivityMainBinding
    // Quản lý ví Bitcoin
    private lateinit var walletManager: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BƯỚC 1: Khởi tạo ví MAINNET
        // File ví sẽ lưu trong bộ nhớ app, không mất khi tắt app
        walletManager = WalletManager(this, "hvl-mainnet.wallet")

        // BƯỚC 2: Hiển thị địa chỉ hiện tại
        updateUI()

        // BƯỚC 3: Nút tạo địa chỉ mới
        binding.btnReceive.setOnClickListener {
            // Tạo địa chỉ nhận mới dạng bech32 (bc1...)
            val newAddress = walletManager.getNewAddress()
            binding.tvAddress.text = "Địa chỉ: $newAddress"
            Toast.makeText(this, "Đã tạo địa chỉ mainnet mới", Toast.LENGTH_SHORT).show()
        }

        // BƯỚC 4: Nút copy địa chỉ
        binding.btnCopy.setOnClickListener {
            val address = walletManager.getCurrentAddress()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("BTC Address", address))
            Toast.makeText(this, "Đã copy: $address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        // Lấy địa chỉ mainnet hiện tại (bắt đầu bằng bc1)
        val address = walletManager.getCurrentAddress()
        binding.tvAddress.text = "Địa chỉ: $address"

        // Lấy số dư (ban đầu = 0)
        val balance = walletManager.getBalance()
        binding.tvBalance.text = "Số dư: %.8f BTC".format(balance)
    }

    override fun onResume() {
        super.onResume()
        // Cập nhật số dư mỗi khi mở app
        updateUI()
    }
}