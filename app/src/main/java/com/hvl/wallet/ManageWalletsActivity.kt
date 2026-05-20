// Khai báo package của app
package com.hvl.wallet

// Import lớp Intent để chuyển màn hình
import android.content.Intent
// Import lớp Bundle để nhận dữ liệu khi tạo activity
import android.os.Bundle
// Import ArrayAdapter để đổ dữ liệu vào ListView
import android.widget.ArrayAdapter
// Import EditText để tạo ô nhập liệu
import android.widget.EditText
// Import LinearLayout để tạo layout chứa nhiều view
import android.widget.LinearLayout
// Import Toast để hiện thông báo nhanh
import android.widget.Toast
// Import AlertDialog để hiện hộp thoại
import androidx.appcompat.app.AlertDialog
// Import AppCompatActivity là lớp activity cơ bản
import androidx.appcompat.app.AppCompatActivity
// Import binding được sinh tự động từ layout activity_manage_wallets.xml
import com.hvl.wallet.databinding.ActivityManageWalletsBinding

// Khai báo class ManageWalletsActivity kế thừa AppCompatActivity
class ManageWalletsActivity : AppCompatActivity() {
    // Khai báo biến binding để truy cập view
    private lateinit var binding: ActivityManageWalletsBinding
    // Khai báo biến quản lý ví
    private lateinit var wm: WalletManager
    // Khai báo biến quản lý mật khẩu
    private lateinit var passHelper: PasswordHelper

