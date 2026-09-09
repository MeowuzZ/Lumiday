# 数据备份契约

当前 schemaVersion 为 2。日期采用本地日历 `YYYY-MM-DD`，时间为 `HH:mm`，空字符串表示全天。习惯按用户选定日期累计计数；总打卡天数统计达到每日目标的日期，当前连续以选定日期为基准，若当天未完成则从前一天开始计算。修改目标会按新目标重新计算历史完成状态。

```json
{
  "schemaVersion": 2,
  "tasks": [
    {"id":"example-task","title":"读书","note":"读一章","date":"2026-09-09","time":"20:00","priority":2,"done":false}
  ],
  "habits": [
    {"id":"example-habit","title":"喝水","note":"记得补充水分","emoji":"💧","target":3,"unit":"杯","logs":{"2026-09-09":2}}
  ]
}
```

priority：0 无，1 低，2 中，3 高。未设置 schemaVersion 按 v1 处理。v1 允许省略 id、habits 内 logs；读取时补齐 UUID 与空打卡映射。tasks / habits 数组以及标题、任务日期是必需字段。

导入先完整解析与校验，再提示用户确认；解析失败、日期不合法、目标小于 1、计数为负、重复 ID、未来版本等均不替换原数据。导入限制 10 MB。恢复前保存 `before-restore.json`，只保留最近一次恢复前快照。

正常保存使用 Android AtomicFile：写临时文件、同步并提交；出现写入异常时回滚磁盘文件。所有业务数据位于应用私有目录，系统云备份关闭。

## 后续版本维护规则

1. 保留 applicationId `com.lumiday.app` 和签名密钥；增加 versionCode。
2. 数据格式变化必须提升 schemaVersion，添加从上一版到新版的显式迁移；保留旧格式测试样本。
3. 禁止为处理解析错误直接清空文件。损坏文件必须保留供导出恢复。
4. 发布前验证覆盖安装、旧备份导入、完整导出再导入、异常输入不改变现有数据。
5. 当前自动兼容仅覆盖已定义的 v1 / v2；未知未来格式应拒绝读取，避免静默丢失数据。
