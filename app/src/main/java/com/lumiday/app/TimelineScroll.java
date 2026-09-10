package com.lumiday.app;

import android.view.*;
import android.widget.ScrollView;

final class TimelineScroll extends ScrollView {
  final MainActivity host;
  final ScaleGestureDetector scale;
  boolean pinching;
  TimelineScroll(MainActivity host) {
    super(host);
    this.host = host;
    scale = new ScaleGestureDetector(host, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      @Override public boolean onScale(ScaleGestureDetector d) {
        zoomBy(d.getScaleFactor(), d.getFocusY());
        return true;
      }
    });
  }
  void zoomBy(float factor, float focusY) {
        TimelineView t = host.timeline;
        if (host.tab != 1 || t == null || host.overview) return;
        float focal = (getScrollY() + focusY - t.getTop() - host.dp(18)) / t.hourHeight;
        float available = Math.max(host.dp(120), getHeight() - host.dp(36));
        t.hourHeight = Math.max(available / 15f, Math.min(available / 2f, t.hourHeight * factor));
        host.timelineHourHeight = t.hourHeight;
        t.getLayoutParams().height = Math.round(t.hourHeight * 24 + host.dp(36));
        t.requestLayout(); t.invalidate();
        int target = Math.round(t.getTop() + host.dp(18) + focal * t.hourHeight - focusY);
        post(() -> scrollTo(0, Math.max(0, target)));
  }
  @Override public boolean dispatchTouchEvent(MotionEvent event) {
    scale.onTouchEvent(event);
    if (event.getPointerCount() > 1) {
      if (!pinching) {
        MotionEvent cancel = MotionEvent.obtain(event);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(cancel); cancel.recycle();
      }
      pinching = true;
      return true;
    }
    if (pinching) {
      if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) pinching = false;
      return true;
    }
    return super.dispatchTouchEvent(event);
  }
}
