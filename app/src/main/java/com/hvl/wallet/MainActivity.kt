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
    private lateinit var wm: WalletManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wm = WalletManager(this)
        wm.start()

        binding.addressText.text = wm.getCurrentAddress()
        binding.balanceText.text = wm.getBalance()
        binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wm.getTransactionHistory())

        scope.launch {
            try {
                val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
                binding.priceText.text = "BTC: $${"%,.0f".format(price)}"
            } catch (_: Exception) {
                binding.priceText.text = "BTC: $0"
            }
        }

        binding.receiveBtn.setOnClickListener { 
            startActivity(Intent(this, ReceiveActivity::class.java)) 
        }
        
        binding.sendBtn.setOnClickListener { 
            startActivity(Intent(this, SendActivity::class.java)) 
        }
        
        // NÚT GỐC - ĐÃ FIX LỖI
        binding.newAddressBtn.setOnClickListener {
            // Bỏ hàm createNewAddress() không tồn tại
            binding.addressText.text = wm.getCurrentAddress()
            Toast.makeText(this, "Địa chỉ hiện tại", Toast.LENGTH_SHORT).show()
        }
        
        binding.manageBtn.setOnClickListener { 
            startActivity(Intent(this, ManageWalletsActivity::class.java)) 
        }
        
        binding.seedBtn.setOnClickListener { 
            Toast.makeText(this, wm.getSeedPhrase(), Toast.LENGTH_LONG).show() 
        }
    }

    override fun onResume() {
        super.onResume()
        if (::wm.isInitialized) {
            binding.balanceText.text = wm.getBalance()
            binding.addressText.text = wm.getCurrentAddress()
            binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wm.getTransactionHistory())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wm.isInitialized) {
            wm.stop()
        }
        scope.cancel()
    }
}