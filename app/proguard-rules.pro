# FILE: proguard-rules.pro
# TÁC DỤNG: Giữ nguyên code bitcoinj khi build release
-keep class org.bitcoinj.** { *; }
-keep class org.bouncycastle.** { *; }