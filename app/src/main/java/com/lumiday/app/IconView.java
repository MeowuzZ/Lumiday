package com.lumiday.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** All controls share a 24 × 24 optical grid, independent of font glyph metrics. */
final class IconView extends View {
  final String symbol;
  final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  int tint;

  IconView(Context context, String symbol, int tint, String label) {
    super(context);
    this.symbol = symbol;
    this.tint = tint;
    setContentDescription(label);
    setFocusable(true);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float size = 26 * getResources().getDisplayMetrics().density;
    canvas.save();
    canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
    canvas.scale(size / 24, size / 24);
    paint.setColor(tint);
    paint.setStyle(Paint.Style.FILL);
    if (symbol.equals("tasks")) {
      canvas.drawRoundRect(2, 2, 22, 22, 6, 6, paint);
      paint.setColor(Color.WHITE);
      stroke(canvas, new float[] {7, 12, 10.5f, 15.5f, 17, 8.5f}, 2);
    } else if (symbol.equals("calendar")) {
      canvas.drawRoundRect(2, 4, 22, 22, 5, 5, paint);
      canvas.drawRoundRect(6, 1, 9, 8, 1.5f, 1.5f, paint);
      canvas.drawRoundRect(15, 1, 18, 8, 1.5f, 1.5f, paint);
      paint.setColor(Color.WHITE);
      for (int y = 11; y < 19; y += 5)
        for (int x = 7; x < 19; x += 5) canvas.drawCircle(x, y, 1.25f, paint);
    } else if (symbol.equals("grid")) {
      for (int y = 2; y <= 13; y += 11)
        for (int x = 2; x <= 13; x += 11) canvas.drawRoundRect(x, y, x + 9, y + 9, 3, 3, paint);
    } else if (symbol.equals("settings")) {
      canvas.drawCircle(12, 12, 9, paint);
      for (int i = 0; i < 6; i++) {
        canvas.save();
        canvas.rotate(i * 60, 12, 12);
        canvas.drawRoundRect(9, 0, 15, 7, 2, 2, paint);
        canvas.restore();
      }
      paint.setColor(Color.WHITE);
      canvas.drawCircle(12, 12, 3.5f, paint);
    } else if (symbol.equals("done")) {
      canvas.drawCircle(12, 12, 10, paint);
      paint.setColor(Color.WHITE);
      stroke(canvas, new float[] {7, 12, 10.5f, 15.5f, 17, 8.5f}, 2);
    } else if (symbol.equals("unchecked")) {
      paint.setAlpha(35);
      canvas.drawCircle(12, 12, 10, paint);
      paint.setAlpha(255);
      canvas.drawCircle(12, 12, 3, paint);
    } else if (symbol.equals("plus")) {
      stroke(canvas, new float[] {12, 3, 12, 21}, 2.4f);
      stroke(canvas, new float[] {3, 12, 21, 12}, 2.4f);
    } else if (symbol.equals("close")) {
      stroke(canvas, new float[] {6, 6, 18, 18}, 2);
      stroke(canvas, new float[] {18, 6, 6, 18}, 2);
    } else if (symbol.equals("left")) {
      stroke(canvas, new float[] {15, 6, 9, 12, 15, 18}, 2);
    } else {
      stroke(canvas, new float[] {9, 6, 15, 12, 9, 18}, 2);
    }
    canvas.restore();
  }

  private void stroke(Canvas canvas, float[] points, float width) {
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(width);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setStrokeJoin(Paint.Join.ROUND);
    Path path = new Path();
    path.moveTo(points[0], points[1]);
    for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
    canvas.drawPath(path, paint);
    paint.setStyle(Paint.Style.FILL);
  }
}
