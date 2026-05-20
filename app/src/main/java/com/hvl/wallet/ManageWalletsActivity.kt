package com.hvl.wallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hvl.wallet.databinding.ActivityManageWalletsBinding

class ManageWalletsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageWalletsBinding
    private lateinit var wm: WalletManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageWalletsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wm = WalletManager(this)
        wm.start()
        // giữ nguyên code cũ của bạn ở dưới, không thêm backBtn
    }

    override fun onDestroy() {
        super.onDestroy()
        wm.stop()
    }
}