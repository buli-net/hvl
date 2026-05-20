package com.hvl.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hvl.wallet.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var wm: WalletManager? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- KHỞI TẠO WALLET AN TOÀN ---
        try {
            wm = WalletManager(this)
            wm?.start()
            binding.addressText.text = wm?.getCurrentAddress() ?: "No address"
            binding.balanceText.text = wm?.getBalance() ?: "0 BTC"
            binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wm?.getTransactionHistory() ?: emptyList())
        } catch (e: Exception) {
            // Nếu wallet lỗi vẫn hiện UI
            binding.addressText.text = "Wallet error: ${e.message}"
            binding.balanceText.text = "0 BTC"
        }

        // Giá BTC
        scope.launch {
            try {
                val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
                binding.priceText.text = "BTC: $${"%,.0f".format(price)}"
            } catch (_: Exception) {}
        }

        // Các nút (ĐÃ XÓA newAddressBtn)
        binding.receiveBtn.setOnClickListener { startActivity(Intent(this, ReceiveActivity::class.java)) }
        binding.sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        binding.manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        binding.seedBtn.setOnClickListener { 
            Toast.makeText(this, wm?.getSeedPhrase() ?: "No seed", Toast.LENGTH_LONG).show() 
        }
    }

    override fun onResume() {
        super.onResume()
        wm?.let { binding.balanceText.text = it.getBalance() }
    }

    override fun onDestroy() {
        super.onDestroy()
        wm?.stop()
        scope.cancel()
    }
}