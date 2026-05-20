// FILE: app/src/main/java/com/hvl/wallet/MainActivity.kt
// TÁC DỤNG: Màn hình chính

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
    private lateinit var binding: ActivityMainBinding
    private lateinit var wm: WalletManager
    private lateinit var passHelper: PasswordHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wm = WalletManager(this)
        passHelper = PasswordHelper(this)
        
        if (passHelper.isPasswordSet()) {
            val input = EditText(this)
            input.hint = "Nhập mật khẩu"
            AlertDialog.Builder(this).setTitle("Mở khóa ví")
                .setView(input).setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    if (passHelper.checkPassword(input.text.toString())) initWallet() else finish()
                }.show()
        } else {
            val input = EditText(this)
            input.hint = "Tạo mật khẩu mới"
            AlertDialog.Builder(this).setTitle("Bảo mật ví")
                .setView(input).setPositiveButton("Lưu") { _, _ ->
                    passHelper.setPassword(input.text.toString())
                    initWallet()
                }.show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::wm.isInitialized && wm.listWallets().isNotEmpty()) {
            refreshUI()
        }
    }

    private fun initWallet() {
        if (wm.listWallets().isEmpty()) {
            startActivity(Intent(this, WalletSetupActivity::class.java))
            finish()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            wm.start()
            withContext(Dispatchers.Main) { refreshUI() }
        }
        binding.sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        binding.manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        binding.receiveBtn.setOnClickListener {
            val addr = wm.getCurrentAddress()
            showQR(addr, "Địa chỉ nhận bc1q")
        }
        binding.newAddressBtn.setOnClickListener { binding.addressText.text = wm.getNewAddress() }
        binding.seedBtn.setOnClickListener {
            val seed = wm.getSeedPhrase()
            showQR(seed, "Seed 12 từ - GIỮ KÍN!")
        }
    }
    
    private fun refreshUI() {
        lifecycleScope.launch {
            val price = PriceHelper.getBtcPrice()
            binding.addressText.text = wm.getCurrentAddress()
            binding.balanceText.text = wm.getBalance()
            binding.priceText.text = "BTC: $${"%.0f".format(price)}"
            val history = wm.getTransactionHistory()
            binding.historyList.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, history)
        }
    }
    
    private fun showQR(text: String, title: String) {
        val bitmap = generateQR(text)
        val image = ImageView(this)
        image.setImageBitmap(bitmap)
        AlertDialog.Builder(this).setTitle(title).setView(image).setMessage(text).setPositiveButton("Đóng", null).show()
    }
    
    private fun generateQR(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) for (y in 0 until 512) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
        return bmp
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wm.isInitialized) wm.stop() // Gọi hàm stop() đã fix
    }
}