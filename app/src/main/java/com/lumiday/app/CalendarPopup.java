package com.lumiday.app;

import android.app.Dialog;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.view.*;
import android.widget.*;

/** Calendar expands from its title, keeping the live agenda blurred underneath. */
final class CalendarPopup extends Dialog {
  final MainActivity host;
  final View anchor;
  final LinearLayout content;
  final GestureFrame surface;
  final CalendarOverview calendar;

  CalendarPopup(MainActivity host, View anchor) {
    super(host);
    this.host = host;
    this.anchor = anchor;
    surface = new GestureFrame();
    content = host.vertical();
    content.setPadding(host.dp(8), host.dp(8), host.dp(8), host.dp(14));
    content.setBackground(host.shape(host.BG, 28));
    surface.addView(content, new FrameLayout.LayoutParams(-1, -2));
    calendar = new CalendarOverview(host, content);
    setContentView(surface);
    getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    getWindow().setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    getWindow().setDimAmount(.12f);
    setOnDismissListener(d -> {
      host.root.setRenderEffect(null);
      host.calendarPopup = null;
    });
  }

  @Override public void show() {
    super.show();
    int[] position = new int[2];
    anchor.getLocationOnScreen(position);
    WindowManager.LayoutParams p = getWindow().getAttributes();
    p.width = host.getResources().getDisplayMetrics().widthPixels - host.dp(20);
    p.height = -2;
    p.y = position[1];
    getWindow().setAttributes(p);
    host.root.setRenderEffect(RenderEffect.createBlurEffect(host.dp(12), host.dp(12), Shader.TileMode.CLAMP));
    surface.setPivotX(host.dp(28));
    surface.setPivotY(0);
    surface.setScaleX(.12f);
    surface.setScaleY(.06f);
    surface.setAlpha(0);
    surface.animate().scaleX(1).scaleY(1).alpha(1).setDuration(340)
        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
  }

  final class GestureFrame extends FrameLayout {
    float downY;
    boolean dragging;
    GestureFrame() { super(host); }
    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
      if (e.getActionMasked() == MotionEvent.ACTION_DOWN) { downY = e.getY(); dragging = false; }
      if (e.getActionMasked() == MotionEvent.ACTION_MOVE && Math.abs(e.getY()-downY) > host.dp(12)) {
        dragging = true;
        return true;
      }
      return dragging;
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
      if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
        float dy = e.getY()-downY;
        if (Math.abs(dy) > host.dp(28)) {
          calendar.setMonth(dy > 0);
          downY = e.getY();
        }
        return true;
      }
      if (e.getActionMasked() == MotionEvent.ACTION_UP || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
        dragging = false;
        return true;
      }
      return true;
    }
  }
}
