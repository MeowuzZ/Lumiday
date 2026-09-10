# Lumiday 2.0.0 验证记录

2026-09-10，Android 16.1 ARM64 模拟器，Java 17 / SDK 36 / AGP 8.9.1。

- `assembleDebug`、`assembleAndroidTest`、`lintDebug`：通过。
- 同包名覆盖安装成功。v1 / v2 APK 证书 SHA-256 相同：`562bdd4006e479eaf5f500c0949938a383c7b6487d3d1db45ac8d481f04f8c80`。
- 设备测试结果：

```text
PASS: v1/v2/v3 backups, legacy archive, all-day/start/range editor, invalid ranges, cancellation, chronological order, four icon tabs, calendar animation, draggable overlay, persistence
```

覆盖默认全天、仅开始时间、起止时间、非法时间段阻止保存、切回全天清除起止时间、取消不保存、编辑草稿重建、按开始时间排序、四个纯图标页面、日历动画、悬浮按钮拖动与误触保护、重新读取文件、三版备份迁移及时间段往返。测试写入独立数据目录，历史习惯打卡数据在新版备份中保留。

实际截图：`home-v2.png`、`agenda-v2.png`、`editor-v2.png`。已人工核对图标尺寸、加号居中、时间显示及面板布局。

未覆盖实体手机及多厂商 ROM、跨日时间段、通知提醒。当前时间段限定同一天。Lint 无错误，仍有动态绘制分配、中文文本和部分自定义触摸无障碍提示；底部图标具备 contentDescription，悬浮按钮点击经 performClick 分发。
