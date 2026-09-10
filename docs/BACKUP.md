# 备份格式 v4

当前 schemaVersion 为 4，兼容导入 v1 / v2 / v3。日期格式 `YYYY-MM-DD`，时间 `HH:mm`。

```json
{
  "schemaVersion": 4,
  "tasks": [
    {"id":"task-1","title":"全天待办","date":"2026-09-10","time":"","endTime":"","priority":0,"done":false},
    {"id":"task-3","title":"时间段","date":"2026-09-10","time":"14:00","endTime":"15:30","priority":2,"done":false}
  ],
  "habits": []
}
```

- 时间为空表示全天待办。设置 time 时必须同时设置 endTime，表示同一天的时间段，结束可用 24:00。
- endTime 必须晚于 time；结束时间不能单独存在，不支持跨天时间段。
- priority 为 0 无、1 低、2 中、3 高。
- 没有 schemaVersion 按 v1 处理；旧版仅开始时间任务迁移为全天，原时间保存在 legacyStartTime，详情可查看。缺少 id 自动补齐 UUID。
- 习惯模块已移除。旧版 habits 数据完整保留在本地文件和导出文件中，不显示、不继续打卡。新版无 habits 字段的备份也可导入。

先完整校验备份，再确认替换。未来版本、无效日期 / 时间段、重复 ID、负打卡计数等不会覆盖当前数据；导入文件上限为 10 MB。恢复前保存最近一次 `before-restore.json`，可在设置中导出。

正常数据写入采用 AtomicFile，持久化异常会恢复内存快照并提示错误。首次读取会在内存中适配旧版格式，之后成功保存时写出 v4。

升级须保持 applicationId 和签名证书，递增 versionCode。应用内备份快照不能替代外部备份，卸载会一并删除。后续格式变化必须添加显式迁移及测试，不能保证未知未来格式的兼容。