    // Hàm onCreate được gọi khi activity khởi tạo
    override fun onCreate(savedInstanceState: Bundle?) {
        // Gọi hàm cha
        super.onCreate(savedInstanceState)
        // Khởi tạo binding từ layout
        binding = ActivityManageWalletsBinding.inflate(layoutInflater)
        // Gán view root cho activity
        setContentView(binding.root)
        // Khởi tạo đối tượng WalletManager
        wm = WalletManager(this)
        // Khởi tạo đối tượng PasswordHelper
        passHelper = PasswordHelper(this)
        // Gọi hàm load danh sách ví
        loadWallets()

        // Đặt lại chữ cho nút addWalletBtn
        binding.addWalletBtn.text = "Tạo ví"

        // Gán sự kiện click cho nút Tạo ví
        binding.addWalletBtn.setOnClickListener {
            // Tạo ô nhập EditText
            val input = EditText(this)
            // Đặt hint gợi ý
            input.hint = "Tên ví mới"
            // Tạo AlertDialog
            AlertDialog.Builder(this).setTitle("Tạo ví mới").setView(input)
                // Nút Tạo
                .setPositiveButton("Tạo") { _, _ ->
                    // Lấy tên ví, xóa khoảng trắng đầu cuối
                    val newName = input.text.toString().trim()
                    // Kiểm tra tên không rỗng
                    if (newName.isNotEmpty()) {
                        // Kiểm tra tên đã tồn tại chưa
                        if (wm.listWallets().contains(newName)) {
                            // Hiện thông báo trùng tên
                            Toast.makeText(this, "Tên ví đã tồn tại", Toast.LENGTH_SHORT).show()
                        } else {
                            // Gọi hàm tạo ví mới
                            wm.createNewWallet(newName)
                            // Load lại danh sách
                            loadWallets()
                            // Hiện thông báo thành công
                            Toast.makeText(this, "Đã tạo ví $newName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                // Nút Hủy
                .setNegativeButton("Hủy", null)
                // Hiện dialog
                .show()
        }

        // Gán sự kiện click cho nút Import ví
        binding.importWalletBtn.setOnClickListener {
            // Tạo LinearLayout dọc
            val layout = LinearLayout(this)
            // Đặt hướng dọc
            layout.orientation = LinearLayout.VERTICAL
            // Đặt padding
            layout.setPadding(50, 40, 50, 10)
            // Tạo ô nhập tên ví
            val nameInput = EditText(this)
            // Đặt hint
            nameInput.hint = "Tên ví"
            // Thêm vào layout
            layout.addView(nameInput)
            // Tạo ô nhập seed
            val seedInput = EditText(this)
            // Đặt hint
            seedInput.hint = "12 từ seed"
            // Thêm vào layout
            layout.addView(seedInput)
            // Tạo dialog
            AlertDialog.Builder(this).setTitle("Thêm ví từ Seed").setView(layout)
                // Nút Import
                .setPositiveButton("Import") { _, _ ->
                    // Lấy tên
                    val name = nameInput.text.toString().trim()
                    // Lấy seed
                    val seed = seedInput.text.toString().trim()
                    // Kiểm tra điều kiện
                    if (name.isEmpty() || seed.isEmpty() || seed.split(" ").size != 12) {
                        // Thông báo lỗi
                        Toast.makeText(this, "Nhập đúng tên và 12 từ seed", Toast.LENGTH_SHORT).show()
                        // Thoát khỏi lambda
                        return@setPositiveButton
                    }
                    // Thử import
                    try {
                        // Gọi hàm import
                        wm.importFromSeed(name, seed)
                        // Load lại
                        loadWallets()
                        // Thông báo
                        Toast.makeText(this, "Đã thêm ví $name", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Thông báo seed sai
                        Toast.makeText(this, "Seed không hợp lệ", Toast.LENGTH_SHORT).show()
                    }
                }
                // Nút Hủy
                .setNegativeButton("Hủy", null)
                // Hiện
                .show()
        }

        // Gán sự kiện click cho nút Đổi mật khẩu
        binding.changePassBtn.setOnClickListener {
            // Tạo layout dọc
            val layout = LinearLayout(this)
            // Đặt hướng
            layout.orientation = LinearLayout.VERTICAL
            // Đặt padding
            layout.setPadding(50, 40, 50, 10)

            // Tạo ô nhập mật khẩu cũ
            val oldPass = EditText(this)
            // Đặt hint
            oldPass.hint = "Mật khẩu cũ"
            // Đặt kiểu nhập password
            oldPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            // Thêm vào layout
            layout.addView(oldPass)

            // Tạo ô nhập mật khẩu mới
            val newPass = EditText(this)
            // Đặt hint
            newPass.hint = "Mật khẩu mới"
            // Đặt kiểu nhập password
            newPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            // Thêm vào layout
            layout.addView(newPass)

            // Tạo dialog đổi mật khẩu
            AlertDialog.Builder(this).setTitle("Đổi mật khẩu").setView(layout)
                // Nút Đổi
                .setPositiveButton("Đổi") { _, _ ->
                    // Kiểm tra mật khẩu cũ đúng không
                    if (passHelper.checkPassword(oldPass.text.toString())) {
                        // Lưu mật khẩu mới
                        passHelper.setPassword(newPass.text.toString())
                        // Thông báo thành công
                        Toast.makeText(this, "Đã đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    } else {
                        // Thông báo sai
                        Toast.makeText(this, "Sai mật khẩu cũ", Toast.LENGTH_SHORT).show()
                    }
                }
                // Nút Hủy
                .setNegativeButton("Hủy", null)
                // Hiện
                .show()
        }

        // Gán sự kiện long click cho ListView
        binding.walletList.setOnItemLongClickListener { _, _, pos, _ ->
            // Lấy tên hiển thị tại vị trí
            val displayName = binding.walletList.adapter.getItem(pos) as String
            // Xóa dấu check
            val name = displayName.replace(" ✓", "")
            // Tạo dialog menu
            AlertDialog.Builder(this).setTitle("Ví: $name")
                // Danh sách lựa chọn
                .setItems(arrayOf("Chọn dùng", "Đổi tên", "Xem địa chỉ", "Xóa")) { _, which ->
                    // Xử lý theo lựa chọn
                    when (which) {
                        // 0 = Chọn dùng
                        0 -> {
                            // Chuyển ví
                            wm.switchWallet(name)
                            // Tạo intent về MainActivity
                            val intent = Intent(this, MainActivity::class.java)
                            // Xóa activity cũ
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            // Chạy
                            startActivity(intent)
                            // Đóng activity hiện tại
                            finish()
                        }
                        // 1 = Đổi tên
                        1 -> {
                            // Tạo ô nhập
                            val input = EditText(this)
                            // Đặt text mặc định
                            input.setText(name)
                            // Tạo dialog
                            AlertDialog.Builder(this).setTitle("Đổi tên").setView(input)
                                // Nút OK
                                .setPositiveButton("OK") { _, _ ->
                                    // Gọi hàm rename
                                    if (wm.renameWallet(name, input.text.toString().trim())) {
                                        // Load lại
                                        loadWallets()
                                    }
                                }
                                // Hiện
                                .show()
                        }
                        // 2 = Xem địa chỉ
                        2 -> {
                            // Lưu ví hiện tại
                            val prev = wm.getCurrentWalletName()
                            // Chuyển sang ví cần xem
                            wm.switchWallet(name)
                            // Lấy danh sách địa chỉ
                            val addrs = wm.getAddressList()
                            // Chuyển lại ví cũ
                            wm.switchWallet(prev)
                            // Hiện dialog danh sách
                            AlertDialog.Builder(this).setTitle("Địa chỉ")
                                .setItems(addrs.toTypedArray()) { _, i ->
                                    // Lấy clipboard
                                    val c = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    // Copy địa chỉ
                                    c.setPrimaryClip(android.content.ClipData.newPlainText("addr", addrs[i]))
                                    // Thông báo
                                    Toast.makeText(this, "Đã copy", Toast.LENGTH_SHORT).show()
                                }.show()
                        }
                        // 3 = Xóa
                        3 -> {
                            // Xóa ví
                            wm.deleteWallet(name)
                            // Load lại
                            loadWallets()
                        }
                    }
                }.show()
            // Trả về true
            true
        }
    }

    // Hàm load danh sách ví
    private fun loadWallets() {
        // Lấy danh sách ví
        val wallets = wm.listWallets()
        // Lấy ví hiện tại
        val current = wm.getCurrentWalletName()
        // Tạo danh sách hiển thị có dấu check
        val displayList = wallets.map { if (it == current) "$it ✓" else it }
        // Gán adapter cho ListView
        binding.walletList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
    }
}