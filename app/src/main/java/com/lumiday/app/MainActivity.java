package com.lumiday.app;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
  final int BG = 0xfff5f6f2, GREEN = 0xff76956d, INK = 0xff29352c;
  final int MUTED = 0xff8a9389, SOFT = 0xffe9eee4;
  Store store;
  LinearLayout root, body, calendar, navigation;
  FrameLayout stage;
  IconView floating;
  TaskEditor editor;
  LocalDate selected = LocalDate.now();
  boolean expanded, animating, completedExpanded = true;
  int tab;
  float touchY;

  int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setDecorFitsSystemWindows(false);
    getWindow()
        .getDecorView()
        .getWindowInsetsController()
        .setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    try {
      store = new Store(this);
    } catch (Exception e) {
      new AlertDialog.Builder(this)
          .setTitle("本地数据无法读取")
          .setMessage("原文件已保留，可先导出原始数据。\n" + e.getMessage())
          .setPositiveButton("导出原始数据", (d, w) -> export())
          .setNegativeButton("关闭", (d, w) -> finish())
          .setCancelable(false)
          .show();
      return;
    }
    if (state != null) {
      selected = LocalDate.parse(state.getString("date", selected.toString()));
      tab = Math.min(3, state.getInt("tab"));
      expanded = state.getBoolean("expanded");
    }
    getOnBackInvokedDispatcher()
        .registerOnBackInvokedCallback(
            0,
            () -> {
              if (tab != 0) {
                tab = 0;
                show();
              } else finish();
            });
    show();
    if (state != null && state.getBundle("editor") != null) {
      Bundle draft = state.getBundle("editor");
      JSONObject existing = findTask(draft.getString("id", ""));
      editor = new TaskEditor(this, existing, draft);
      editor.show();
    }
  }

  JSONObject findTask(String id) {
    for (int i = 0; i < store.tasks().length(); i++) {
      JSONObject t = store.tasks().optJSONObject(i);
      if (id.equals(t.optString("id"))) return t;
    }
    return null;
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("date", selected.toString());
    b.putInt("tab", tab);
    b.putBoolean("expanded", expanded);
    if (editor != null && editor.isShowing()) b.putBundle("editor", editor.draft());
  }

  GradientDrawable shape(int color, int radius) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(dp(radius));
    return g;
  }

  TextView text(String value, int size, int color) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setGravity(Gravity.CENTER_VERTICAL);
    t.setFontFeatureSettings("tnum");
    return t;
  }

  TextView button(String value, Runnable action) {
    TextView t = text(value, 15, GREEN);
    t.setGravity(Gravity.CENTER);
    t.setPadding(dp(14), dp(12), dp(14), dp(12));
    t.setMinHeight(dp(48));
    t.setOnClickListener(v -> action.run());
    return t;
  }

  IconView icon(String name, String label, Runnable action) {
    IconView view = new IconView(this, name, GREEN, label);
    view.setOnClickListener(v -> action.run());
    view.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
    return view;
  }

  LinearLayout vertical() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  void weighted(LinearLayout r, View v) {
    r.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
  }

  void space(LinearLayout l, int height) {
    l.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
  }

  LinearLayout card(LinearLayout parent) {
    LinearLayout c = vertical();
    c.setPadding(dp(18), dp(10), dp(18), dp(10));
    c.setBackground(shape(Color.WHITE, 24));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(20), dp(6), dp(20), dp(6));
    parent.addView(c, p);
    return c;
  }

  void error(Exception e) {
    new AlertDialog.Builder(this)
        .setTitle("操作未完成")
        .setMessage(e.getMessage())
        .setPositiveButton("知道了", null)
        .show();
  }

  void put(JSONObject o, String k, Object value) {
    try {
      o.put(k, value);
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  boolean persist(JSONObject previous) {
    try {
      store.save();
      return true;
    } catch (Exception e) {
      store.data = previous;
      error(e);
      return false;
    }
  }

  JSONObject snapshot() {
    try {
      return new JSONObject(store.data.toString());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  SettingsScene settingsScene;
  boolean overview;
  TimelineView timeline;
  android.app.Dialog detailsDialog;
  CalendarPopup calendarPopup;

  void show() {
    animating = false;
    if (tab == 3) {
      settingsScene = new SettingsScene(this);
      setContentView(settingsScene);
      getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
      getWindow()
          .getInsetsController()
          .setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
      return;
    }
    settingsScene = null;
    getWindow().getInsetsController().show(WindowInsets.Type.systemBars());
    root = vertical();
    root.setBackgroundColor(BG);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          Insets bars =
              insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
          root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          return insets;
        });
    root.requestApplyInsets();
    LinearLayout header = row();
    header.setPadding(dp(24), dp(18), dp(16), dp(8));
    LinearLayout titles = vertical();
    TextView brand = text("LUMIDAY", 11, GREEN);
    brand.setLetterSpacing(.18f);
    titles.addView(brand);
    space(titles, 6);
    TextView title = text(new String[] {"今天", "日程", "四象限", "设置"}[tab], 29, INK);
    title.setTypeface(null, Typeface.BOLD);
    if (tab == 1)
      title.setOnClickListener(
          v -> {
            calendarPopup = new CalendarPopup(this, title);
            calendarPopup.show();
          });
    titles.addView(title);
    weighted(header, titles);
    if (tab == 1) {
      TextView today =
          button(
              "回到今天",
              () -> {
                selected = LocalDate.now();
                show();
              });
      today.setBackground(shape(SOFT, 20));
      header.addView(today);
    }
    root.addView(header);
    calendar = null;
    if (tab == 1 && !overview) calendar();
    stage = new FrameLayout(this);
    ScrollView scroll = new ScrollView(this);
    scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    body = vertical();
    body.setPadding(0, dp(6), 0, dp(96));
    scroll.addView(body);
    stage.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
    root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));
    if (tab == 0) today();
    else if (tab == 1) agenda();
    else if (tab == 2) quadrants();

    navigation = row();
    navigation.setPadding(dp(24), dp(10), dp(24), dp(10));
    String[] names = {"tasks", "calendar", "grid", "settings"};
    String[] labels = {"今日任务", "日程", "优先级", "设置"};
    for (int i = 0; i < 4; i++) {
      final int index = i;
      FrameLayout slot = new FrameLayout(this);
      IconView item =
          icon(
              names[i],
              labels[i],
              () -> {
                tab = index;
                show();
              });
      item.tint = tab == i ? GREEN : 0xffa4ada1;
      item.setSelected(tab == i);
      if (tab == i) item.setBackground(shape(SOFT, 20));
      FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(58), dp(48), Gravity.CENTER);
      slot.addView(item, p);
      navigation.addView(slot, new LinearLayout.LayoutParams(0, dp(50), 1));
    }
    root.addView(navigation);
    if (tab < 3) installFloatingButton();
  }

  void installFloatingButton() {
    floating = new IconView(this, "plus", Color.WHITE, "添加待办或日程，可拖动位置");
    floating.setBackground(shape(GREEN, 40));
    floating.setElevation(dp(7));
    floating.setOnClickListener(v -> editTask(null));
    stage.addView(floating, new FrameLayout.LayoutParams(dp(62), dp(62)));
    SharedPreferences prefs = getSharedPreferences("layout", MODE_PRIVATE);
    stage.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          floating.setX(dp(12) + prefs.getFloat("fabX", 1f) * availableX());
          floating.setY(dp(12) + prefs.getFloat("fabY", 1f) * availableY());
        });
    floating.setOnTouchListener(
        new View.OnTouchListener() {
          float downX, downY, originX, originY;
          boolean dragging;

          @Override
          public boolean onTouch(View v, MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
              downX = e.getRawX();
              downY = e.getRawY();
              originX = v.getX();
              originY = v.getY();
              dragging = false;
              v.getParent().requestDisallowInterceptTouchEvent(true);
              return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
              float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
              dragging |=
                  Math.hypot(dx, dy)
                      > ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
              if (dragging) {
                v.setX(clamp(originX + dx, dp(12), dp(12) + availableX()));
                v.setY(clamp(originY + dy, dp(12), dp(12) + availableY()));
              }
              return true;
            }
            if (e.getActionMasked() == MotionEvent.ACTION_UP
                || e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
              if (dragging)
                prefs
                    .edit()
                    .putFloat("fabX", (v.getX() - dp(12)) / Math.max(1, availableX()))
                    .putFloat("fabY", (v.getY() - dp(12)) / Math.max(1, availableY()))
                    .apply();
              else if (e.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
              v.getParent().requestDisallowInterceptTouchEvent(false);
              return true;
            }
            return true;
          }
        });
  }

  float availableX() {
    return Math.max(0, stage.getWidth() - dp(86));
  }

  float availableY() {
    return Math.max(0, stage.getHeight() - dp(86));
  }

  float clamp(float v, float min, float max) {
    return Math.max(min, Math.min(max, v));
  }

  ArrayList<JSONObject> tasks() {
    ArrayList<JSONObject> list = new ArrayList<>();
    for (int i = 0; i < store.tasks().length(); i++) {
      JSONObject t = store.tasks().optJSONObject(i);
      if ((tab == 0 ? LocalDate.now() : selected).toString().equals(t.optString("date")))
        list.add(t);
    }
    list.sort(
        Comparator.comparingInt(
                (JSONObject t) ->
                    t.optString("time").isEmpty()
                        ? -1
                        : LocalTime.parse(t.optString("time")).toSecondOfDay())
            .thenComparing(t -> t.optString("id")));
    return list;
  }

  String timeLabel(JSONObject t) {
    String start = t.optString("time"), end = t.optString("endTime");
    return start.isEmpty() ? "全天" : start + (end.isEmpty() ? "" : " – " + end);
  }

  void section(String title, String detail) {
    LinearLayout r = row();
    r.setPadding(dp(26), dp(14), dp(26), dp(8));
    TextView label = text(title, 14, INK);
    label.setTypeface(null, Typeface.BOLD);
    weighted(r, label);
    r.addView(text(detail, 12, MUTED));
    body.addView(r);
  }

  void empty(LinearLayout parent, String message) {
    TextView t = text(message, 15, MUTED);
    t.setPadding(dp(4), dp(24), dp(4), dp(24));
    parent.addView(t);
  }

  void today() {
    ArrayList<JSONObject> list = tasks();
    int pending = 0;
    for (JSONObject t : list) if (!t.optBoolean("done")) pending++;
    section("待完成", pending + " 项");
    LinearLayout c = card(body);
    for (JSONObject t : list) if (!t.optBoolean("done")) task(c, t, false);
    if (pending == 0) empty(c, "暂时没有待办，留一点时间给自己。");
    int completed = list.size() - pending;
    if (completed > 0) {
      TextView toggle =
          button(
              "已完成  " + completed + (completedExpanded ? "  ﹀" : "  ›"),
              () -> {
                completedExpanded = !completedExpanded;
                show();
              });
      body.addView(toggle);
      if (completedExpanded) {
        LinearLayout done = card(body);
        for (JSONObject t : list) if (t.optBoolean("done")) task(done, t, false);
      }
    }
  }

  int priorityColor(int p) {
    return new int[] {0xff68a67b, 0xff638bd5, 0xffe2a050, 0xffd86c6c}[Math.max(0, Math.min(p, 3))];
  }

  void task(LinearLayout parent, JSONObject t, boolean timeline) {
    LinearLayout r = row();
    r.setPadding(0, dp(8), 0, dp(8));
    IconView check =
        new IconView(
            this,
            t.optBoolean("done") ? "done" : "unchecked",
            t.optBoolean("done") ? 0xffb8c4b0 : priorityColor(t.optInt("priority")),
            t.optBoolean("done") ? "标为未完成" : "完成任务");
    check.setOnClickListener(
        v -> {
          JSONObject previous = snapshot();
          put(t, "done", !t.optBoolean("done"));
          persist(previous);
          show();
        });
    r.addView(check, new LinearLayout.LayoutParams(dp(44), dp(52)));
    LinearLayout label = vertical();
    label.setPadding(dp(6), dp(8), dp(4), dp(8));
    TextView title = text(t.optString("title"), 17, t.optBoolean("done") ? MUTED : INK);
    if (t.optBoolean("done"))
      title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
    label.addView(title);
    String subtitle =
        timeline
            ? t.optString("note")
            : timeLabel(t) + (t.optString("note").isEmpty() ? "" : " · " + t.optString("note"));
    if (tab == 2) subtitle = t.optString("date") + " · " + timeLabel(t);
    if (!subtitle.isEmpty()) {
      TextView note = text(subtitle, 12, MUTED);
      note.setMaxLines(2);
      label.addView(note);
    }
    weighted(r, label);
    label.setOnClickListener(v -> editTask(t));
    parent.addView(r);
  }

  void agenda() {
    if (overview) {
      new CalendarOverview(this, body);
      return;
    }
    section("全天待办", "");
    LinearLayout allDay = card(body);
    int count = 0;
    for (JSONObject task : tasks())
      if (task.optString("time").isEmpty()) {
        task(allDay, task, true);
        count++;
      }
    if (count == 0) empty(allDay, "没有全天待办");
    timeline = new TimelineView(this, tasks());
    body.addView(timeline, new LinearLayout.LayoutParams(-1, dp(24 * 76 + 36)));
  }

  void taskDetails(JSONObject task) {
    LinearLayout panel = vertical();
    panel.setPadding(dp(22), dp(16), dp(22), dp(16));
    panel.addView(text(task.optString("date") + "  " + timeLabel(task), 15, GREEN));
    if (!task.optString("note").isEmpty()) panel.addView(text(task.optString("note"), 16, INK));
    if (!task.optString("legacyStartTime").isEmpty())
      panel.addView(text("原开始时间：" + task.optString("legacyStartTime"), 13, MUTED));
    panel.addView(text(task.optBoolean("done") ? "已完成" : "未完成", 14, MUTED));
    detailsDialog = new android.app.Dialog(this);
    panel.setBackground(shape(BG, 28));
    TextView heading = text(task.optString("title"), 24, INK);
    heading.setTypeface(null, Typeface.BOLD);
    panel.addView(heading, 0);
    space(panel, 20);
    TextView edit = button("编辑", () -> {
      detailsDialog.dismiss();
      if (calendarPopup != null) calendarPopup.dismiss();
      editTask(task);
    });
    edit.setBackground(shape(GREEN, 20));
    edit.setTextColor(Color.WHITE);
    panel.addView(edit, new LinearLayout.LayoutParams(-1, dp(54)));
    panel.addView(button("删除", () -> { detailsDialog.dismiss();
      if (calendarPopup != null) calendarPopup.dismiss();
      removeTask(task); }));
    panel.addView(button("关闭", () -> detailsDialog.dismiss()));
    detailsDialog.setContentView(panel);
    Window window = detailsDialog.getWindow();
    window.setBackgroundDrawableResource(android.R.color.transparent);
    window.setGravity(Gravity.BOTTOM);
    window.setDimAmount(.35f);
    detailsDialog.show();
    window.setLayout(-1, -2);
  }

  void completeTask(JSONObject task) {
    int scrollY = stage.getChildAt(0).getScrollY();
    JSONObject previous = snapshot();
    put(task, "done", true);
    if (persist(previous)) {
      Toast.makeText(this, "任务已完成", Toast.LENGTH_SHORT).show();
      show();
      stage.post(() -> stage.getChildAt(0).scrollTo(0, scrollY));
    }
  }

  void quadrants() {
    body.setPadding(dp(12), dp(10), dp(12), dp(88));
    String[] labels = {"重要且紧急", "重要不紧急", "不重要但紧急", "不重要不紧急"};
    String[] numbers = {"Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ"};
    int height = Math.max(dp(235), (getResources().getDisplayMetrics().heightPixels - dp(270)) / 2);
    for (int r = 0; r < 2; r++) {
      LinearLayout pair = row();
      pair.setGravity(Gravity.TOP);
      body.addView(pair, new LinearLayout.LayoutParams(-1, height));
      for (int col = 0; col < 2; col++) {
        int index = r * 2 + col;
        final int priority = 3 - index;
        LinearLayout box = vertical();
        box.setPadding(dp(12), dp(14), dp(10), dp(8));
        box.setBackground(shape(Color.WHITE, 24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        pair.addView(box, lp);
        box.setContentDescription(labels[index] + "，点击添加");
        box.setOnClickListener(v -> addPriority(priority));
        TextView title = text(numbers[index] + "  " + labels[index], 13, priorityColor(priority));
        title.setTypeface(null, Typeface.BOLD);
        title.setMinHeight(dp(44));
        title.setOnClickListener(v -> addPriority(priority));
        box.addView(title);
        ScrollView listScroll = new ScrollView(this);
        listScroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = vertical();
        list.setOnClickListener(v -> addPriority(priority));
        listScroll.setFillViewport(true);
        listScroll.addView(list);
        box.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        int count = 0;
        for (int i = 0; i < store.tasks().length(); i++) {
          JSONObject task = store.tasks().optJSONObject(i);
          if (task.optInt("priority") == priority) {
            task(list, task, false);
            count++;
          }
        }
        if (count == 0) {
          TextView empty = button("＋", () -> addPriority(priority));
          empty.setTextColor(priorityColor(priority));
          empty.setTextSize(28);
          list.addView(empty, new LinearLayout.LayoutParams(-1, -1));
        }
        TextView add = button("＋", () -> addPriority(priority));
        add.setTextColor(priorityColor(priority));
        add.setContentDescription("添加" + labels[index] + "任务");
        if (count > 0) box.addView(add);
      }
    }
  }

  void addPriority(int priority) {
    editor = new TaskEditor(this, null, null);
    editor.priority = priority;
    editor.show();
  }

  void editTask(JSONObject task) {
    editor = new TaskEditor(this, task, null);
    editor.show();
  }

  boolean saveTask(
      JSONObject existing,
      String title,
      String note,
      String date,
      String start,
      String end,
      int priority) {
    try {
      JSONObject previous = snapshot();
      JSONObject t = existing == null ? new JSONObject() : findTask(existing.optString("id"));
      Store.validateTimes(start, end);
      put(t, "title", title);
      put(t, "note", note);
      put(t, "date", date);
      put(t, "time", start);
      put(t, "endTime", end);
      put(t, "priority", priority);
      if (existing == null) {
        put(t, "id", UUID.randomUUID().toString());
        store.tasks().put(t);
      }
      if (!persist(previous)) return false;
      selected = LocalDate.parse(date);
      show();
      return true;
    } catch (Exception e) {
      error(e);
      return false;
    }
  }

  void removeTask(JSONObject task) {
    new AlertDialog.Builder(this)
        .setTitle("删除任务？")
        .setNegativeButton("取消", null)
        .setPositiveButton(
            "删除",
            (d, w) -> {
              JSONObject previous = snapshot();
              for (int i = 0; i < store.tasks().length(); i++)
                if (store.tasks().optJSONObject(i) == task) {
                  store.tasks().remove(i);
                  break;
                }
              if (persist(previous)) {
                if (editor != null) editor.dismiss();
                show();
              }
            })
        .show();
  }

  void calendar() {
    LinearLayout wrapper = new CalendarPanel();
    wrapper.setOrientation(LinearLayout.VERTICAL);
    wrapper.setPadding(dp(14), 0, dp(14), 0);
    LinearLayout heading = row();
    heading.addView(
        icon(
            "left",
            "上一周或上个月",
            () -> {
              selected = expanded ? selected.minusMonths(1) : selected.minusWeeks(1);
              show();
            }));
    TextView month =
        button(
            selected.getYear() + " 年 " + selected.getMonthValue() + " 月", () -> toggleCalendar());
    weighted(heading, month);
    heading.addView(
        icon(
            "right",
            "下一周或下个月",
            () -> {
              selected = expanded ? selected.plusMonths(1) : selected.plusWeeks(1);
              show();
            }));
    wrapper.addView(heading);
    LinearLayout weekdays = row();
    for (String day : new String[] {"日", "一", "二", "三", "四", "五", "六"}) {
      TextView t = text(day, 13, MUTED);
      t.setGravity(Gravity.CENTER);
      weighted(weekdays, t);
    }
    wrapper.addView(weekdays);
    calendar = vertical();
    wrapper.addView(calendar, new LinearLayout.LayoutParams(-1, dp(expanded ? 264 : 44)));
    fillCalendar(expanded);
    TextView handle = button("━━", () -> toggleCalendar());
    handle.setTextSize(12);
    handle.setPadding(0, dp(3), 0, dp(5));
    wrapper.addView(handle);
    View.OnTouchListener drag =
        (v, e) -> {
          if (e.getAction() == MotionEvent.ACTION_DOWN) {
            touchY = e.getY();
            return true;
          }
          if (e.getAction() == MotionEvent.ACTION_UP) {
            float dy = e.getY() - touchY;
            if (Math.abs(dy) > dp(15)) {
              if ((dy > 0) != expanded) toggleCalendar();
            } else toggleCalendar();
            return true;
          }
          return true;
        };
    handle.setOnTouchListener(drag);
    month.setOnTouchListener(drag);
    root.addView(wrapper);
  }

  class CalendarPanel extends LinearLayout {
    float startX, startY;
    boolean dragging;

    CalendarPanel() {
      super(MainActivity.this);
    }

    @Override
    public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        startY = e.getY();
        startX = e.getX();
        dragging = false;
      }
      if (e.getAction() == MotionEvent.ACTION_MOVE
          && Math.abs(e.getY() - startY) > dp(14)
          && Math.abs(e.getY() - startY) > Math.abs(e.getX() - startX)) {
        dragging = true;
        return true;
      }
      return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_UP && dragging) {
        if ((e.getY() > startY) != expanded) toggleCalendar();
        dragging = false;
      }
      return true;
    }
  }

  void fillCalendar(boolean full) {
    calendar.removeAllViews();
    LocalDate first = selected.withDayOfMonth(1);
    LocalDate start =
        full
            ? first.minusDays(first.getDayOfWeek().getValue() % 7)
            : selected.minusDays(selected.getDayOfWeek().getValue() % 7);
    for (int r = 0; r < (full ? 6 : 1); r++) {
      LinearLayout line = row();
      for (int c = 0; c < 7; c++) {
        LocalDate d = start.plusDays(r * 7 + c);
        TextView b =
            button(
                "" + d.getDayOfMonth(),
                () -> {
                  selected = d;
                  show();
                });
        b.setPadding(0, 0, 0, 0);
        b.setTextColor(
            d.equals(selected) ? Color.WHITE : d.getMonth() == selected.getMonth() ? INK : MUTED);
        FrameLayout cell = new FrameLayout(this);
        if (d.equals(selected)) b.setBackground(shape(GREEN, 30));
        cell.addView(b, new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER));
        line.addView(cell, new LinearLayout.LayoutParams(0, dp(44), 1));
      }
      calendar.addView(line);
    }
  }

  void toggleCalendar() {
    if (animating) return;
    animating = true;
    boolean next = !expanded;
    LinearLayout target = calendar;
    int from = target.getHeight(), to = dp(next ? 264 : 44);
    LocalDate first = selected.withDayOfMonth(1);
    int week = (selected.getDayOfMonth() - 1 + first.getDayOfWeek().getValue() % 7) / 7;
    float offset = -week * dp(44);
    fillCalendar(true);
    ValueAnimator animation = ValueAnimator.ofFloat(0, 1);
    animation.setDuration(320);
    animation.setInterpolator(new DecelerateInterpolator());
    animation.addUpdateListener(
        value -> {
          float f = (float) value.getAnimatedValue();
          target.getLayoutParams().height = Math.round(from + (to - from) * f);
          float translation = next ? offset * (1 - f) : offset * f;
          for (int i = 0; i < target.getChildCount(); i++)
            target.getChildAt(i).setTranslationY(translation);
          target.requestLayout();
        });
    animation.addListener(
        new android.animation.AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(android.animation.Animator a) {
            if (calendar == target) {
              expanded = next;
              fillCalendar(next);
              animating = false;
            }
          }
        });
    animation.start();
  }

  boolean exportSnapshot = false;

  void export() {
    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    i.setType("application/json");
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.putExtra(Intent.EXTRA_TITLE, "Lumiday-" + LocalDate.now() + ".json");
    startActivityForResult(i, 10);
  }

  void importBackup() {
    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.setType("*/*");
    i.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, 11);
  }

  @Override
  protected void onActivityResult(int req, int result, Intent data) {
    super.onActivityResult(req, result, data);
    if (result != RESULT_OK || data == null) {
      exportSnapshot = false;
      return;
    }
    try {
      if (req == 10) {
        byte[] bytes;
        if (exportSnapshot) {
          bytes =
              java.nio.file.Files.readAllBytes(
                  new File(getFilesDir(), "before-restore.json").toPath());
        } else if (store != null) bytes = store.data.toString(2).getBytes(StandardCharsets.UTF_8);
        else
          bytes =
              java.nio.file.Files.readAllBytes(new File(getFilesDir(), "lumiday.json").toPath());
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          out.write(bytes);
        }
        Toast.makeText(this, "备份已导出", Toast.LENGTH_LONG).show();
      } else if (req == 11) {
        String raw;
        try (InputStream in = getContentResolver().openInputStream(data.getData());
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
          byte[] buf = new byte[8192];
          int n;
          while ((n = in.read(buf)) != -1) {
            if (out.size() + n > 10 * 1024 * 1024) throw new IOException("备份超过 10 MB");
            out.write(buf, 0, n);
          }
          raw = out.toString("UTF-8");
        }
        JSONObject verified = Store.validate(raw);
        new AlertDialog.Builder(this)
            .setTitle("恢复备份？")
            .setMessage(
                "包含 "
                    + verified.getJSONArray("tasks").length()
                    + " 条任务，将替换当前数据。恢复前会保存本地快照。旧版附加数据会保留在备份中。")
            .setNegativeButton("取消", null)
            .setPositiveButton(
                "恢复",
                (d, w) -> {
                  try {
                    store.restore(raw);
                    show();
                    Toast.makeText(this, "备份已恢复", Toast.LENGTH_LONG).show();
                  } catch (Exception e) {
                    error(e);
                  }
                })
            .show();
      }
    } catch (Exception e) {
      error(e);
    } finally {
      exportSnapshot = false;
    }
  }
}
