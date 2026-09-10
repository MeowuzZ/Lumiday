package com.lumiday.app;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.JSONObject;

/** Interval partitioning: touching endpoints do not overlap; each connected group shares width. */
final class TimelineView extends android.widget.FrameLayout {
  static final class Slot {
    JSONObject task;
    int start, end, lane, lanes;

    Slot(JSONObject task) {
      this.task = task;
      start = Store.minutes(task.optString("time"));
      end = Store.minutes(task.optString("endTime"));
    }
  }

  static ArrayList<Slot> arrange(List<JSONObject> tasks) {
    ArrayList<Slot> slots = new ArrayList<>();
    for (JSONObject t : tasks) if (!t.optString("time").isEmpty()) slots.add(new Slot(t));
    slots.sort(Comparator.comparingInt((Slot s) -> s.start).thenComparingInt(s -> s.end));
    ArrayList<Slot> group = new ArrayList<>();
    ArrayList<Integer> ends = new ArrayList<>();
    int groupEnd = -1;
    for (Slot slot : slots) {
      if (slot.start >= groupEnd) {
        for (Slot old : group) old.lanes = ends.size();
        group.clear();
        ends.clear();
      }
      int lane = 0;
      while (lane < ends.size() && ends.get(lane) > slot.start) lane++;
      if (lane == ends.size()) ends.add(slot.end);
      else ends.set(lane, slot.end);
      slot.lane = lane;
      group.add(slot);
      groupEnd = Math.max(groupEnd, slot.end);
    }
    for (Slot old : group) old.lanes = ends.size();
    return slots;
  }

  final MainActivity host;
  final ArrayList<Slot> slots;
  float hourHeight;
  final Paint paint = new Paint(3);

  TimelineView(MainActivity host, List<JSONObject> tasks) {
    super(host);
    this.host = host;
    hourHeight = host.timelineHourHeight > 0 ? host.timelineHourHeight : host.dp(76);
    java.util.ArrayList<JSONObject> clipped = new java.util.ArrayList<>();
    for (JSONObject t : tasks) {
      if (t.optString("time").isEmpty()) continue;
      try {
        JSONObject copy = new JSONObject(t.toString());
        int from = TaskDates.from(t, host.selected), to = TaskDates.to(t, host.selected);
        copy.put("time", String.format(java.util.Locale.ROOT, "%02d:%02d", from/60, from%60));
        copy.put("endTime", String.format(java.util.Locale.ROOT, "%02d:%02d", to/60, to%60));
        clipped.add(copy);
      } catch (Exception e) { throw new IllegalStateException(e); }
    }
    slots = arrange(clipped);
    for (Slot slot : slots) {
      slot.lanes = Math.max(3, slot.lanes);
      for (JSONObject original : tasks) if (original.optString("id").equals(slot.task.optString("id"))) slot.task = original;
    }
    setWillNotDraw(false);
    for (Slot slot : slots) {
      TextView block =
          host.text(
              slot.task.optString("title") + "\n" + host.timeLabel(slot.task), 13, Color.WHITE);
      block.setGravity(Gravity.TOP);
      block.setEllipsize(android.text.TextUtils.TruncateAt.END);
      block.setPadding(host.dp(6), host.dp(3), host.dp(4), 0);
      block.setBackground(host.shape(slot.task.optBoolean("done") ? pale(host.priorityColor(slot.task.optInt("priority"))) : host.priorityColor(slot.task.optInt("priority")), 5));
      if (slot.task.optBoolean("done")) {

        block.setPaintFlags(block.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      }
      block.setContentDescription(slot.task.optString("title") + " " + host.timeLabel(slot.task));
      block.setOnClickListener(v -> host.taskDetails(slot.task));
      block.setOnLongClickListener(
          v -> {
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            host.completeTask(slot.task);
            return true;
          });
      addView(block, new LayoutParams(1, 1));
    }
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int w = MeasureSpec.getSize(widthSpec);
    for (int i = 0; i < slots.size(); i++) {
      Slot s = slots.get(i);
      float laneWidth = (w - host.dp(60)) / (float) s.lanes;
      LayoutParams p = (LayoutParams) getChildAt(i).getLayoutParams();
      p.leftMargin = host.dp(54) + Math.round(s.lane * laneWidth);
      p.topMargin = host.dp(18) + Math.round(s.start * hourHeight / 60f);
      p.width = Math.max(1, Math.round(laneWidth) - host.dp(3));
      p.height = Math.max(1, Math.round((s.end - s.start) * hourHeight / 60f));
      ((TextView) getChildAt(i)).setMaxLines(Math.max(1, (p.height-host.dp(6)) / host.dp(17)));
    }
    super.onMeasure(widthSpec, heightSpec);
  }

  static int pale(int color) {
    return Color.rgb((Color.red(color)+510)/3, (Color.green(color)+510)/3, (Color.blue(color)+510)/3);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    paint.setTextSize(host.dp(11));
    for (int hour = 0; hour <= 24; hour++) {
      float y = host.dp(18) + hour * hourHeight;
      paint.setColor(0xffdfe4dd);
      paint.setStrokeWidth(1);
      canvas.drawLine(host.dp(52), y, getWidth(), y, paint);
      paint.setColor(host.MUTED);
      canvas.drawText(
          String.format(Locale.ROOT, "%02d:00", hour), host.dp(8), y + host.dp(4), paint);
    }
  }
}
