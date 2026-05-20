package com.hvl.wallet // Khai báo package

// --- IMPORT ---
import android.content.Intent // Dùng để chuyển màn hình
import android.os.Bundle // Nhận dữ liệu khi tạo Activity
import android.widget.ArrayAdapter // Adapter cho ListView
import android.widget.Toast // Hiện thông báo nhanh
import androidx.appcompat.app.AppCompatActivity // Activity cơ bản
import com.hvl.wallet.databinding.ActivityMainBinding // Binding từ XML
import kotlinx.coroutines.* // Coroutine để chạy background

// Khai báo class MainActivity kế thừa AppCompatActivity
class MainActivity : AppCompatActivity() {

    // Biến binding để truy cập view trong XML
    private lateinit var binding: ActivityMainBinding

    // Biến quản lý ví Bitcoin
    private lateinit var wm: WalletManager

    // Tạo scope chạy trên Main thread
    private val scope = CoroutineScope(Dispatchers.Main)

    // Hàm chạy khi Activity được tạo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Gọi hàm cha

        // Khởi tạo binding từ layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Gán view root cho activity
        setContentView(binding.root)

        // Khởi tạo WalletManager
        wm = WalletManager(this)
        // Bắt đầu ví (load wallet, kết nối mạng)
        wm.start()

        // Hiển thị địa chỉ ví hiện tại
        binding.addressText.text = wm.getCurrentAddress()
        // Hiển thị số dư
        binding.balanceText.text = wm.getBalance()

        // Lấy giá BTC bằng coroutine
        scope.launch {
            // Chạy trên IO để gọi API
            val price = withContext(Dispatchers.IO) { PriceHelper.getBtcPrice() }
            // Cập nhật lên TextView
            binding.priceText.text = "BTC: $${price.toInt()}"
        }

        // Lấy lịch sử giao dịch
        val history = wm.getTransactionHistory()
        // Gán adapter cho ListView
        binding.historyList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, history)

        // Nút Nhận -> mở ReceiveActivity
        binding.receiveBtn.setOnClickListener {
            startActivity(Intent(this, ReceiveActivity::class.java))
        }

        // Nút Gửi -> mở SendActivity
        binding.sendBtn.setOnClickListener {
            startActivity(Intent(this, SendActivity::class.java))
        }

        // Nút Quản lý ví -> mở ManageWalletsActivity
        binding.manageBtn.setOnClickListener {
            startActivity(Intent(this, ManageWalletsActivity::class.java))
        }

        // Nút Xuất Seed -> hiện Toast (chỉ test)
        binding.seedBtn.setOnClickListener {
            Toast.makeText(this, "Seed: ${wm.getSeedPhrase()}", Toast.LENGTH_LONG).show()
        }
    }

    // Hàm chạy khi Activity bị hủy
    override fun onDestroy() {
        super.onDestroy()
        wm.stop() // Dừng ví
        scope.cancel() // Hủy coroutine
    }
}