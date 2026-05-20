// FILE: MainActivity.kt

// Package
package com.hvl.wallet

// Import Intent
import android.content.Intent
// Import Bitmap
import android.graphics.Bitmap
// Import Bundle
import android.os.Bundle
// Import ArrayAdapter
import android.widget.ArrayAdapter
// Import EditText
import android.widget.EditText
// Import ImageView
import android.widget.ImageView
// Import AlertDialog
import androidx.appcompat.app.AlertDialog
// Import AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
// Import lifecycleScope
import androidx.lifecycle.lifecycleScope
// Import BarcodeFormat
import com.google.zxing.BarcodeFormat
// Import QRCodeWriter
import com.google.zxing.qrcode.QRCodeWriter
// Import binding
import com.hvl.wallet.databinding.ActivityMainBinding
// Import Dispatchers
import kotlinx.coroutines.Dispatchers
// Import launch
import kotlinx.coroutines.launch
// Import withContext
import kotlinx.coroutines.withContext

// Class MainActivity
class MainActivity : AppCompatActivity() {
    // Binding
    private lateinit var binding: ActivityMainBinding
    // WalletManager
    private lateinit var wm: WalletManager
    // PasswordHelper
    private lateinit var passHelper: PasswordHelper

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        // Super
        super.onCreate(savedInstanceState)
        // Inflate
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Set content
        setContentView(binding.root)
        // Khởi tạo wm
        wm = WalletManager(this)
        // Khởi tạo pass
        passHelper = PasswordHelper(this)
        
        // Nếu có pass
        if (passHelper.isPasswordSet()) {
            // Tạo input
            val input = EditText(this)
            // Hint
            input.hint = "Nhập mật khẩu"
            // Dialog
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
                .setView(input).setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    // Kiểm tra
                    if (passHelper.checkPassword(input.text.toString())) initWallet() else finish()
                }.show()
        } else {
            // Tạo input
            val input = EditText(this)
            // Hint
            input.hint = "Tạo mật khẩu mới"
            // Dialog
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
                .setView(input).setPositiveButton("Lưu") { _, _ ->
                    // Lưu pass
                    passHelper.setPassword(input.text.toString())
                    // Init
                    initWallet()
                }.show()
        }
    }
    
    // onResume
    override fun onResume() {
        // Super
        super.onResume()
        // Nếu đã init
        if (::wm.isInitialized && wm.listWallets().isNotEmpty()) {
            // Refresh
            refreshUI()
        }
    }

    // initWallet
    private fun initWallet() {
        // Nếu chưa có ví
        if (wm.listWallets().isEmpty()) {
            // Chuyển setup
            startActivity(Intent(this, WalletSetupActivity::class.java))
            // Đóng
            finish()
            // Thoát
            return
        }
        // Chạy background
        lifecycleScope.launch(Dispatchers.IO) {
            // Start ví
            wm.start()
            // Về main
            withContext(Dispatchers.Main) { refreshUI() }
        }
        // Nút gửi
        binding.sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        // Nút quản lý
        binding.manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        // Nút nhận
        binding.receiveBtn.setOnClickListener {
            // Lấy địa chỉ
            val addr = wm.getCurrentAddress()
            // Hiện QR
            showQR(addr, "Địa chỉ nhận bc1q")
        }
        // Nút tạo địa chỉ
        binding.newAddressBtn.setOnClickListener {
            // Tạo mới
            val newAddr = wm.getNewAddress()
            // Set text
            binding.addressText.text = newAddr
            // Toast
            android.widget.Toast.makeText(this, "Đã tạo và lưu địa chỉ mới", android.widget.Toast.LENGTH_SHORT).show()
        }
        // Nút seed
        binding.seedBtn.setOnClickListener {
            // Lấy seed
            val seed = wm.getSeedPhrase()
            // Hiện
            showQR(seed, "Seed 12 từ - GIỮ KÍN!")
        }
    }
    
    // refreshUI
    private fun refreshUI() {
        // Coroutine
        lifecycleScope.launch {
            // Lấy giá
            val price = PriceHelper.getBtcPrice()
            // Set địa chỉ
            binding.addressText.text = wm.getCurrentAddress()
            // Set balance
            binding.balanceText.text = wm.getBalance()
            // Set giá
            binding.priceText.text = "BTC: $${"%.0f".format(price)}"
            // Lấy history
            val history = wm.getTransactionHistory()
            // Set adapter
            binding.historyList.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, history)
        }
    }
    
    // showQR
    private fun showQR(text: String, title: String) {
        // Tạo bitmap
        val bitmap = generateQR(text)
        // Tạo imageview
        val image = ImageView(this)
        // Set bitmap
        image.setImageBitmap(bitmap)
        // Dialog
        AlertDialog.Builder(this).setTitle(title).setView(image).setMessage(text).setPositiveButton("Đóng", null).show()
    }
    
    // generateQR
    private fun generateQR(text: String): Bitmap {
        // Writer
        val writer = QRCodeWriter()
        // Encode
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        // Bitmap
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        // Loop
        for (x in 0 until 512) for (y in 0 until 512) {
            // Set pixel
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        // Trả
        return bmp
    }

    // onDestroy
    override fun onDestroy() {
        // Super
        super.onDestroy()
        // Dừng ví
        if (::wm.isInitialized) wm.stop()
    }
}