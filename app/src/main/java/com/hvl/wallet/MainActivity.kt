// FILE: MainActivity.kt
// TÁC DỤNG: Điều khiển giao diện, xử lý nút bấm, tạo QR, quét QR

package com.hvl.wallet

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.hvl.wallet.databinding.ActivityMainBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Binding để truy cập các view trong layout
    private lateinit var binding: ActivityMainBinding
    // Quản lý ví
    private lateinit var walletManager: WalletManager

    // Trình quét QR - đăng ký trước để nhận kết quả
    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        // Nếu quét được địa chỉ
        if (result.contents != null) {
            // Mở dialog nhập số tiền gửi
            showSendDialog(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tạo WalletManager
        walletManager = WalletManager(this)

        // Chạy ví trên luồng IO (không chặn UI)
        lifecycleScope.launch(Dispatchers.IO) {
            walletManager.start()
            // Quay lại luồng chính để cập nhật UI
            withContext(Dispatchers.Main) {
                binding.addressText.text = walletManager.getCurrentAddress()
                binding.balanceText.text = walletManager.getBalance()
            }
        }

        // Nút tạo địa chỉ mới
        binding.newAddressBtn.setOnClickListener {
            binding.addressText.text = walletManager.getNewAddress()
        }

        // Nút làm mới số dư
        binding.refreshBtn.setOnClickListener {
            binding.addressText.text = walletManager.getCurrentAddress()
            binding.balanceText.text = walletManager.getBalance()
        }

        // NÚT NHẬN - hiện QR địa chỉ ví
        binding.receiveBtn.setOnClickListener {
            val addr = walletManager.getCurrentAddress()
            showQR(addr, "QR Nhận BTC")
        }

        // NÚT XUẤT SEED - hiện 12 từ + QR
        binding.seedBtn.setOnClickListener {
            val seed = walletManager.getSeedPhrase()
            showQR(seed, "Seed 12 từ - BẢO MẬT TUYỆT ĐỐI!")
        }

        // NÚT GỬI - mở camera quét QR
        binding.sendBtn.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Quét địa chỉ BTC người nhận")
            options.setBeepEnabled(true) // kêu bíp khi quét xong
            qrLauncher.launch(options)
        }
    }

    // Hiện dialog nhập số BTC sau khi quét QR
    private fun showSendDialog(scannedAddress: String) {
        val input = EditText(this)
        input.hint = "Số BTC muốn gửi (vd: 0.001)"
        AlertDialog.Builder(this)
            .setTitle("Gửi tới: $scannedAddress")
            .setView(input)
            .setPositiveButton("Gửi") { _, _ ->
                val amount = input.text.toString()
                // Gửi trên luồng IO
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = walletManager.sendCoins(scannedAddress, amount)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                        binding.balanceText.text = walletManager.getBalance()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Hiện QR code từ chuỗi text
    private fun showQR(text: String, title: String) {
        val bitmap = generateQR(text)
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(image)
            .setMessage(text) // hiện text bên dưới để copy
            .setPositiveButton("Đóng", null)
            .show()
    }

    // Tạo ảnh QR từ chuỗi
    private fun generateQR(text: String): Bitmap {
        val writer = QRCodeWriter()
        // Mã hóa text thành ma trận QR 512x512
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        // Vẽ từng pixel đen/trắng
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }

    // Dừng ví khi thoát app
    override fun onDestroy() {
        super.onDestroy()
        walletManager.stop()
    }
}