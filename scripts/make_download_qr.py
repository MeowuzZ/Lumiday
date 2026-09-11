"""Deterministic QR + vector presentation. Never paints over encoded modules."""
from pathlib import Path
import base64
import qrcode
import cairosvg
import zxingcpp
from PIL import Image

root = Path(__file__).resolve().parents[1]
out = root / 'branding'
out.mkdir(exist_ok=True)
url = 'https://github.com/MeowuzZ/Lumiday/releases/latest/download/Lumiday.apk'
qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=12, border=4)
qr.add_data(url)
qr.make(fit=True)
qr.make_image(fill_color='#244C3B', back_color='white').save(out / 'download-qr.png')
matrix = qr.get_matrix()
unit = 12
size = len(matrix) * unit
x = (1200-size)//2
y = 450
modules = ''.join(f'<rect x="{x+c*unit}" y="{y+r*unit}" width="{unit}" height="{unit}"/>'
                  for r,row in enumerate(matrix) for c,on in enumerate(row) if on)
icon = base64.b64encode((out/'lumiday-sun-icon.png').read_bytes()).decode()
svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1200" height="1600" viewBox="0 0 1200 1600">
<defs>
 <linearGradient id="paper" x2="1" y2="1"><stop stop-color="#e0eddf"/><stop offset="1" stop-color="#faf8e9"/></linearGradient>
 <clipPath id="icon"><rect x="522" y="90" width="156" height="156" rx="40"/></clipPath>
</defs>
<rect width="1200" height="1600" fill="url(#paper)"/>
<rect x="48" y="48" width="1104" height="1504" rx="48" fill="none" stroke="#a1b89a" stroke-width="2"/>
<image x="522" y="90" width="156" height="156" clip-path="url(#icon)" xlink:href="data:image/png;base64,{icon}"/>
<text x="600" y="328" text-anchor="middle" fill="#294b38" font-family="Georgia,serif" font-size="66" letter-spacing="3">Lumiday</text>
<text x="600" y="382" text-anchor="middle" fill="#76956d" font-family="PingFang SC,sans-serif" font-size="26" letter-spacing="8">时间只属于你</text>
<rect x="{x-30}" y="{y-30}" width="{size+60}" height="{size+60}" rx="32" fill="white"/>
<g fill="#244c3b">{modules}</g>
<g fill="none" stroke="#92ae83" stroke-width="2">
 <path d="M150 1330 Q200 1275 165 1230 M173 1288 Q118 1280 144 1250 Q177 1250 173 1288 M173 1288 Q219 1260 204 1240 Q173 1240 173 1288"/>
 <path d="M1050 1330 Q1000 1275 1035 1230 M1027 1288 Q1082 1280 1056 1250 Q1023 1250 1027 1288 M1027 1288 Q981 1260 996 1240 Q1027 1240 1027 1288"/>
</g>
<text x="600" y="1370" text-anchor="middle" fill="#294b38" font-family="PingFang SC,sans-serif" font-size="34">扫码下载最新版</text>
<text x="600" y="1423" text-anchor="middle" fill="#76956d" font-family="PingFang SC,sans-serif" font-size="23">Android 16+  ·  本地存储  ·  轻盈日程</text>
<text x="600" y="1480" text-anchor="middle" fill="#7b8877" font-family="PingFang SC,sans-serif" font-size="20">微信若未开始下载，请在右上角选择“在浏览器打开”</text>
</svg>'''
(out/'download-card.svg').write_text(svg)
cairosvg.svg2png(bytestring=svg.encode(), write_to=str(out/'download-card.png'))
# Validate both original and decorated results, plus a smaller shared-image size.
for path in (out/'download-qr.png',out/'download-card.png'):
    image = Image.open(path)
    result = zxingcpp.read_barcode(image)
    assert result and result.text == url, f'QR decode failed: {path}'
    if path.name == 'download-card.png':
        reduced = image.resize((600,800))
        result = zxingcpp.read_barcode(reduced)
        assert result and result.text == url, 'Shared-size QR decode failed'
print('Verified original, decorated, and 600x800 QR:', url)
