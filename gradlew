#!/usr/bin/env sh
# FILE: gradlew
# TÁC DỤNG: Giả lập Gradle Wrapper để GitHub Actions chạy lệnh ./gradlew
# Vì chúng ta build trên máy ảo, không cần tải Gradle nặng, dùng gradle có sẵn

# Chuyển mọi lệnh ./gradlew thành lệnh gradle hệ thống
# Ví dụ: ./gradlew assembleDebug -> gradle assembleDebug
exec gradle "$@"