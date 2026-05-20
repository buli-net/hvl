// FILE: app/src/main/java/com/hvl/wallet/MainActivity.kt
// TÁC DỤNG: Màn hình chính của ví Bitcoin - hiển thị địa chỉ bc1q, số dư, giá, lịch sử
// PHIÊN BẢN: 2.1 - Fix nút Nhận QR, refresh khi đổi ví

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
    
    // Binding để truy cập các view trong activity_main.xml
    private lateinit var binding: ActivityMainBinding
    
    // Quản lý ví Bitcoin (tạo địa chỉ, gửi nhận, lịch sử)
    private lateinit var wm: WalletManager
    
    // Quản lý mật khẩu mở app
    private lateinit var passHelper: PasswordHelper

    // Hàm chạy đầu tiên khi app mở
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nạp layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Khởi tạo 2 helper
        wm = WalletManager(this)
        passHelper = PasswordHelper(this)
        
        // KIỂM TRA MẬT KHẨU - nếu đã đặt thì yêu cầu nhập
        if (passHelper.isPasswordSet()) {
            val input = EditText(this)
            input.hint = "Nhập mật khẩu"
            // Hiện dialog không cho thoát nếu chưa nhập đúng
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
                .setView(input).setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    // Kiểm tra mật khẩu đúng mới vào ví
                    if (passHelper.checkPassword(input.text.toString())) {
                        initWallet()
                    } else {
                        finish() // Sai pass thì thoát app
                    }
                }.show()
        } else {
            // Lần đầu chưa có pass - yêu cầu tạo
            val input = EditText(this)
            input.hint = "Tạo mật khẩu mới"
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
                .setView(input).setPositiveButton("Lưu") { _, _ ->
                    passHelper.setPassword(input.text.toString())
                    initWallet()
                }.show()
        }
    }
    
    // Hàm chạy mỗi khi quay lại màn hình chính (quan trọng để refresh sau khi đổi ví)
    override fun onResume() {
        super.onResume()
        // Nếu ví đã khởi tạo và có ví, refresh lại UI
        if (::wm.isInitialized && wm.listWallets().isNotEmpty()) {
            refreshUI()
        }
    }

    // Khởi tạo ví và gán sự kiện nút
    private fun initWallet() {
        // Nếu chưa có ví nào, chuyển sang màn hình tạo ví
        if (wm.listWallets().isEmpty()) {
            startActivity(Intent(this, WalletSetupActivity::class.java))
            finish()
            return
        }
        
        // Chạy ví trong background để không đơ UI
        lifecycleScope.launch(Dispatchers.IO) {
            wm.start() // Khởi động blockchain sync
            withContext(Dispatchers.Main) { 
                refreshUI() // Cập nhật giao diện sau khi ví sẵn sàng
            }
        }
        
        // NÚT GỬI - mở màn hình SendActivity
        binding.sendBtn.setOnClickListener { 
            startActivity(Intent(this, SendActivity::class.java)) 
        }
        
        // NÚT QUẢN LÝ VÍ - mở màn hình ManageWalletsActivity
        binding.manageBtn.setOnClickListener {
            startActivity(Intent(this, ManageWalletsActivity::class.java))
        }
        
        // FIX LỖI 1: NÚT NHẬN - hiện QR địa chỉ bc1q
        binding.receiveBtn.setOnClickListener {
            val addr = wm.getCurrentAddress() // Lấy địa chỉ hiện tại
            showQR(addr, "Địa chỉ nhận bc1q") // Hiện QR
        }
        
        // NÚT TẠO ĐỊA CHỈ MỚI
        binding.newAddressBtn.setOnClickListener {
            binding.addressText.text = wm.getNewAddress()
        }
        
        // NÚT XUẤT SEED - hiện QR seed 12 từ
        binding.seedBtn.setOnClickListener {
            val seed = wm.getSeedPhrase()
            showQR(seed, "Seed 12 từ - GIỮ KÍN!")
        }
    }
    
    // Cập nhật số dư, giá, lịch sử
    private fun refreshUI() {
        lifecycleScope.launch {
            val price = PriceHelper.getBtcPrice() // Lấy giá BTC từ internet
            binding.addressText.text = wm.getCurrentAddress()
            binding.balanceText.text = wm.getBalance()
            binding.priceText.text = "BTC: $${"%.0f".format(price)}"
            // Lấy lịch sử giao dịch
            val history = wm.getTransactionHistory()
            binding.historyList.adapter = ArrayAdapter(this@MainActivity, 
                android.R.layout.simple_list_item_1, history)
        }
    }
    
    // Hàm tạo dialog hiển thị QR code
    private fun showQR(text: String, title: String) {
        val bitmap = generateQR(text) // Tạo ảnh QR
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(image)
            .setMessage(text) // Hiện cả text để copy
            .setPositiveButton("Đóng", null)
            .show()
    }
    
    // Hàm tạo QR code từ text
    private fun generateQR(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                // Điểm đen nếu bitMatrix true, trắng nếu false
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }

    // Dừng ví khi thoát app để không tốn pin
    override fun onDestroy() {
        super.onDestroy()
        if (::wm.isInitialized) wm.stop()
    }
}