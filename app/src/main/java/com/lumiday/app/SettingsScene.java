package com.lumiday.app;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/**
 * Four intact words stay in reading order; the gesture changes presentation, never the sentence.
 */
final class SettingsScene extends FrameLayout {
  final MainActivity host;
  final Typography hero;
  final LinearLayout buttons;
  final ValueAnimator ambient;
  float progress, phase, downY, startProgress, direction = 1;
  boolean dragging;
  ValueAnimator settle;

  SettingsScene(MainActivity host) {
    super(host);
    this.host = host;
    setBackgroundColor(0xff101f1b);
    hero = new Typography();
    addView(hero, new LayoutParams(-1, -1));
    buttons = host.vertical();
    buttons.setPadding(host.dp(30), host.dp(24), host.dp(30), host.dp(24));
    addButton("导出备份", host::export);
    addButton("从备份恢复", host::importBackup);
    addButton(
        "导出恢复前快照",
        () -> {
          host.exportSnapshot = true;
          host.export();
        });
    addButton(
        "重置添加按钮位置",
        () -> {
          host.getSharedPreferences("layout", 0).edit().clear().apply();
          Toast.makeText(host, "已重置", Toast.LENGTH_SHORT).show();
        });
    IconView back = new IconView(host, "left", 0xffd9e5c3, "返回今天");
    back.setOnClickListener(
        v -> {
          host.tab = 0;
          host.show();
        });
    LinearLayout.LayoutParams backLayout = new LinearLayout.LayoutParams(host.dp(52), host.dp(52));
    backLayout.gravity = Gravity.CENTER_HORIZONTAL;
    backLayout.topMargin = host.dp(22);
    buttons.addView(back, backLayout);
    ScrollView scroll = new ScrollView(host);
    scroll.setFillViewport(false);
    scroll.setVerticalScrollBarEnabled(false);
    scroll.addView(buttons);
    LayoutParams p = new LayoutParams(-1, -2, Gravity.CENTER);
    addView(scroll, p);
    setProgress(0);
    ambient = ValueAnimator.ofFloat(0, 1);
    ambient.setDuration(7000);
    ambient.setRepeatCount(ValueAnimator.INFINITE);
    ambient.setInterpolator(new android.view.animation.LinearInterpolator());
    ambient.addUpdateListener(
        a -> {
          phase = (float) a.getAnimatedValue();
          hero.invalidate();
        });
  }

  void addButton(String title, Runnable action) {
    TextView button = host.button(title, action);
    button.setTextColor(0xffe0e9d1);
    button.setBackground(host.shape(0xff263b31, 22));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, host.dp(62));
    p.bottomMargin = host.dp(14);
    buttons.addView(button, p);
  }

  void setProgress(float value) {
    progress = Math.max(0, Math.min(1, value));
    float reveal = Math.max(0, Math.min(1, (progress - .3f) / .65f));
    buttons.setAlpha(reveal);
    buttons.setTranslationY(host.dp(48) * (1 - reveal));
    buttons.setVisibility(reveal == 0 ? INVISIBLE : VISIBLE);
    for (int i = 0; i < buttons.getChildCount(); i++)
      buttons.getChildAt(i).setEnabled(reveal > .85f && !dragging);
    hero.setImportantForAccessibility(
        progress > .8f ? IMPORTANT_FOR_ACCESSIBILITY_NO : IMPORTANT_FOR_ACCESSIBILITY_YES);
    hero.invalidate();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    ambient.start();
  }

  @Override
  protected void onDetachedFromWindow() {
    ambient.cancel();
    if (settle != null) settle.cancel();
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downY = e.getY();
      startProgress = progress;
      dragging = false;
      if (settle != null) settle.cancel();
    }
    if (e.getActionMasked() == MotionEvent.ACTION_MOVE
        && Math.abs(e.getY() - downY) > host.dp(10)) {
      // A downward finger gesture and conventional scroll-down both reveal from the landing state.
      if (startProgress == 0) direction = e.getY() > downY ? 1 : -1;
      dragging = true;
      return true;
    }
    return dragging;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      downY = e.getY();
      startProgress = progress;
      return true;
    }
    if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
      if (startProgress == 0) direction = e.getY() > downY ? 1 : -1;
      dragging = true;
      setProgress(startProgress + direction * (e.getY() - downY) / Math.max(1, getHeight() * .48f));
      return true;
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP
        || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      if (dragging) {
        dragging = false;
        settle = ValueAnimator.ofFloat(progress, progress > .45f ? 1 : 0);
        settle.setDuration(280);
        settle.addUpdateListener(a -> setProgress((float) a.getAnimatedValue()));
        settle.start();
      } else performClick();
      return true;
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  final class Typography extends View {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final String[] words = {"时间", "只", "属于", "你"};

    Typography() {
      super(host);
      setContentDescription("时间只属于你");
      paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float w = getWidth(), h = getHeight();
      float size = Math.min(w * .28f, h * .14f), line = size * 1.25f;
      float first = (h - line * 3) / 2;
      paint.setTextSize(size);
      paint.setTextAlign(Paint.Align.CENTER);
      float wave = (float) Math.sin(phase * Math.PI * 2);
      paint.setShader(
          new LinearGradient(
              0,
              h * (.15f + phase * .4f),
              w,
              h * (.85f + phase * .4f),
              new int[] {0xfff4eed6, 0xffadcaab, 0xffd8e5b2},
              null,
              Shader.TileMode.MIRROR));
      for (int i = 0; i < words.length; i++) {
        float fade = Math.max(0, 1 - progress * 1.5f);
        paint.setAlpha(Math.round(255 * fade));
        float breathe = (float) Math.sin(phase * Math.PI * 2 + i * .35f) * host.dp(3);
        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = first + i * line - (fm.ascent + fm.descent) / 2;
        canvas.drawText(
            words[i], w / 2, baseline + breathe - progress * host.dp(45 + i * 12), paint);
      }
    }
  }
}
