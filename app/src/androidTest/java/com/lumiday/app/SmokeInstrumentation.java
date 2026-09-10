package com.lumiday.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.time.LocalDate;
import org.json.*;

/** All writes are isolated from user records, including editor interaction tests. */
public class SmokeInstrumentation extends Instrumentation {
  void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  void rejected(String start, String end) throws Exception {
    boolean invalid = false;
    try {
      Store.validateTimes(start, end);
    } catch (Exception e) {
      invalid = true;
    }
    check(invalid, "invalid time range rejected: " + start + "/" + end);
  }

  void screenshot(String name) throws Exception {
    android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot();
    try (java.io.FileOutputStream out =
        new java.io.FileOutputStream(new File(getTargetContext().getFilesDir(), name))) {
      bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    MainActivity activity = null;
    Store original = null;
    int exitCode = Activity.RESULT_CANCELED;
    SharedPreferences layout = getTargetContext().getSharedPreferences("layout", 0);
    float originalX = layout.getFloat("fabX", 1f), originalY = layout.getFloat("fabY", 1f);
    try {
      File dir = getTargetContext().getDir("instrumentation-v2", 0);
      new File(dir, "lumiday.json").delete();
      Store s = new Store(dir);
      JSONObject legacy =
          new JSONObject(
              "{\"schemaVersion\":2,\"tasks\":[{\"id\":\"old\",\"title\":\"旧任务\",\"date\":\"2026-09-10\",\"time\":\"09:00\"}],\"habits\":[{\"id\":\"old-habit\",\"title\":\"喝水\",\"target\":3,\"logs\":{\"2026-09-09\":2}}]}");
      s.restore(legacy.toString());
      check(s.data.getInt("schemaVersion") == 3, "v2 migration");
      check(
          s.habits().getJSONObject(0).getJSONObject("logs").getInt("2026-09-09") == 2,
          "legacy archive retained");
      legacy.put("schemaVersion", 1);
      check(Store.validate(legacy.toString()).getInt("schemaVersion") == 3, "v1 migration");
      rejected("", "10:00");
      rejected("10:00", "09:00");
      rejected("10:00", "10:00");
      rejected("25:00", "");
      Store.validateTimes("", "");
      Store.validateTimes("09:00", "");
      Store.validateTimes("09:00", "10:00");
      boolean rejected = false;
      try {
        s.restore("{\"schemaVersion\":99,\"tasks\":[]}");
      } catch (Exception e) {
        rejected = true;
      }
      check(rejected && s.tasks().length() == 1, "future schema leaves data intact");
      check(new File(dir, "before-restore.json").exists(), "restore snapshot exists");
      s.data.put("tasks", new JSONArray());
      s.save();
      activity =
          (MainActivity)
              startActivitySync(
                  new Intent(getTargetContext(), MainActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      MainActivity a = activity;
      original = a.store;
      runOnMainSync(
          () -> {
            a.store = s;
            a.selected = LocalDate.now();
            a.show();
          });
      runOnMainSync(
          () -> {
            check(a.navigation.getChildCount() == 4, "four icon-only tabs");
            for (int i = 0; i < 4; i++)
              check(
                  ((FrameLayout) a.navigation.getChildAt(i)).getChildAt(0) instanceof IconView,
                  "navigation uses vector icons");
            a.floating.performClick();
            check(a.editor.isShowing(), "plus opens editor directly");
            check(a.editor.mode == 0, "new task defaults to all-day");
            a.editor.name.setText("整理本周计划");
            a.editor.submit();
            check(
                s.tasks().length() == 1 && s.tasks().optJSONObject(0).optString("time").isEmpty(),
                "all-day saved");
            a.editTask(null);
            a.editor.name.setText("项目讨论");
            a.editor.chooseMode(2);
            a.editor.start = "14:00";
            a.editor.end = "13:00";
            a.editor.submit();
            check(s.tasks().length() == 1 && a.editor.isShowing(), "invalid range stays in editor");
            a.editor.end = "15:30";
            a.editor.submit();
            a.editTask(null);
            a.editor.name.setText("阅读与记录");
            a.editor.chooseMode(1);
            a.editor.start = "09:00";
            a.editor.submit();
            check(
                a.tasks().get(1).optString("time").equals("09:00"),
                "earlier start sorts first regardless of insertion");
            check(a.tasks().get(2).optString("endTime").equals("15:30"), "range end persisted");
            JSONObject range = a.tasks().get(2);
            a.editTask(range);
            a.editor.chooseMode(0);
            a.editor.submit();
            check(
                range.optString("time").isEmpty() && range.optString("endTime").isEmpty(),
                "all-day clears both times");
            a.editTask(range);
            a.editor.chooseMode(2);
            a.editor.start = "14:00";
            a.editor.end = "15:30";
            a.editor.submit();
            a.editTask(null);
            a.editor.name.setText("未保存草稿");
            a.editor.note.setText("草稿备注");
            a.editor.chooseMode(2);
            a.editor.start = "10:00";
            a.editor.end = "11:00";
            Bundle draft = a.editor.draft();
            a.editor.dismiss();
            a.editor = new TaskEditor(a, null, draft);
            a.editor.show();
            check(
                a.editor.name.getText().toString().equals("未保存草稿") && a.editor.end.equals("11:00"),
                "editor draft restores");
            a.editor.dismiss();
            check(s.tasks().length() == 3, "cancel does not save");
            for (int i = 0; i < 4; i++) {
              a.tab = i;
              a.show();
            }
            a.tab = 0;
            a.show();
            a.toggleCalendar();
          });
      Thread.sleep(450);
      runOnMainSync(
          () -> {
            check(a.expanded, "month expanded");
            a.toggleCalendar();
          });
      Thread.sleep(450);
      runOnMainSync(() -> check(!a.expanded, "week collapsed"));
      runOnMainSync(
          () -> {
            float x = a.floating.getX(), y = a.floating.getY();
            long now = SystemClock.uptimeMillis();
            for (int action :
                new int[] {
                  MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP
                }) {
              float point = action == MotionEvent.ACTION_DOWN ? 200 : 100;
              MotionEvent event = MotionEvent.obtain(now, now + 20, action, point, point, 0);
              a.floating.dispatchTouchEvent(event);
              event.recycle();
            }
            check(a.floating.getX() < x || a.floating.getY() < y, "floating button drags");
            check(a.editor == null || !a.editor.isShowing(), "drag does not open editor");
            a.show();
          });
      Thread.sleep(100);
      runOnMainSync(
          () ->
              check(
                  a.floating.getX() >= a.dp(12) && a.floating.getY() >= a.dp(12),
                  "floating position clamped after re-layout"));
      check(new Store(dir).tasks().length() == 3, "editor records survive reload");
      Store restored = new Store(getTargetContext().getDir("restore-v3", 0));
      restored.restore(s.data.toString());
      check(
          restored.tasks().getJSONObject(1).getString("endTime").equals("15:30"),
          "v3 backup range round-trip");
      runOnMainSync(
          () -> {
            a.getSharedPreferences("layout", 0).edit().clear().apply();
            a.tab = 0;
            a.show();
          });
      Thread.sleep(200);
      screenshot("home-v2.png");
      runOnMainSync(
          () -> {
            a.tab = 1;
            a.show();
          });
      Thread.sleep(200);
      screenshot("agenda-v2.png");
      runOnMainSync(
          () -> {
            a.editTask(s.tasks().optJSONObject(1));
          });
      Thread.sleep(500);
      screenshot("editor-v2.png");
      runOnMainSync(() -> a.editor.dismiss());
      result.putString(
          "stream",
          "PASS: v1/v2/v3 backups, legacy archive, all-day/start/range editor, invalid ranges,"
              + " cancellation, chronological order, four icon tabs, calendar animation, draggable"
              + " overlay, persistence\n");
      exitCode = Activity.RESULT_OK;
    } catch (Throwable e) {
      result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(e));

    } finally {
      layout.edit().putFloat("fabX", originalX).putFloat("fabY", originalY).commit();
      if (activity != null && original != null) {
        MainActivity a = activity;
        Store previous = original;
        runOnMainSync(
            () -> {
              if (a.editor != null) a.editor.dismiss();
              a.store = previous;
              a.show();
            });
      }
    }
    finish(exitCode, result);
  }
}
