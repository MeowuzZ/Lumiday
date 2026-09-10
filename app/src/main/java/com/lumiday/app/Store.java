package com.lumiday.app;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.json.*;

final class Store {
  final AtomicFile file;
  JSONObject data;

  Store(Context c) throws Exception {
    this(c.getFilesDir());
  }

  Store(File dir) throws Exception {
    file = new AtomicFile(new File(dir, "lumiday.json"));
    if (file.getBaseFile().exists() || new File(file.getBaseFile() + ".bak").exists())
      data = validate(new String(file.readFully(), StandardCharsets.UTF_8));
    else
      data =
          new JSONObject()
              .put("schemaVersion", 5)
              .put("tasks", new JSONArray())
              .put("habits", new JSONArray());
  }

  static JSONObject validate(String raw) throws Exception {
    JSONObject d = new JSONObject(raw);
    int v = d.optInt("schemaVersion", 1);
    if (v < 1 || v > 5) throw new IOException("此备份版本不受支持，请使用更新版 Lumiday");
    JSONArray ts = d.getJSONArray("tasks");
    if (!d.has("habits")) d.put("habits", new JSONArray());
    JSONArray hs = d.getJSONArray("habits");
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (int k = 0; k < 2; k++) {
      JSONArray a = k == 0 ? ts : hs;
      for (int i = 0; i < a.length(); i++) {
        JSONObject o = a.getJSONObject(i);
        if (o.getString("title").trim().isEmpty()) throw new IOException("名称不能为空");
        if (!o.has("id")) o.put("id", java.util.UUID.randomUUID().toString());
        if (!ids.add(o.getString("id"))) throw new IOException("记录编号重复");
        if (k == 0) {
          LocalDate.parse(o.getString("date"));
          String t = o.optString("time", "");
          if (v < 4 && !t.isEmpty() && o.optString("endTime").isEmpty()) {
            java.time.LocalTime.parse(t);
            o.put("legacyStartTime", t);
            o.put("time", "");
            o.put("endTime", "");
            t = "";
          }
          TaskDates.validate(o.optString("date"), o.optString("endDate", o.optString("date")), t, o.optString("endTime", ""));
          if (o.optBoolean("undated") && !t.isEmpty()) throw new IOException("时间日程必须有日期");
          if (o.optInt("priority", 0) < 0 || o.optInt("priority", 0) > 3)
            throw new IOException("优先级无效");
        } else {
          if (o.optInt("target", 1) < 1) throw new IOException("习惯目标无效");
          if (!o.has("logs")) o.put("logs", new JSONObject());
          JSONObject logs = o.getJSONObject("logs");
          java.util.Iterator<String> it = logs.keys();
          while (it.hasNext()) {
            String date = it.next();
            LocalDate.parse(date);
            if (logs.getInt(date) < 0) throw new IOException("打卡记录无效");
          }
        }
      }
    }
    return d.put("schemaVersion", 5);
  }

  static void validateTimes(String start, String end) throws IOException {
    try {
      if (start.isEmpty()) {
        if (!end.isEmpty()) throw new IOException("请先设置开始时间");
        return;
      }
      if (end.isEmpty()) throw new IOException("请选择结束时间");
      int from = minutes(start), to = minutes(end);
      if (from >= 1440 || to <= from) throw new IOException("结束时间须晚于开始时间（同一天）");
    } catch (java.time.format.DateTimeParseException e) {
      throw new IOException("时间格式无效，请使用 HH:mm");
    }
  }

  static int minutes(String time) {
    if (time.equals("24:00")) return 1440;
    return java.time.LocalTime.parse(time).getHour() * 60
        + java.time.LocalTime.parse(time).getMinute();
  }

  JSONArray tasks() {
    return data.optJSONArray("tasks");
  }

  JSONArray habits() {
    return data.optJSONArray("habits");
  }

  void save() throws Exception {
    byte[] bytes = data.toString(2).getBytes(StandardCharsets.UTF_8);
    FileOutputStream out = null;
    try {
      out = file.startWrite();
      out.write(bytes);
      file.finishWrite(out);
    } catch (Exception e) {
      file.failWrite(out);
      throw e;
    }
  }

  void restore(String raw) throws Exception {
    JSONObject incoming = validate(raw);
    File backup = new File(file.getBaseFile().getParentFile(), "before-restore.json");
    try (FileOutputStream out = new FileOutputStream(backup)) {
      out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));
    }
    JSONObject old = data;
    data = incoming;
    try {
      save();
    } catch (Exception e) {
      data = old;
      throw e;
    }
  }
}
