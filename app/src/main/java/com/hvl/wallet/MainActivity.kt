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
    // Biến binding
    private lateinit var binding: ActivityMainBinding
    // Biến quản lý ví
    private lateinit var wm: WalletManager
    // Biến quản lý mật khẩu
    private lateinit var passHelper: PasswordHelper

    // Hàm onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        // Gọi super
        super.onCreate(savedInstanceState)
        // Inflate layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Set content view
        setContentView(binding.root)
        // Khởi tạo WalletManager
        wm = WalletManager(this)
        // Khởi tạo PasswordHelper
        passHelper = PasswordHelper(this)

        // Nếu đã đặt mật khẩu
        if (passHelper.isPasswordSet()) {
            // Tạo EditText
            val input = EditText(this)
            // Gợi ý
            input.hint = "Nhập mật khẩu"
            // Hiện dialog
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
              .setView(input).setCancelable(false)
              .setPositiveButton("OK") { _, _ ->
                    // Kiểm tra mật khẩu
                    if (passHelper.checkPassword(input.text.toString())) initWallet() else finish()
                }.show()
        } else {
            // Tạo EditText
            val input = EditText(this)
            // Gợi ý
            input.hint = "Tạo mật khẩu mới"
            // Hiện dialog
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
              .setView(input).setPositiveButton("Lưu") { _, _ ->
                    // Lưu mật khẩu
                    passHelper.setPassword(input.text.toString())
                    // Khởi tạo ví
                    initWallet()
                }.show()
        }
    }

    // Hàm onResume
    override fun onResume() {
        // Gọi super
        super.onResume()
        // Nếu ví đã khởi tạo
        if (::wm.isInitialized && wm.listWallets().isNotEmpty()) {
            // Refresh UI
            refreshUI()
        }
    }

    // Hàm initWallet
    private fun initWallet() {
        // Nếu chưa có ví nào
        if (wm.listWallets().isEmpty()) {
            // Chuyển sang màn hình setup
            startActivity(Intent(this, WalletSetupActivity::class.java))
            // Đóng activity
            finish()
            // Thoát
            return
        }
        // Chạy coroutine IO
        lifecycleScope.launch(Dispatchers.IO) {
            // Start ví
            wm.start()
            // Quay về main thread
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
        // Nút seed
        binding.seedBtn.setOnClickListener {
            // Lấy seed
            val seed = wm.getSeedPhrase()
            // Hiện QR
            showQR(seed, "Seed 12 từ - GIỮ KÍN!")
        }
    }

    // Hàm refreshUI
    private fun refreshUI() {
        // Coroutine
        lifecycleScope.launch {
            // Lấy giá BTC
            val price = PriceHelper.getBtcPrice()
            // Set địa chỉ
            binding.addressText.text = wm.getCurrentAddress()
            // Set số dư
            binding.balanceText.text = wm.getBalance()
            // Set giá
            binding.priceText.text = "BTC: $${"%.0f".format(price)}"
            // Lấy lịch sử
            val history = wm.getTransactionHistory()
            // Set adapter
            binding.historyList.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, history)
        }
    }

    // Hàm showQR
    private fun showQR(text: String, title: String) {
        // Tạo bitmap QR
        val bitmap = generateQR(text)
        // Tạo ImageView
        val image = ImageView(this)
        // Set bitmap
        image.setImageBitmap(bitmap)
        // Hiện dialog
        AlertDialog.Builder(this).setTitle(title).setView(image).setMessage(text).setPositiveButton("Đóng", null).show()
    }

    // Hàm generateQR
    private fun generateQR(text: String): Bitmap {
        // Tạo writer
        val writer = QRCodeWriter()
        // Encode text
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        // Tạo bitmap
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        // Vẽ từng pixel
        for (x in 0 until 512) for (y in 0 until 512) {
            // Nếu true thì đen, false thì trắng
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        // Trả bitmap
        return bmp
    }

    // Hàm onDestroy
    override fun onDestroy() {
        // Gọi super
        super.onDestroy()
        // Dừng ví nếu đã init
        if (::wm.isInitialized) wm.stop()
    }
}