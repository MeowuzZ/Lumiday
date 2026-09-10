# 备份格式 v5

`schemaVersion: 5`，兼容 v1–v4。任务的 `date` 为开始日期；`endDate` 为结束日期，旧数据缺省为开始日期。

```json
{
  "schemaVersion": 5,
  "tasks": [
    {"id":"a","title":"跨日待办","date":"2026-09-10","endDate":"2026-09-12","time":"","endTime":"","undated":false,"priority":3,"done":false},
    {"id":"b","title":"跨夜日程","date":"2026-09-10","endDate":"2026-09-11","time":"23:00","endTime":"01:00","priority":1,"done":false},
    {"id":"c","title":"无日期待办","date":"2026-09-10","endDate":"2026-09-10","time":"","endTime":"","undated":true,"priority":0,"done":false}
  ],
  "habits": []
}
```

- 日期格式 YYYY-MM-DD，时间 HH:mm，结束允许 24:00。同日结束须晚于开始，跨日结束日期不能早于开始。
- 时间字段均为空表示全天；时间日程必须有完整起止时间。
- `undated: true` 仅用于全天待办，内部 date 保留创建日。未完成时显示在今天，完成后按 `completedDate` 显示，不修改或复制原记录。
- 有日期全天任务覆盖起止日；跨日时间任务按天裁切，00:00 结束的任务不在结束当天生成零长度图块。
- 优先级 0 无 / 1 低 / 2 中 / 3 高。完成标记为 done。
- v1–v3 仅开始时间任务转为全天，原值保存在 legacyStartTime；历史习惯数据保留在备份，不显示打卡功能。
- 导入不应用“禁止新建过去时间”规则，因此历史记录可完整恢复。

导入先校验，再确认替换，保存 before-restore.json。未知未来版本、无效日期时间等拒绝导入；上限 10 MB。写入使用 AtomicFile，失败回滚内存数据。

升级须保持 applicationId 与签名，并递增 versionCode。卸载会删除应用内数据，外部导出备份需要用户自行保存。
