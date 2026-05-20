// FILE: app/src/main/java/com/hvl/wallet/MainActivity.kt
// TÁC DỤNG: Màn hình chính - hiển thị số dư, địa chỉ bc1q, QR nhận tiền

package com.hvl.wallet

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hvl.wallet.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // Binding giúp truy cập view không cần findViewById
    private lateinit var binding: ActivityMainBinding
    // Quản lý ví
    private lateinit var wm: WalletManager
    // Quản lý mật khẩu
    private lateinit var passHelper: PasswordHelper

    // onCreate chạy khi mở app
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
            // Dialog không cho bấm ra ngoài
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
                .setView(input).setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    // Kiểm tra đúng pass mới vào
                    if (passHelper.checkPassword(input.text.toString())) {
                        initWallet()
                    } else {
                        finish() // Sai thì thoát
                    }
                }.show()
        } else {
            // Lần đầu tạo mật khẩu
            val input = EditText(this)
            input.hint = "Tạo mật khẩu mới"
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
                .setView(input).setPositiveButton("Lưu") { _, _ ->
                    passHelper.setPassword(input.text.toString())
                    initWallet()
                }.show()
        }
    }
    
    // onResume chạy khi quay lại từ màn hình khác (quan trọng để refresh sau khi đổi ví)
    override fun onResume() {
        super.onResume()
        if (::wm.isInitialized && wm.listWallets().isNotEmpty()) {
            refreshUI() // Cập nhật lại địa chỉ, số dư
        }
    }

    // Khởi tạo ví và gán sự kiện nút
    private fun initWallet() {
        // Nếu chưa có ví nào, chuyển sang tạo ví
        if (wm.listWallets().isEmpty()) {
            startActivity(Intent(this, WalletSetupActivity::class.java))
            finish()
            return
        }
        
        // Chạy ví trong thread background
        lifecycleScope.launch(Dispatchers.IO) {
            wm.start() // Khởi động blockchain
            withContext(Dispatchers.Main) {
                refreshUI() // Cập nhật UI sau khi xong
            }
        }
        
        // NÚT GỬI
        binding.sendBtn.setOnClickListener {
            startActivity(Intent(this, SendActivity::class.java))
        }
        
        // NÚT QUẢN LÝ VÍ
        binding.manageBtn.setOnClickListener {
            startActivity(Intent(this, ManageWalletsActivity::class.java))
        }
        
        // NÚT NHẬN - HIỆN QR
        binding.receiveBtn.setOnClickListener {
            val addr = wm.getCurrentAddress() // Lấy địa chỉ bc1q
            showQR(addr, "Địa chỉ nhận bc1q")
        }
        
        // NÚT TẠO ĐỊA CHỈ MỚI
        binding.newAddressBtn.setOnClickListener {
            binding.addressText.text = wm.getNewAddress()
        }
        
        // NÚT XUẤT SEED
        binding.seedBtn.setOnClickListener {
            val seed = wm.getSeedPhrase()
            showQR(seed, "Seed 12 từ - GIỮ KÍN!")
        }
    }
    
    // Cập nhật số dư, giá BTC, lịch sử
    private fun refreshUI() {
        lifecycleScope.launch {
            val price = PriceHelper.getBtcPrice() // Lấy giá từ API
            binding.addressText.text = wm.getCurrentAddress()
            binding.balanceText.text = wm.getBalance()
            binding.priceText.text = "BTC: $${"%.0f".format(price)}"
            val history = wm.getTransactionHistory()
            binding.historyList.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, history)
        }
    }
    
    // Hiện QR code
    private fun showQR(text: String, title: String) {
        val bitmap = generateQR(text)
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(image)
            .setMessage(text) // Hiện text để copy
            .setPositiveButton("Đóng", null)
            .show()
    }
    
    // Tạo QR từ text
    private fun generateQR(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                // Nếu bit true thì đen, false thì trắng
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }

    // Dừng ví khi thoát
    override fun onDestroy() {
        super.onDestroy()
        if (::wm.isInitialized) wm.stop()
    }
}