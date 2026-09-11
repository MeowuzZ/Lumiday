# 自动发布

推送 main 或在 main 手动运行 `Build and publish Lumiday`：

1. 使用 Java 17 / Android SDK 36 构建、Lint 检查。
2. 从仓库 Secret `LUMIDAY_KEYSTORE_BASE64` 恢复临时签名文件。
3. 构建 release APK，验证签名并生成 SHA256SUMS.txt。
4. 创建以 run number 标识的 GitHub Release，标记为 Latest，上传固定名称 Lumiday.apk。
5. 删除 runner 上的临时签名文件。

PR 只运行只读验证，不读取签名 Secret、不发布。过期提交不替换 main 的最新发布。重跑同一次 workflow 会覆盖同名 release 资产。

`versionCode = 10000 + github.run_number` 保持各次发布递增；源码 versionName 由 app/build.gradle 管理。默认本机构建使用较低版本代码，自动发布后不要用低版本代码覆盖安装。

签名与之前仓库测试 APK 一致，可以保留用户本地数据覆盖更新；目前沿用的仍是原有 Android debug 签名身份，但输出是非 debuggable release 构建。更换证书需另行设计迁移，不能简单换一把密钥。

固定链接： https://github.com/MeowuzZ/Lumiday/releases/latest/download/Lumiday.apk

遵循 [GitHub 最新 release 资产链接规则](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases)。二维码不会随版本变化。微信可能阻止 APK 下载或 GitHub 链接，请使用右上角“在浏览器打开”；GitHub 在不同地区的可达性不由应用控制。
