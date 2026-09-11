# Lumiday brand assets

- `lumiday-sun-icon.png`: 1:1 原始阳光油画图标，已接入 Android 自适应桌面图标。
- `download-qr.png`: 原始高纠错二维码。
- `download-card.svg`: 可编辑的美化分享卡。
- `download-card.png`: 美化分享卡 PNG。

二维码固定指向 https://github.com/MeowuzZ/Lumiday/releases/latest/download/Lumiday.apk 。每次 workflow 发布完成后自动指向最新版。微信内可能拦截 APK 下载，需要选择“在浏览器打开”；未保证绕过微信限制。

使用内置 imagegen 工具生成图标。提示词：

> Use case: logo-brand. Create a final 1:1 square Android app icon for Lumiday, a calm offline calendar app. Main subject: a single radiant warm cream-gold sun, centered, bold simple circular sun silhouette with short painterly rays, occupying middle 55% safe area. Medium: refined hand-painted oil painting, tactile impasto brush strokes, subtle linen canvas texture, elegant rather than childish. Background: soft sage green and pale mint with gentle warm ivory light, muted palette #76956d #d9efdf #f5f6f2, small warm golden accents. Composition: edge-to-edge square artwork, no rounded-corner frame baked into image, no text, no letters, no watermark, no surrounding mockup; recognizable at small launcher size. Sun is the clear focal point, peaceful fresh morning light. Output one square image.

二维码通过脚本确定性生成，再添加浅绿奶油背景、图标和外框。没有覆盖码点；原始 PNG、美化 PNG 和 600×800 缩小图均通过 ZXing 解码校验。

```sh
pip install qrcode pillow zxing-cpp cairosvg
python scripts/make_download_qr.py
```

macOS 安装 Cairo 后可能需要设置 `DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib`。
