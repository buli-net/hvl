// Khai báo package của ứng dụng
package com.hvl.wallet

// Import Intent để chuyển màn hình
import android.content.Intent
// Import Bundle để nhận dữ liệu khi tạo activity
import android.os.Bundle
// Import ArrayAdapter để đổ dữ liệu vào ListView
import android.widget.ArrayAdapter
// Import Toast để hiện thông báo
import android.widget.Toast
// Import AppCompatActivity là activity cơ bản
import androidx.appcompat.app.AppCompatActivity
// Import binding được sinh từ activity_main.xml
import com.hvl.wallet.databinding.ActivityMainBinding
// Import Coroutine để chạy tác vụ bất đồng bộ
import kotlinx.coroutines.*

// Khai báo class MainActivity kế thừa AppCompatActivity
class MainActivity : AppCompatActivity() {
    // Khai báo biến binding để truy cập view
    private lateinit var binding: ActivityMainBinding
    // Khai báo biến quản lý ví
    private lateinit var wm: WalletManager
    // Tạo CoroutineScope chạy trên Main thread
    private val scope = CoroutineScope(Dispatchers.Main)

    // Hàm onCreate chạy khi activity được tạo
    override fun onCreate(savedInstanceState: Bundle?) {
        // Gọi hàm cha
        super.onCreate(savedInstanceState)
        // Khởi tạo binding từ layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Gán view cho activity
        setContentView(binding.root)

        // Khởi tạo WalletManager
        wm = WalletManager(this)
        // Khởi động ví (kết nối mạng, load wallet)
        wm.start()

        // Hiển thị địa chỉ ví hiện tại lên TextView
        binding.addressText.text = wm.getCurrentAddress()
        // Hiển thị số dư lên TextView
        binding.balanceText.text = wm.getBalance()

        // Chạy coroutine để lấy giá BTC
        scope.launch {
            // Chạy trên thread IO để gọi API
            val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
            // Cập nhật giá lên TextView
            binding.priceText.text = "BTC: $${price.toInt()}"
        }

        // Lấy lịch sử giao dịch từ ví
        val history = wm.getTransactionHistory()
        // Tạo adapter cho ListView lịch sử
        binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)

        // Xử lý nút Nhận (QR)
        binding.receiveBtn.setOnClickListener {
            // Mở màn hình ReceiveActivity
            startActivity(Intent(this, ReceiveActivity::class.java))
        }

        // Xử lý nút Gửi
        binding.sendBtn.setOnClickListener {
            // Mở màn hình SendActivity
            startActivity(Intent(this, SendActivity::class.java))
        }

        // Xử lý nút Quản lý ví
        binding.manageBtn.setOnClickListener {
            // Mở màn hình ManageWalletsActivity
            startActivity(Intent(this, ManageWalletsActivity::class.java))
        }

        // Xử lý nút Xuất Seed
        binding.seedBtn.setOnClickListener {
            // Hiện seed phrase bằng Toast (cảnh báo: chỉ dùng test)
            Toast.makeText(this, "Seed: ${wm.getSeedPhrase()}", Toast.LENGTH_LONG).show()
        }

        // LƯU Ý: ĐÃ XÓA HOÀN TOÀN CODE LIÊN QUAN ĐẾN NÚT "Tạo địa chỉ mới"
    }

    // Hàm onDestroy chạy khi activity bị hủy
    override fun onDestroy() {
        // Gọi hàm cha
        super.onDestroy()
        // Dừng ví để giải phóng tài nguyên
        wm.stop()
        // Hủy coroutine scope
        scope.cancel()
    }
}