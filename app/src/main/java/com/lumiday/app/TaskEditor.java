package com.lumiday.app;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.Locale;
import org.json.JSONObject;

/** A single draft: no model mutation until validation and Save succeed. */
final class TaskEditor extends Dialog {
  final MainActivity host;
  final JSONObject existing;
  EditText name, note;
  LinearLayout content, modes, times, priorities;
  TextView dateButton, startButton, endButton, feedback;
  LocalDate date;
  String start = "", end = "";
  int mode, priority;
  final Bundle restored;

  TaskEditor(MainActivity host, JSONObject existing, Bundle restored) {
    super(host);
    this.host = host;
    this.existing = existing;
    this.restored = restored;
    JSONObject t = existing == null ? new JSONObject() : existing;
    date =
        LocalDate.parse(
            t.optString("date", (host.tab == 1 ? host.selected : LocalDate.now()).toString()));
    start = t.optString("time");
    end = t.optString("endTime");
    priority = t.optInt("priority");
    mode = start.isEmpty() ? 0 : 1;
    if (restored != null) {
      date = LocalDate.parse(restored.getString("date"));
      start = restored.getString("start");
      end = restored.getString("end");
      mode = restored.getInt("mode");
      priority = restored.getInt("priority");
    }
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    content = host.vertical();
    content.setPadding(host.dp(22), host.dp(14), host.dp(22), host.dp(24));
    content.setBackground(host.shape(Color.WHITE, 30));
    LinearLayout header = host.row();
    TextView heading = host.text(existing == null ? "新建任务" : "编辑任务", 21, host.INK);
    heading.setTypeface(null, android.graphics.Typeface.BOLD);
    host.weighted(header, heading);
    header.addView(host.icon("close", "关闭添加面板", this::dismiss));
    content.addView(header);
    name = field("准备做什么？", existing == null ? "" : existing.optString("title"), false);
    name.setTextSize(21);
    note = field("添加备注（可选）", existing == null ? "" : existing.optString("note"), true);
    if (restored != null) {
      name.setText(restored.getString("title"));
      note.setText(restored.getString("note"));
    }
    dateButton =
        host.button(
            "",
            () ->
                new DatePickerDialog(
                        host,
                        (v, y, m, d) -> {
                          date = LocalDate.of(y, m + 1, d);
                          updateDate();
                        },
                        date.getYear(),
                        date.getMonthValue() - 1,
                        date.getDayOfMonth())
                    .show());
    dateButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    dateButton.setBackground(host.shape(host.BG, 16));
    content.addView(dateButton);
    updateDate();
    host.space(content, 16);
    modes = host.row();
    modes.setPadding(host.dp(4), host.dp(4), host.dp(4), host.dp(4));
    modes.setBackground(host.shape(host.BG, 18));
    content.addView(modes);
    times = host.vertical();
    content.addView(times);
    host.space(content, 14);
    content.addView(host.text("优先级", 13, host.MUTED));
    host.space(content, 8);
    priorities = host.row();
    content.addView(priorities);
    renderPriorities();
    feedback = host.text("", 13, 0xffb76057);
    feedback.setMinHeight(host.dp(28));
    content.addView(feedback);
    TextView save = host.button("保存", this::submit);
    save.setTextColor(Color.WHITE);
    save.setBackground(host.shape(host.GREEN, 20));
    content.addView(save, new LinearLayout.LayoutParams(-1, host.dp(54)));
    if (existing != null) content.addView(host.button("删除任务", () -> host.removeTask(existing)));
    ScrollView scroll = new ScrollView(host);
    scroll.setFillViewport(false);
    scroll.addView(content);
    setContentView(scroll);
    Window window = getWindow();
    window.setBackgroundDrawableResource(android.R.color.transparent);
    window.setGravity(Gravity.BOTTOM);
    window.setWindowAnimations(0);
    window.setDimAmount(.35f);
    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    renderModes();
  }

  @Override
  public void show() {
    super.show();
    getWindow().setLayout(-1, -2);
    getWindow()
        .getDecorView()
        .setOnApplyWindowInsetsListener(
            (view, insets) -> {
              android.graphics.Insets safe =
                  insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
              content.setPadding(
                  host.dp(22) + safe.left,
                  host.dp(14),
                  host.dp(22) + safe.right,
                  host.dp(24) + safe.bottom);
              return insets;
            });
  }

