from pathlib import Path
import base64,html
import cairosvg
p=Path(__file__).parent
icon=base64.b64encode((p.parent/'branding/lumiday-sun-icon.png').read_bytes()).decode()
parts=[f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1200" height="1600" viewBox="0 0 1200 1600"><defs><linearGradient id="bg" x2="1" y2="1"><stop stop-color="#e0eddf"/><stop offset="1" stop-color="#fbf8eb"/></linearGradient><clipPath id="logo"><rect x="72" y="68" width="100" height="100" rx="28"/></clipPath></defs><rect width="1200" height="1600" fill="url(#bg)"/><image x="72" y="68" width="100" height="100" clip-path="url(#logo)" xlink:href="data:image/png;base64,{icon}"/>''']
def text(x,y,s,size=26,color='#294b38',weight='normal'):
 parts.append(f'<text x="{x}" y="{y}" font-family="PingFang SC,sans-serif" font-size="{size}" fill="{color}" font-weight="{weight}">{html.escape(s)}</text>')
def rect(x,y,w,h,fill,rx=22):parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}"/>')
text(198,112,'Lumiday / 开发者版图',34,weight='600');text(198,153,'公开源码 · 自由探索你的日程设计',22,'#76956d')
text(72,247,'从一份源码，长出你的版本。',49,weight='600')
text(72,300,'Java + Android 原生 View / Canvas  ·  Android 16+',24,'#76956d')
text(72,372,'01  已实现的功能与代码入口',29,weight='600')
items=[('今日任务 / 优先级','MainActivity · IconView','当天筛选、四象限、拖动添加'),('月周历 / 连续折叠','CalendarPopup · CalendarOverview','模糊浮层、手势收缩、任务色条'),('时间轴 / 双指缩放','TimelineView · TimelineScroll','持续时间图块、重叠分轨、长按切换'),('跨日 / 待办顺延','TaskDates · TaskEditor','起止日期校验、跨日切分、无日期顺延'),('本地数据 / 备份','Store · AtomicFile · JSON','格式校验、历史迁移、恢复前快照'),('动效 / 自动发布','SettingsScene · GitHub Actions','落叶与文字动效、签名构建、最新版下载')]
for i,(a,b,c) in enumerate(items):
 x=72+(i%2)*544;y=405+(i//2)*170
 rect(x,y,512,148,'#ffffff');text(x+24,y+40,a,25,weight='600');text(x+24,y+77,b,20,'#76956d');text(x+24,y+115,c,19,'#687866')
text(72,978,'02  你可以继续拓展',29,weight='600')
for i,(title,body) in enumerate([('视觉与交互','换主题、字体、图标；重绘卡片与动画'),('日程与视图','探索重复任务、筛选统计、自定义布局'),('能力与连接','自行接入提醒、日历导入或同步方案')]):
 y=1010+i*100;rect(72,y,1056,84,'#d4e4cf',18);text(96,y+34,title,23,weight='600');text(310,y+34,body,23);text(96,y+64,'扩展方向 · 需自行开发' if i==2 else '按你的偏好设计与实现',17,'#76956d')
text(72,1364,'保留数据迁移与备份校验，让改造后的版本也能安心升级。',22,'#687866')
rect(72,1410,1056,120,'#294b38',24);text(101,1456,'github.com/MeowuzZ/Lumiday',31,'#ffffff',weight='500');text(101,1498,'欢迎体验、交流，也期待看到你的个性化作品。',23,'#dce9d4')
parts.append('</svg>');svg=''.join(parts);(p/'03-developer-map.svg').write_text(svg);cairosvg.svg2png(bytestring=svg.encode(),write_to=str(p/'03-developer-map.png'))
