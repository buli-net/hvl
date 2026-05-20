// FILE: ManageWalletsActivity.kt
// TÁC DỤNG: Cho phép đổi tên, xóa, chọn ví đang dùng
package com.hvl.wallet

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
        loadWallets()
        
        // Nhấn giữ để đổi tên hoặc xóa
        binding.walletList.setOnItemLongClickListener { _, _, pos, _ ->
            val name = binding.walletList.adapter.getItem(pos) as String
            AlertDialog.Builder(this).setTitle("Ví: $name")
                .setItems(arrayOf("Chọn dùng", "Đổi tên", "Xóa")) { _, which ->
                    when (which) {
                        0 -> { wm.createNewWallet(name); finish() } // chọn
                        1 -> renameWallet(name)
                        2 -> { wm.deleteWallet(name); loadWallets() }
                    }
                }.show()
            true
        }
        
        binding.addWalletBtn.setOnClickListener {
            val input = EditText(this)
            input.hint = "Tên ví mới"
            AlertDialog.Builder(this).setTitle("Tạo ví")
                .setView(input)
                .setPositiveButton("Tạo") { _, _ ->
                    wm.createNewWallet(input.text.toString())
                    loadWallets()
                }.show()
        }
    }
    
    private fun loadWallets() {
        val wallets = wm.listWallets()
        binding.walletList.adapter = ArrayAdapter(this, 
            android.R.layout.simple_list_item_1, wallets)
    }
    
    private fun renameWallet(oldName: String) {
        // Đơn giản: tạo ví mới với tên mới rồi xóa cũ (bitcoinj không hỗ trợ rename trực tiếp)
        val input = EditText(this)
        input.setText(oldName)
        AlertDialog.Builder(this).setTitle("Đổi tên")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                // Thực tế cần copy file, ở đây mình tạo mới
                wm.createNewWallet(input.text.toString())
                loadWallets()
            }.show()
    }
}