package com.lumiday.app;

import java.time.*;
import org.json.JSONObject;

final class TaskDates {
  static LocalDate start(JSONObject t) { return LocalDate.parse(t.optString("date")); }
  static LocalDate end(JSONObject t) { return LocalDate.parse(t.optString("endDate", t.optString("date"))); }
  static boolean special(JSONObject t) { return t.optBoolean("undated") || end(t).isAfter(start(t)); }
  static boolean occurs(JSONObject t, LocalDate day) {
    if (t.optBoolean("undated")) {
      LocalDate visible = t.optBoolean("done")
          ? LocalDate.parse(t.optString("completedDate", t.optString("date")))
          : (start(t).isAfter(LocalDate.now()) ? start(t) : LocalDate.now());
      return day.equals(visible);
    }
    if (day.isBefore(start(t)) || day.isAfter(end(t))) return false;
    return t.optString("time").isEmpty() || !day.equals(end(t))
        || !t.optString("endTime").equals("00:00") || start(t).equals(end(t));
  }
  static int rank(JSONObject t) { return t.optBoolean("done") ? 2 : special(t) ? 0 : 1; }
  static int from(JSONObject t, LocalDate day) { return day.isAfter(start(t)) ? 0 : Store.minutes(t.optString("time")); }
  static int to(JSONObject t, LocalDate day) { return day.isBefore(end(t)) ? 1440 : Store.minutes(t.optString("endTime")); }
  static void validate(String date, String endDate, String start, String end) throws java.io.IOException {
    LocalDate first = LocalDate.parse(date), last = LocalDate.parse(endDate);
    if (last.isBefore(first)) throw new java.io.IOException("结束日期不能早于开始日期");
    if (start.isEmpty() || first.equals(last)) { Store.validateTimes(start, end); return; }
    if (end.isEmpty() || Store.minutes(start) >= 1440) throw new java.io.IOException("请设置有效起止时间");
    Store.minutes(end);
  }
}
