// FILE: ManageWalletsActivity.kt

// Package
package com.hvl.wallet

// Import Intent
import android.content.Intent
// Import Bundle
import android.os.Bundle
// Import ArrayAdapter
import android.widget.ArrayAdapter
// Import EditText
import android.widget.Toast
// Import AlertDialog
import androidx.appcompat.app.AlertDialog
// Import AppCompatActivity
import androidx.appcompat.app.AppCompatActivity
// Import binding
import com.hvl.wallet.databinding.ActivityManageWalletsBinding

// Class
class ManageWalletsActivity : AppCompatActivity() {
    // Binding
    private lateinit var binding: ActivityManageWalletsBinding
    // wm
    private lateinit var wm: WalletManager

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        // Super
        super.onCreate(savedInstanceState)
        // Inflate
        binding = ActivityManageWalletsBinding.inflate(layoutInflater)
        // Set view
        setContentView(binding.root)
        // Khởi tạo wm
        wm = WalletManager(this)
        // Load ví
        loadWallets()

        // ĐỔI TÊN NÚT CŨ THÀNH "Tạo ví"
        binding.addWalletBtn.text = "Tạo ví"

        // NÚT 1: TẠO VÍ MỚI
        binding.addWalletBtn.setOnClickListener {
            // Input
            val input = EditText(this)
            // Hint
            input.hint = "Tên ví mới"
            // Dialog
            AlertDialog.Builder(this).setTitle("Tạo ví mới").setView(input)
                .setPositiveButton("Tạo") { _, _ ->
                    // Lấy tên
                    val newName = input.text.toString().trim()
                    // Nếu không rỗng
                    if (newName.isNotEmpty()) {
                        // Kiểm tra trùng tên
                        if (wm.listWallets().contains(newName)) {
                            Toast.makeText(this, "Tên ví đã tồn tại", Toast.LENGTH_SHORT).show()
                        } else {
                            // Tạo ví mới trắng
                            wm.createNewWallet(newName)
                            // Reload danh sách
                            loadWallets()
                            Toast.makeText(this, "Đã tạo ví $newName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // NÚT 2: THÊM VÍ TỪ SEED (cần thêm Button id=importWalletBtn trong XML)
        binding.importWalletBtn.setOnClickListener {
            // Layout chứa 2 ô nhập
            val layout = android.widget.LinearLayout(this)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            // Ô nhập tên ví
            val nameInput = EditText(this)
            nameInput.hint = "Tên ví (vd: ví tiết kiệm)"
            layout.addView(nameInput)

            // Ô nhập seed 12 từ
            val seedInput = EditText(this)
            seedInput.hint = "12 từ seed, cách nhau bằng dấu cách"
            layout.addView(seedInput)

            // Dialog
            AlertDialog.Builder(this).setTitle("Thêm ví từ Seed").setView(layout)
                .setPositiveButton("Import") { _, _ ->
                    val name = nameInput.text.toString().trim()
                    val seed = seedInput.text.toString().trim()
                    // Kiểm tra
                    if (name.isEmpty() || seed.isEmpty()) {
                        Toast.makeText(this, "Nhập đủ tên và seed", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (seed.split(" ").size != 12) {
                        Toast.makeText(this, "Seed phải đủ 12 từ", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (wm.listWallets().contains(name)) {
                        Toast.makeText(this, "Tên ví đã tồn tại", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    try {
                        // Import ví
                        wm.importFromSeed(name, seed)
                        // Reload
                        loadWallets()
                        Toast.makeText(this, "Đã thêm ví $name", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Seed không hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // Long click vào ví
        binding.walletList.setOnItemLongClickListener { _, _, pos, _ ->
            // Lấy tên
            val displayName = binding.walletList.adapter.getItem(pos) as String
            // Bỏ dấu check
            val name = displayName.replace(" ✓", "")
            // Dialog
            AlertDialog.Builder(this).setTitle("Ví: $name")
                .setItems(arrayOf("Chọn dùng", "Đổi tên", "Xem địa chỉ", "Xóa")) { _, which ->
                    when (which) {
                        0 -> {
                            // Chọn ví
                            wm.switchWallet(name)
                            Toast.makeText(this, "Đã chọn $name", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        1 -> renameWallet(name)
                        2 -> {
                            // Xem địa chỉ
                            val prev = wm.getCurrentWalletName()
                            wm.switchWallet(name)
                            val addresses = wm.getAddressList()
                            wm.switchWallet(prev)
                            if (addresses.isEmpty()) {
                                Toast.makeText(this, "Chưa có địa chỉ", Toast.LENGTH_SHORT).show()
                            } else {
                                AlertDialog.Builder(this).setTitle("Địa chỉ của $name")
                                    .setItems(addresses.toTypedArray()) { _, idx ->
                                        val addr = addresses[idx]
                                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("address", addr))
                                        Toast.makeText(this, "Đã copy: $addr", Toast.LENGTH_SHORT).show()
                                    }.show()
                            }
                        }
                        3 -> {
                            // Xóa
                            wm.deleteWallet(name)
                            loadWallets()
                        }
                    }
                }.show()
            true
        }
    }

    // loadWallets
    private fun loadWallets() {
        // Lấy danh sách
        val wallets = wm.listWallets()
        // Ví hiện tại
        val current = wm.getCurrentWalletName()
        // Thêm dấu check
        val display = wallets.map { if (it == current) "$it ✓" else it }
        // Set adapter
        binding.walletList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
    }

    // renameWallet
    private fun renameWallet(oldName: String) {
        // Input
        val input = EditText(this)
        // Set text
        input.setText(oldName)
        // Dialog
        AlertDialog.Builder(this).setTitle("Đổi tên ví").setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != oldName) {
                    if (wm.renameWallet(oldName, newName)) {
                        Toast.makeText(this, "Đã đổi tên", Toast.LENGTH_SHORT).show()
                        loadWallets()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}