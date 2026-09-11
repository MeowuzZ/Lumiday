# Lumiday 2.5

离线 Android 日程与待办应用，需要 **Android 16+**。

[下载最新 APK](https://github.com/MeowuzZ/Lumiday/releases/latest/download/Lumiday.apk)

推送 main 后自动构建发布，固定下载链接始终指向最新版本。安装包统一在 [Releases](https://github.com/MeowuzZ/Lumiday/releases) 分发。



## 本版功能

- 点击“日程”打开模糊背景的日历浮层。上滑时其他周渐隐收缩，当前周移到顶部；下滑展开月历。取消月 / 周切换按钮。
- 日历任务统一白字、红橙蓝绿优先级背景。长按切换完成状态，完成后背景变淡、文字加删除线；时间轴采用同样规则。
- 未完成跨日任务优先显示并连接各天色块，日期数字不着色。常规与已完成内容位于后面，超量显示 +N。
- 时间轴初始至少三轨，重叠较多时增加轨道。双指缩放围绕触点调整间距，范围为一屏约 15 小时至 2 小时；最小比例可完整看到 06:00–21:00。过长文字用省略号显示。
- 时间日程支持开始 / 结束日期及时间，跨午夜按每日实际覆盖时段显示。新建或修改开始时间时不能早于本机当前分钟；未改开始时间的历史任务仍可编辑备注。
- 全天待办可设置开始日和结束日，或清除日期。无日期的未完成待办顺延到今天，完成后保留在完成当天，不复制任务。
- 今天页只显示今天，四象限可按优先级添加，纯图标导航与可拖动添加按钮保留。
- 设置页使用用户提供的艺术字参考图，按“时间 / 只属 / 于你”断句，慢速落叶动效；滑动显示备份与重置按钮。

## 更新日志

以下日期按仓库提交记录整理；同一版本可以有多个自动构建，具体构建见 [Releases](https://github.com/MeowuzZ/Lumiday/releases)。应用版本、备份格式版本与自动构建编号分别管理。

| 版本 / 日期 | 更新内容 |
| --- | --- |
| **2.5 · 2026-09-11** | 接入 1:1 阳光油画图标；开屏、图标底色及系统栏统一为全屏绿色；配置 main 自动构建、签名与发布；新增固定下载二维码及美化分享卡。 |
| **2.4 · 2026-09-10** | 月周历改为随手势连续收缩；移除月 / 周按钮；统一优先级背景和完成删除线；时间轴双指缩放、至少三轨及文字省略；支持跨日时间、全天日期范围与无日期待办顺延；备份升级为 v5。 |
| **2.3 · 2026-09-10** | 日历从“日程”标题处弹出，底层日程高斯模糊；任务详情统一为圆角底部面板；设置页使用参考艺术字，断句为“时间 / 只属 / 于你”。 |
| **2.2 · 2026-09-10** | 新增带任务色条和 +N 的月 / 周总览；新增 0–24 时时间轴及重叠分轨；点击详情、长按完成；移除仅开始时间模式；加入落叶动效；备份升级为 v4。 |
| **2.1 · 2026-09-10** | 今天页固定展示当天且不显示日历；恢复红橙蓝绿四象限及按象限添加；设置页增加全屏文字和滑动显现备份按钮。 |
| **2.0 · 2026-09-10** | 最低系统提升为 Android 16；改为纯图标导航；添加按钮支持拖动与位置记忆；移除习惯模块，旧习惯数据保留在备份；支持全天及时间段编辑；备份格式 v3。 |
| **1.0 · 2026-09-09** | 初始离线版本：任务、日历、习惯与本地备份。习惯功能在后续版本中已移除。 |

## 各功能实现说明

应用使用 **Java + Android 原生 View / Canvas**，业务不依赖网络服务。下面的链接可直接定位实现代码。

### 今天与四象限

[MainActivity.java](app/src/main/java/com/lumiday/app/MainActivity.java) 负责页面切换、任务列表与交互。今天页以本机 `LocalDate.now()` 取当天内容，不受日程页已选日期影响；今天和四象限不显示顶部日历。四象限汇总不同日期的任务，点击相应区域创建任务时预选优先级。

| 优先级值 | 色彩 | 象限 |
| --- | --- | --- |
| 3 | 红 | 重要且紧急 |
| 2 | 橙 | 重要不紧急 |
| 1 | 蓝 | 不重要但紧急 |
| 0 | 绿 | 不重要不紧急 |

今天页将已完成任务单独分组，可展开或收起；日历和时间轴长按任务可以切换完成状态。完成后使用淡色底、白字与删除线。

### 月周历浮层与连续动画

[CalendarPopup.java](app/src/main/java/com/lumiday/app/CalendarPopup.java) 将日历作为独立 Dialog 从标题附近展开，对底层页面应用 `RenderEffect.createBlurEffect`。关闭后清除模糊并刷新任务显示。

[CalendarOverview.java](app/src/main/java/com/lumiday/app/CalendarOverview.java) 使用 0–1 的收缩进度控制每周行高与透明度：其他周逐渐缩短、淡出，已选日期所在周保持可见并移到顶部，随后展开更多任务。手势结束由 `ValueAnimator` 平滑吸附到月历或周历状态；没有额外的月 / 周切换按钮。

任务色条位于日期数字下方。月历通常显示两条、周历最多五条，超出部分用 +N 表示；点击日期或 +N 返回对应日期的日程。跨日未完成任务在一周内使用一致的排列位置与相连色块，跨周则在下一行继续显示。未完成的特殊任务优先，常规任务及已完成任务排在后面。

### 时间轴、重叠分轨与缩放

[TimelineView.java](app/src/main/java/com/lumiday/app/TimelineView.java) 绘制 0–24 时刻度，图块顶部由开始分钟数计算，高度由当天持续分钟数计算。事件按开始时间排序，在相互重叠的一组中复用已空出的轨道；结束时间恰好等于另一个开始时间时不算重叠。每组至少三轨，需要更多轨道时等宽细分。

[TimelineScroll.java](app/src/main/java/com/lumiday/app/TimelineScroll.java) 使用原生 `ScaleGestureDetector` 处理双指缩放。缩放时保留触点对应的时间位置，动态调整每小时高度与滚动偏移；下限约为一屏 15 小时，上限约为一屏 2 小时。调整到 06:00 后，最小比例可完整看到 06:00–21:00。文本行数随图块高度变化，放不下的内容以省略号结束。

点击图块打开统一的任务详情面板，可编辑或删除；长按切换完成状态，并保留原滚动位置。

### 任务编辑与日期校验

[TaskEditor.java](app/src/main/java/com/lumiday/app/TaskEditor.java) 使用独立草稿承载标题、备注、日期、时间和优先级，只有校验通过并点击保存才写入数据。取消不会保存，页面重建可恢复草稿。

- **全天待办**：时间字段为空，可设置开始日 / 结束日，或清除日期。
- **时间日程**：必须同时具备开始和结束时间，默认开始时间取本机当前时间，默认结束时间为一小时后，必要时跨到次日。
- 新建或修改开始时间不能早于本机当前分钟；未改变开始时间的历史任务仍可修改其他内容。
- 同日结束必须晚于开始；跨日结束日期不得早于开始；支持结束于 24:00。
- 备份导入不套用“禁止新建过去时间”规则，历史记录可以恢复。

### 跨日显示与无日期待办顺延

[TaskDates.java](app/src/main/java/com/lumiday/app/TaskDates.java) 统一计算任务是否属于某一天，以及当天应显示的时间范围。

例如 9 月 10 日 23:00 至 9 月 11 日 01:00，会显示为前一天的 23:00–24:00 和后一天的 00:00–01:00；仅绘制两个视图片段，底层仍是一条任务。结束于次日 00:00 时，不在结束日生成零长度图块。

无日期待办以 `undated` 标记，内部保留创建日期；未完成时显示在今天，完成后以 `completedDate` 固定到完成当天。顺延通过日期筛选实现，不复制任务。应用在前台检查日期变化，重新进入时也会刷新；因此不需要依赖后台定时任务改写记录。

### 导航与悬浮添加按钮

[IconView.java](app/src/main/java/com/lumiday/app/IconView.java) 绘制统一尺寸的导航与操作图标，底部导航不显示文字。添加按钮作为应用内部浮层支持自由拖动，通过 `SharedPreferences` 记录位置；点击直接进入任务编辑。设置页可重置位置，不申请跨应用悬浮窗权限。

### 设置动效与开屏

[SettingsScene.java](app/src/main/java/com/lumiday/app/SettingsScene.java) 使用 Canvas 展示艺术字素材，并绘制缓慢飘落的树叶。滑动进度控制文字淡出与备份按钮显现；离开页面后停止动画。当前艺术字来自参考图，不依赖系统字体替代。

[styles.xml](app/src/main/res/values/styles.xml) 将系统 SplashScreen、图标底色及状态栏、导航栏统一为 `launcher_sage`；[自适应图标](app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) 引用阳光油画素材，避免开屏出现白色留边。应用运行所需的图标和艺术字素材保留在 `app/src/main/res` 与 `app/src/main/assets`。

### 本地存储、备份与自动发布

[Store.java](app/src/main/java/com/lumiday/app/Store.java) 使用 JSON 保存任务及历史归档，通过 `AtomicFile` 写入；持久化失败时恢复内存快照。导入先验证格式，再确认替换，同时保存恢复前快照。备份格式与迁移规则见下文“本地数据与升级”。

[android.yml](.github/workflows/android.yml) 在 main 推送或手动触发时构建、Lint 检查、签名并上传 `Lumiday.apk` 到 GitHub Releases。PR 仅验证，不读取签名 Secret、不发布。自动版本代码为 `10000 + github.run_number`，应用版本名由 Gradle 管理。发布时从 `LUMIDAY_KEYSTORE_BASE64` Secret 恢复临时签名文件，生成 APK 与 SHA-256 校验文件，并标记最新 Release；完成后删除临时签名文件。


## 本地数据与升级

所有数据保存在应用内部，无网络权限、账号或云同步。备份格式 v5，兼容 v1–v4；旧版仅开始时间记录迁移为全天，原时间保留。新字段包括结束日期、无日期待办标记和完成日期。

| 字段 | 含义 |
| --- | --- |
| `date` / `endDate` | 开始日 / 结束日；旧数据缺少结束日时视为同一天。 |
| `time` / `endTime` | 起止时间，均为空时为全天待办；结束可用 `24:00`。 |
| `undated` | 无日期全天待办标记；内部日期用于保留创建日。 |
| `done` / `completedDate` | 完成状态 / 完成日期。 |
| `priority` | 0 无、1 低、2 中、3 高优先级。 |
| `legacyStartTime` | 旧版仅开始时间任务迁移时保留的原始时间。 |

导入上限 10 MB；未知未来版本与无效日期时间不会覆盖当前数据。恢复前保存 `before-restore.json` 快照。旧习惯数据保留在备份中，但不再提供打卡界面。卸载会删除应用内部数据，外部导出备份应自行保存。

自动发布的 `Lumiday.apk` 是 release 构建，签名沿用此前仓库 APK 的证书，可覆盖安装同源旧版。签名文件由仓库 Secret 提供，不提交到源码。验证用 debug artifact 可能使用不同证书，请优先下载 Releases 中的安装包。当前沿用的是原有 debug 签名身份；更新前建议导出备份，更换签名需要另行规划迁移。

## 构建与验证

Java 17、Android SDK 36、AGP 8.9.1、Gradle 8.11.1。

```sh
./gradlew assembleDebug assembleAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.lumiday.app.test/com.lumiday.app.SmokeInstrumentation
```

Android 16.1 模拟器通过回归与边界测试。测试使用独立数据目录。尚未完成实体手机多厂商性能验证。


## 仓库结构

```text
.github/workflows/  自动构建与发布
app/               应用源码、运行素材、设备测试
gradle/            Gradle Wrapper
README.md          功能说明、更新日志、构建与备份说明
```

仓库只保留软件开发与构建所需内容。历史截图、宣发素材、独立说明文档和旧 APK 已从当前目录清理；软件版本通过 Releases 下载。
