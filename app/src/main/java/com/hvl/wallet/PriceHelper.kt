// FILE: PriceHelper.kt
// TÁC DỤNG: Lấy giá BTC từ internet

package com.hvl.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object PriceHelper {
    // Lấy giá BTC/USD từ Coingecko (miễn phí)
    suspend fun getBtcPrice(): Double = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd").readText()
            JSONObject(json).getJSONObject("bitcoin").getDouble("usd")
        } catch (e: Exception) {
            0.0
        }
    }
    
    // Lấy phí mạng từ mempool.space
    suspend fun getFeeRate(): Long = withContext(Dispatchers.IO) {
        try {
            val json = URL("https://mempool.space/api/v1/fees/recommended").readText()
            JSONObject(json).getLong("hourFee") // sat/vB
        } catch (e: Exception) {
            10 // mặc định 10 sat/vB
        }
    }
}