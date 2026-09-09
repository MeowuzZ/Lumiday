# 验证记录

2026-09-09，本机 Android 36.1 ARM64 模拟器。

- `assembleDebug`、`assembleAndroidTest`、`lintDebug`：通过。
- 安装应用与 instrumentation 测试包：通过。
- 设备测试输出：

```text
PASS: persistence, partial progress, migration, restore, snapshot, invalid backups, five screens, calendar animation, habit statistics
```

验证任务完成持久化、习惯 2/3 进度持久化、v1 转 v2、备份往返、恢复前快照、未来版本不覆盖已有数据、非法日期拒绝、五个主页面加载、周历/月历动画状态、习惯累计完成统计。

已核对实际模拟器首页截图。未进行物理手机、多厂商 ROM、长时间压力、正式签名覆盖升级测试。Lint 尚有中文硬编码/无障碍触摸及目标 SDK 更新提示；不影响本次 debug 构建。AGP 7.4.2 对 compileSdk 34 有工具版本提示，后续升级构建工具需重新验证。
