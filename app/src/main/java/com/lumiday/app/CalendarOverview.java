package com.lumiday.app;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

final class CalendarOverview {
  final MainActivity host;
  final LinearLayout parent;
  boolean month = true;

  CalendarOverview(MainActivity host, LinearLayout parent) {
    this.host = host;
    this.parent = parent;
    render();
  }

  ArrayList<JSONObject> onDate(LocalDate date) {
    ArrayList<JSONObject> result = new ArrayList<>();
    for (int i = 0; i < host.store.tasks().length(); i++) {
      JSONObject t = host.store.tasks().optJSONObject(i);
      if (t.optString("date").equals(date.toString())) result.add(t);
    }
    result.sort(Comparator.comparing(t -> t.optString("time")));
    return result;
  }

  void render() {
    parent.removeAllViews();
    LinearLayout card = host.card(parent);
    LinearLayout controls = host.row();
    controls.addView(
        host.icon(
            "left",
            "上一周期",
            () -> {
              host.selected = month ? host.selected.minusMonths(1) : host.selected.minusWeeks(1);
              render();
            }));
    host.weighted(
        controls,
        host.button(
            host.selected.getYear() + " 年 " + host.selected.getMonthValue() + " 月",
            () -> {
              month = !month;
              render();
            }));
    controls.addView(
        host.icon(
            "right",
            "下一周期",
            () -> {
              host.selected = month ? host.selected.plusMonths(1) : host.selected.plusWeeks(1);
              render();
            }));
    card.addView(controls);
    LinearLayout modes = host.row();
    host.weighted(
        modes,
        host.button(
            "月",
            () -> {
              month = true;
              render();
            }));
    host.weighted(
        modes,
        host.button(
            "周",
            () -> {
              month = false;
              render();
            }));
    card.addView(modes);
    LinearLayout week = host.row();
    for (String day : new String[] {"日", "一", "二", "三", "四", "五", "六"}) {
      TextView t = host.text(day, 12, host.MUTED);
      t.setGravity(Gravity.CENTER);
      host.weighted(week, t);
    }
    card.addView(week);
    LocalDate first = month ? host.selected.withDayOfMonth(1) : host.selected;
    LocalDate start = first.minusDays(first.getDayOfWeek().getValue() % 7);
    for (int r = 0; r < (month ? 6 : 1); r++) {
      LinearLayout line = host.row();
      line.setGravity(Gravity.TOP);
      card.addView(line);
      for (int c = 0; c < 7; c++) {
        LocalDate date = start.plusDays(r * 7 + c);
        LinearLayout cell = host.vertical();
        cell.setPadding(host.dp(2), host.dp(5), host.dp(2), host.dp(5));
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(0, host.dp(month ? 102 : 188), 1);
        line.addView(cell, lp);
        cell.setBackground(host.shape(date.equals(host.selected) ? 0xffedf3e7 : Color.WHITE, 6));
        TextView number =
            host.text(
                "" + date.getDayOfMonth(),
                13,
                date.getMonth() == host.selected.getMonth() ? host.INK : host.MUTED);
        cell.addView(number);
        ArrayList<JSONObject> tasks = onDate(date);
        int limit = month ? 2 : 5;
        for (int i = 0; i < Math.min(limit, tasks.size()); i++) {
          JSONObject task = tasks.get(i);
          TextView chip =
              host.text(task.optString("title"), 10, host.priorityColor(task.optInt("priority")));
          chip.setSingleLine(true);
          chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
          chip.setBackground(host.shape(0xffedf1e9, 3));
          LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, host.dp(20));
          p.topMargin = host.dp(2);
          cell.addView(chip, p);
          chip.setOnClickListener(v -> host.taskDetails(task));
        }
        if (tasks.size() > limit) {
          TextView more = host.text("+" + (tasks.size() - limit), 11, host.MUTED);
          more.setOnClickListener(v -> openDay(date));
          cell.addView(more);
        }
        cell.setOnClickListener(v -> openDay(date));
      }
    }
  }

  void openDay(LocalDate day) {
    host.selected = day;
    host.overview = false;
    host.show();
  }
}
