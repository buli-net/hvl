// FILE: SendActivity.kt
// TÁC DỤNG: Xử lý gửi BTC với quét QR và tính phí USD
package com.hvl.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hvl.wallet.databinding.ActivitySendBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Coin

class SendActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySendBinding
    private lateinit var wm: WalletManager
    private var btcPrice = 0.0
    private var feeRate = 10L // sat/vB

    // Khởi tạo trình quét QR
    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Điền địa chỉ quét được vào ô input
            binding.addressInput.setText(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        wm = WalletManager(this)
        
        // Lấy giá BTC và phí mạng
        lifecycleScope.launch {
            btcPrice = PriceHelper.getBtcPrice()
            feeRate = PriceHelper.getFeeRate()
            updateFeeEstimate()
        }
        
        // Nút quét QR
        binding.scanBtn.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Quét địa chỉ ví nhận")
            qrLauncher.launch(options)
        }
        
        // Khi thay đổi mức phí, cập nhật lại
        binding.feeGroup.setOnCheckedChangeListener { _, _ -> updateFeeEstimate() }
        
        // Khi nhập số tiền, cập nhật tổng
        binding.amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { updateFeeEstimate() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Nút xác nhận gửi
        binding.sendConfirmBtn.setOnClickListener {
            val address = binding.addressInput.text.toString()
            val amount = binding.amountInput.text.toString()
            if (address.isEmpty() || amount.isEmpty()) {
                Toast.makeText(this, "Nhập đủ địa chỉ và số tiền", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Tính phí theo mức chọn
            val feePerKb = when (binding.feeGroup.checkedRadioButtonId) {
                R.id.feeLow -> Coin.valueOf(feeRate * 500) // phí thấp
                R.id.feeHigh -> Coin.valueOf(feeRate * 2000) // phí cao
                else -> Coin.valueOf(feeRate * 1000) // trung bình
            }
            
            lifecycleScope.launch(Dispatchers.IO) {
                val result = wm.sendCoins(address, amount, feePerKb)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SendActivity, result, Toast.LENGTH_LONG).show()
                    if (result.contains("Đã gửi")) finish()
                }
            }
        }
    }
    
    // Cập nhật ước tính phí và tổng
    private fun updateFeeEstimate() {
        val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
        val multiplier = when (binding.feeGroup.checkedRadioButtonId) {
            R.id.feeLow -> 0.5
            R.id.feeHigh -> 2.0
            else -> 1.0
        }
        // Ước tính phí ~ 0.00001 BTC
        val feeBtc = 0.00001 * multiplier
        val feeUsd = feeBtc * btcPrice
        val total = amount + feeBtc
        
        binding.feeEstimate.text = "Phí: ~${"%.5f".format(feeBtc)} BTC ($${"%.2f".format(feeUsd)})"
        binding.totalEstimate.text = "Tổng: ${"%.5f".format(total)} BTC"
    }
}