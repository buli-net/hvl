// FILE: app/src/main/java/com/hvl/wallet/ManageWalletsActivity.kt
// TÁC DỤNG: Quản lý ví

package com.hvl.wallet

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
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
        
        binding.walletList.setOnItemLongClickListener { _, _, pos, _ ->
            val displayName = binding.walletList.adapter.getItem(pos) as String
            val name = displayName.replace(" ✓", "")
            AlertDialog.Builder(this).setTitle("Ví: $name")
                .setItems(arrayOf("Chọn dùng", "Đổi tên", "Xóa")) { _, which ->
                    when (which) {
                        0 -> {
                            wm.switchWallet(name) // Hàm đã có
                            Toast.makeText(this, "Đã chọn $name", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        1 -> renameWallet(name)
                        2 -> { wm.deleteWallet(name); loadWallets() }
                    }
                }.show()
            true
        }
        
        binding.addWalletBtn.setOnClickListener {
            val input = EditText(this)
            input.hint = "Tên ví mới"
            AlertDialog.Builder(this).setTitle("Tạo ví").setView(input)
                .setPositiveButton("Tạo") { _, _ ->
                    val newName = input.text.toString()
                    if (newName.isNotEmpty()) { wm.createNewWallet(newName); loadWallets() }
                }.show()
        }
    }
    
    private fun loadWallets() {
        val wallets = wm.listWallets()
        val current = wm.getCurrentWalletName()
        val display = wallets.map { if (it == current) "$it ✓" else it }
        binding.walletList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
    }
    
    private fun renameWallet(oldName: String) {
        val input = EditText(this)
        input.setText(oldName)
        AlertDialog.Builder(this).setTitle("Đổi tên ví").setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (wm.renameWallet(oldName, newName)) { // Hàm đã có
                    Toast.makeText(this, "Đã đổi tên", Toast.LENGTH_SHORT).show()
                    loadWallets()
                }
            }.show()
    }
}