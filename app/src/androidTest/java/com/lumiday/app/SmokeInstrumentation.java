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
      check(s.data.getInt("schemaVersion") == 5, "v2 migration");
      check(
          s.habits().getJSONObject(0).getJSONObject("logs").getInt("2026-09-09") == 2,
          "legacy archive retained");
      legacy.put("schemaVersion", 1);
      check(Store.validate(legacy.toString()).getInt("schemaVersion") == 5, "v1 migration");
      rejected("", "10:00");
      rejected("10:00", "09:00");
      rejected("10:00", "10:00");
      rejected("25:00", "");
      Store.validateTimes("", "");
      rejected("09:00", "");
      Store.validateTimes("09:00", "10:00");
      JSONObject overnight = new JSONObject().put("id","overnight").put("title","跨夜日程")
          .put("date","2026-09-10").put("endDate","2026-09-11").put("time","23:00").put("endTime","01:00");
      TaskDates.validate("2026-09-10","2026-09-11","23:00","01:00");
      check(TaskDates.occurs(overnight,LocalDate.parse("2026-09-11")),"overnight visible next day");
      check(TaskDates.from(overnight,LocalDate.parse("2026-09-11"))==0 && TaskDates.to(overnight,LocalDate.parse("2026-09-10"))==1440,"overnight clipped at midnight");
      overnight.put("endTime","00:00");
      check(!TaskDates.occurs(overnight,LocalDate.parse("2026-09-11")),"midnight endpoint excluded");
      JSONObject rolling = new JSONObject().put("date",LocalDate.now().minusDays(3).toString()).put("undated",true);
      check(TaskDates.occurs(rolling,LocalDate.now()),"undated unfinished rolls forward");
      rolling.put("done",true).put("completedDate",LocalDate.now().minusDays(1).toString());
      check(!TaskDates.occurs(rolling,LocalDate.now()),"completed undated stops rolling");
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
            a.selected = LocalDate.now().plusDays(1);
            a.tab = 1;
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
            a.editor.undated = false;
            a.editor.submit();
            check(
                s.tasks().length() == 1 && s.tasks().optJSONObject(0).optString("time").isEmpty(),
                "all-day saved");
            a.editTask(null);
            a.editor.name.setText("项目讨论");
            a.editor.chooseMode(1);
            a.editor.endDate = a.editor.date;
            a.editor.start = "14:00";
            a.editor.end = "13:00";
            a.editor.submit();
            check(s.tasks().length() == 1 && a.editor.isShowing(), "invalid range stays in editor");
            a.editor.end = "15:30";
            a.editor.submit();
            a.editTask(null);
            a.editor.name.setText("阅读与记录");
            a.editor.chooseMode(1);
            a.editor.endDate = a.editor.date;
            a.editor.start = "09:00";
            a.editor.end = "10:00";
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
            a.editor.chooseMode(1);
            a.editor.endDate = a.editor.date;
            a.editor.start = "14:00";
            a.editor.end = "15:30";
            a.editor.submit();
            a.editTask(null);
            a.editor.name.setText("未保存草稿");
            a.editor.note.setText("草稿备注");
            a.editor.chooseMode(1);
            a.editor.endDate = a.editor.date;
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
            a.tab = 1;
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
      runOnMainSync(
          () -> {
            a.tab = 0;
            a.selected = LocalDate.now().plusDays(9);
            a.show();
            check(a.calendar == null, "today has no calendar");
            check(a.tasks().isEmpty(), "today excludes future dated tasks despite selected agenda date");
            a.tab = 2;
            a.show();
            check(a.calendar == null, "quadrants have no calendar");
            check(a.body.getChildCount() == 2, "quadrants are two rows");
            for (int i = 0; i < 4; i++) {
              LinearLayout pair = (LinearLayout) a.body.getChildAt(i / 2);
              pair.getChildAt(i % 2).performClick();
              check(a.editor.priority == 3 - i, "quadrant preselects priority");
              a.editor.name.setText(new String[] {"完成重要事项", "规划下周", "回复消息", "整理桌面"}[i]);
              a.editor.submit();
              check(
                  s.tasks().optJSONObject(s.tasks().length() - 1).optInt("priority") == 3 - i,
                  "quadrant priority saved");
            }
          });
      Thread.sleep(200);
      screenshot("quadrants-v21.png");
      runOnMainSync(
          () -> {
            a.tab = 3;
            a.show();
          });
      Thread.sleep(400);
      screenshot("settings-intro-v21.png");
      runOnMainSync(
          () -> {
            check(
                a.settingsScene.progress == 0
                    && a.settingsScene.buttons.getVisibility() == View.INVISIBLE,
                "settings initially only sentence");
            check(
                a.settingsScene.hero.getContentDescription().equals("时间只属于你"),
                "sentence meaning unchanged");
            SettingsScene scene = a.settingsScene;
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100, 100, 0);
            scene.dispatchTouchEvent(down);
            down.recycle();
            MotionEvent move =
                MotionEvent.obtain(
                    now, now + 20, MotionEvent.ACTION_MOVE, 100, scene.getHeight() * .7f, 0);
            scene.dispatchTouchEvent(move);
            move.recycle();
            MotionEvent up =
                MotionEvent.obtain(
                    now, now + 40, MotionEvent.ACTION_UP, 100, scene.getHeight() * .7f, 0);
            scene.dispatchTouchEvent(up);
            up.recycle();
          });
      Thread.sleep(400);
      runOnMainSync(
          () ->
              check(
                  a.settingsScene.progress > .9f && a.settingsScene.buttons.getAlpha() > .9f,
                  "down swipe reveals buttons"));
      screenshot("settings-actions-v21.png");
      runOnMainSync(
          () -> {
            a.tab = 0;
            a.show();
          });
      Thread.sleep(200);
      screenshot("home-v21.png");
      runOnMainSync(
          () -> {
            a.tab = 1;
            a.selected = LocalDate.now();
            a.overview = false;
            for (int i = 0; i < 3; i++) {
              JSONObject task = new JSONObject();
              a.put(task, "id", "overlap-" + i);
              a.put(task, "title", new String[] {"课程讨论", "项目评审", "笔记整理"}[i]);
              a.put(task, "date", a.selected.toString());
              a.put(task, "time", new String[] {"13:00", "14:00", "14:30"}[i]);
              a.put(task, "endTime", new String[] {"15:00", "16:00", "15:30"}[i]);
              a.put(task, "priority", 3 - i);
              s.tasks().put(task);
            }
            a.show();
            java.util.ArrayList<TimelineView.Slot> slots = TimelineView.arrange(a.tasks());
            for (int i = 0; i < slots.size(); i++)
              for (int j = i + 1; j < slots.size(); j++) {
                TimelineView.Slot x = slots.get(i), y = slots.get(j);
                if (x.start < y.end && y.start < x.end)
                  check(x.lane != y.lane, "overlapping events occupy different lanes");
              }
            check(slots.stream().anyMatch(x -> x.lanes >= 3), "three-way overlaps split lanes");
            a.timeline.getChildAt(0).performClick();
            check(a.detailsDialog.isShowing(), "tap opens details");
            a.detailsDialog.dismiss();
            a.timeline.getChildAt(0).performLongClick();
            check(a.tasks().stream().anyMatch(t -> t.optBoolean("done")), "long press completes");
          });
      Thread.sleep(2500);
      runOnMainSync(() -> {
        check(a.timeline.getChildAt(0).getWidth() > a.dp(40), "timeline blocks measured at visible width");
        check(a.timeline.getChildAt(0).getHeight() > a.dp(40), "timeline duration measured");
        ((ScrollView) a.stage.getChildAt(0)).scrollTo(0, a.timeline.getTop() + a.dp(12 * 76));
      });
      Thread.sleep(200);
      screenshot("timeline-v22.png");
      runOnMainSync(() -> {
        TimelineScroll scroll = (TimelineScroll)a.stage.getChildAt(0);
        scroll.zoomBy(.001f, a.dp(100));
        check(Math.abs(a.timeline.hourHeight * 15 - (scroll.getHeight()-a.dp(36))) < 2, "minimum zoom fits fifteen hours");
        scroll.zoomBy(1000, a.dp(100));
        check(Math.abs(a.timeline.hourHeight * 2 - (scroll.getHeight()-a.dp(36))) < 2, "maximum zoom fits two hours");
        scroll.zoomBy(.001f, a.dp(100));
      });
      Thread.sleep(150);
      runOnMainSync(() -> ((ScrollView)a.stage.getChildAt(0)).scrollTo(0,a.timeline.getTop()+Math.round(a.timeline.hourHeight*6)));
      Thread.sleep(150);
      screenshot("timeline-zoom-v24.png");
      runOnMainSync(() -> {
        JSONObject span = new JSONObject();
        a.put(span,"id","span-demo");a.put(span,"title","准备项目发布");
        a.put(span,"date",LocalDate.now().minusDays(1).toString());
        a.put(span,"endDate",LocalDate.now().plusDays(2).toString());
        a.put(span,"time","");a.put(span,"endTime","");a.put(span,"priority",3);
        s.tasks().put(span);
        a.editTask(null); a.editor.name.setText("不允许过去时间");a.editor.chooseMode(1);
            a.editor.endDate = a.editor.date;
        a.editor.date=LocalDate.now().minusDays(1);a.editor.endDate=LocalDate.now().minusDays(1);
        a.editor.start="09:00";a.editor.end="10:00";
        int count=s.tasks().length();a.editor.submit();
        check(a.editor.isShowing() && count==s.tasks().length(),"past start rejected");a.editor.dismiss();
      });
      runOnMainSync(
          () -> {
            a.overview = true;
            a.show();
          });
      Thread.sleep(200);
      screenshot("calendar-month-v22.png");
      runOnMainSync(() -> {
        a.overview = false;
        a.show();
        a.calendarPopup = new CalendarPopup(a, a.root.getChildAt(0));
        a.calendarPopup.show();
        check(a.calendarPopup.isShowing(), "calendar overlay opens");
      });
      Thread.sleep(500);
      screenshot("calendar-popup-v23.png");
      runOnMainSync(() -> {
        a.calendarPopup.calendar.setCollapse(.5f);
        check(a.calendarPopup.calendar.rows.get(0).getAlpha() < 1,"other weeks fade continuously");
      });
      Thread.sleep(100);
      screenshot("calendar-mid-v24.png");
      runOnMainSync(() -> a.calendarPopup.calendar.setMonth(false));
      Thread.sleep(500);
      screenshot("calendar-week-v23.png");
      runOnMainSync(() -> {
        check(!a.calendarPopup.calendar.month, "calendar collapses to week");
        a.calendarPopup.dismiss();
      });
      runOnMainSync(
          () -> {
            a.tab = 3;
            a.show();
          });
      Thread.sleep(350);
      screenshot("settings-v23.png");
      runOnMainSync(
          () -> {
            a.tab = 0;
            a.overview = false;
            a.show();
          });
      result.putString(
          "stream",
          "PASS: today-only, quadrant add priorities, immersive sentence, swipe reveal, v1/v2/v3"
              + " backups, legacy archive, all-day/range editor, timeline dimensions and overlap lanes, details and long press, invalid ranges,"
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
