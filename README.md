# 汽水歌单助手（Android）0.1.0

这是一个可由 GitHub Actions 云端编译的 Android 起步工程，适配 Android 6.0 及以上系统，目标 SDK 35。

## 当前版本功能
- 从手机文件选择器多选本地音频文件
- 显示本地音频列表
- 点击列表项播放音频
- 粘贴 HTTP/HTTPS 歌单链接并进行格式检查

## 当前版本尚未实现
- 汽水音乐官方歌单接口接入与自动解析
- 在线音频下载
- MP3 转码及 320kbps 导出

这些功能需要合法授权的 API/音源，以及额外的音频处理模块。不要将 DRM 保护或未获授权的音频下载、转换加入应用。

## 用手机编译 APK
1. 将此压缩包解压。
2. 将解压后的**文件夹内部所有内容**上传到 GitHub 仓库根目录，必须保留 `.github/workflows/build-apk.yml` 路径。
3. 打开仓库的 Actions 页面，进入 `Build Android APK` 工作流。
4. 等待运行成功后，打开本次运行页面底部的 Artifacts，下载 `soda-playlist-helper-debug-apk`。
5. 解压下载的 artifact，得到 `app-debug.apk`，在 Android 手机上安装。

## 工程说明
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Gradle 8.9（由 GitHub Actions 安装）
- JDK 17
