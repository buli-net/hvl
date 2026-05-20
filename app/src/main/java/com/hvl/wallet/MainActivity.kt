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
            val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
            binding.priceText.text = "BTC: $${"%,.0f".format(price)}"
        }

        binding.receiveBtn.setOnClickListener { startActivity(Intent(this, ReceiveActivity::class.java)) }
        binding.sendBtn.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        
        // NÚT GỐC - CHƯA XÓA
        binding.newAddressBtn.setOnClickListener {
            wm.createNewAddress()
            binding.addressText.text = wm.getCurrentAddress()
            Toast.makeText(this, "Đã tạo địa chỉ mới", Toast.LENGTH_SHORT).show()
        }
        
        binding.manageBtn.setOnClickListener { startActivity(Intent(this, ManageWalletsActivity::class.java)) }
        binding.seedBtn.setOnClickListener { Toast.makeText(this, wm.getSeedPhrase(), Toast.LENGTH_LONG).show() }
    }

    override fun onResume() {
        super.onResume()
        binding.balanceText.text = wm.getBalance()
        binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wm.getTransactionHistory())
    }

    override fun onDestroy() {
        super.onDestroy()
        wm.stop()
        scope.cancel()
    }
}