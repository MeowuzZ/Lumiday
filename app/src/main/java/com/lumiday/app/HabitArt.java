package com.lumiday.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Resolution-independent illustration, drawn locally without downloaded assets. */
final class HabitArt extends View {
 private final Paint paint = new Paint(3);
 private final String emoji;
 private final float progress;
 HabitArt(Context context, String emoji, float progress) {
  super(context); this.emoji = emoji; this.progress = Math.min(1, progress);
  setContentDescription(emoji.equals("🍎") ? "水果插画" : "习惯进度插画");
 }
 private void oval(Canvas c, int color, float l, float t, float r, float b) {
  paint.setColor(color); c.drawOval(l,t,r,b,paint);
 }
 @Override protected void onDraw(Canvas canvas) {
  super.onDraw(canvas); canvas.save(); canvas.scale(getWidth()/360f,getHeight()/240f);
  oval(canvas,0x22605f50,55,208,310,229);
  if(emoji.equals("🍎")) {
   oval(canvas,0xff13b77d,20,63,192,203);
   paint.setColor(0xff7edd69);paint.setStrokeWidth(14);paint.setStyle(Paint.Style.STROKE);
   for(int i=0;i<3;i++){Path stripe=new Path();stripe.moveTo(35,100+i*32);stripe.cubicTo(80,70+i*32,125,140+i*22,175,110+i*22);canvas.drawPath(stripe,paint);}paint.setStyle(Paint.Style.FILL);
   float lift=progress>=1?18:0;
   oval(canvas,0xffff5638,135,104-lift,222,199-lift);
   paint.setColor(0xff845033);canvas.drawRect(177,87-lift,184,122-lift,paint);
   oval(canvas,0xffff9638,208,112-lift,305,204-lift);
   oval(canvas,0xffa8f164,232,91-lift,282,113-lift);
   for(int i=0;i<9;i++)oval(canvas,0xff7850b9,89+(i%3)*18,157+(i/3)*17,110+(i%3)*18,178+(i/3)*17);
   paint.setColor(0xffffe23a);Path banana=new Path();banana.moveTo(235,180);banana.quadTo(290,197,342,143);banana.quadTo(325,223,237,198);banana.close();canvas.drawPath(banana,paint);
   paint.setColor(Color.WHITE);Path plate=new Path();plate.moveTo(40,205);plate.lineTo(328,205);plate.quadTo(270,251,128,226);plate.quadTo(72,224,40,205);canvas.drawPath(plate,paint);
  } else if(emoji.equals("💧")) {
   paint.setColor(0x66ffffff);Path glass=new Path();glass.moveTo(105,40);glass.lineTo(255,40);glass.lineTo(238,210);glass.quadTo(180,222,122,210);glass.close();canvas.drawPath(glass,paint);
   canvas.save();canvas.clipPath(glass);paint.setColor(0xff3daed2);float level=198-145*progress;canvas.drawRect(108,level,255,220,paint);oval(canvas,0xff89e6ef,108,level-9,253,level+9);canvas.restore();
   paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(5);canvas.drawPath(glass,paint);paint.setStyle(Paint.Style.FILL);
   oval(canvas,0x99ffffff,140,155,149,164);oval(canvas,0x99ffffff,200,125,213,138);
  } else {
   oval(canvas,0x33ffffff,85,28,275,219);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(100);canvas.drawText(emoji,180,163,paint);
  }
  canvas.restore();
 }
}