  EditText field(String hint, String value, boolean multiline) {
    EditText field = new EditText(host);
    field.setHint(hint);
    field.setText(value);
    field.setTextColor(host.INK);
    field.setHintTextColor(host.MUTED);
    field.setBackgroundColor(Color.TRANSPARENT);
    field.setPadding(host.dp(2), host.dp(12), host.dp(2), host.dp(12));
    field.setSingleLine(!multiline);
    field.setMaxLines(multiline ? 3 : 1);
    field.setTextSize(15);
    field.setContentDescription(hint);
    content.addView(field, new LinearLayout.LayoutParams(-1, -2));
    return field;
  }

  void updateDate() {
    dateButton.setText("日期    " + date + (date.equals(LocalDate.now()) ? "  · 今天" : ""));
  }

  void chooseMode(int value) {
    mode = value;
    feedback.setText("");
    renderModes();
  }

  void renderModes() {
    modes.removeAllViews();
    String[] labels = {"全天待办", "时间日程"};
    for (int i = 0; i < 2; i++) {
      final int value = i;
      TextView b = host.button(labels[i], () -> chooseMode(value));
      b.setTextSize(13);
      b.setPadding(host.dp(4), host.dp(12), host.dp(4), host.dp(12));
      if (mode == i) b.setBackground(host.shape(Color.WHITE, 14));
      b.setTextColor(mode == i ? host.INK : host.MUTED);
      host.weighted(modes, b);
    }
    times.removeAllViews();
    if (mode == 0) {
      TextView caption = host.text("不设时间，默认全天", 12, host.MUTED);
      caption.setPadding(0, host.dp(10), 0, 0);
      times.addView(caption);
      return;
    }
    startButton = host.button("开始    " + (start.isEmpty() ? "选择时间" : start), () -> pick(false));
    startButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    times.addView(startButton);
    if (mode == 1) {
      endButton = host.button("结束    " + (end.isEmpty() ? "选择时间" : end), () -> pick(true));
      endButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      times.addView(endButton);
      times.addView(
          host.button(
              "结束于 24:00",
              () -> {
                end = "24:00";
                renderModes();
              }));
    }
  }

  void pick(boolean ending) {
    String current = ending ? end : start;
    LocalTime initial =
        current.isEmpty()
            ? (ending && !start.isEmpty()
                ? LocalTime.parse(start).plusHours(1)
                : LocalTime.of(9, 0))
            : (current.equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(current));
    new TimePickerDialog(
            host,
            (v, h, m) -> {
              String value = String.format(Locale.ROOT, "%02d:%02d", h, m);
              if (ending) end = value;
              else start = value;
              renderModes();
              feedback.setText("");
            },
            initial.getHour(),
            initial.getMinute(),
            true)
        .show();
  }

  void renderPriorities() {
    priorities.removeAllViews();
    String[] labels = {"无", "低", "中", "高"};
    for (int i = 0; i < 4; i++) {
      final int value = i;
      TextView b =
          host.button(
              labels[i],
              () -> {
                priority = value;
                renderPriorities();
              });
      b.setTextColor(host.priorityColor(i));
      if (priority == i) b.setBackground(host.shape(host.SOFT, 14));
      host.weighted(priorities, b);
    }
  }

  void submit() {
    String title = name.getText().toString().trim();
    if (title.isEmpty()) {
      name.setError("请输入任务名称");
      name.requestFocus();
      return;
    }
    if (mode > 0 && start.isEmpty()) {
      feedback.setText("请选择开始时间");
      return;
    }
    if (mode == 1 && end.isEmpty()) {
      feedback.setText("请选择结束时间");
      return;
    }
    String from = mode == 0 ? "" : start, to = mode == 1 ? end : "";
    try {
      Store.validateTimes(from, to);
    } catch (Exception e) {
      feedback.setText(e.getMessage());
      return;
    }
    if (host.saveTask(
        existing, title, note.getText().toString().trim(), date.toString(), from, to, priority))
      dismiss();
  }

  Bundle draft() {
    Bundle b = new Bundle();
    b.putString("id", existing == null ? "" : existing.optString("id"));
    b.putString("title", name.getText().toString());
    b.putString("note", note.getText().toString());
    b.putString("date", date.toString());
    b.putString("start", start);
    b.putString("end", end);
    b.putInt("mode", mode);
    b.putInt("priority", priority);
    return b;
  }
}
