package com.hvl.wallet

import android.content.Intent
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

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}