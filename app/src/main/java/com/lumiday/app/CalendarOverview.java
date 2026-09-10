package com.lumiday.app;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
import org.json.JSONObject;

final class CalendarOverview {
  final MainActivity host;
  final LinearLayout parent;
  final ArrayList<LinearLayout> rows = new ArrayList<>();
  final ArrayList<LinearLayout> cells = new ArrayList<>();
  boolean month = true;
  float collapse;
  int selectedWeek;
  ValueAnimator animation;

  CalendarOverview(MainActivity host, LinearLayout parent) {
    this.host = host; this.parent = parent; render();
  }
  ArrayList<JSONObject> onDate(LocalDate date) {
    ArrayList<JSONObject> result = new ArrayList<>();
    for (int i=0;i<host.store.tasks().length();i++) {
      JSONObject t = host.store.tasks().optJSONObject(i);
      if (TaskDates.occurs(t,date)) result.add(t);
    }
    result.sort(Comparator.comparingInt(TaskDates::rank).thenComparing(t -> t.optString("time")).thenComparing(t -> t.optString("id")));
    return result;
  }
  void setMonth(boolean value) {
    if (animation != null) animation.cancel();
    month = value;
    animation = ValueAnimator.ofFloat(collapse, value ? 0 : 1);
    animation.setDuration(440);
    animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
    animation.addUpdateListener(a -> setCollapse((float)a.getAnimatedValue()));
    animation.start();
  }
  void setCollapse(float value) {
    collapse = Math.max(0,Math.min(1,value));
    for (int i=0;i<rows.size();i++) {
      LinearLayout row = rows.get(i);
      int height = host.dp(i == selectedWeek ? 88 + 100 * collapse : 88 * (1-collapse));
      row.getLayoutParams().height = height;
      row.setAlpha(i == selectedWeek ? 1 : (1-collapse)*(1-collapse));
      row.requestLayout();
      for (int c=0;c<row.getChildCount();c++) {
        LinearLayout cell = (LinearLayout)row.getChildAt(c);
        // Extra entries unfold after the selected week has reached the top.
        for (int n=3;n<cell.getChildCount();n++) {
          View entry = cell.getChildAt(n);
          if (entry.getTag() instanceof int[]) {
            TextView badge=(TextView)entry;
            int[] counts=(int[])badge.getTag();
            int hidden=collapse>.7f ? counts[1] : counts[0];
            badge.setText(hidden>0 ? "+"+hidden : "");
          }
          if ("extra".equals(entry.getTag())) entry.setAlpha(Math.max(0,(collapse-.7f)/.3f));
        }
      }
    }
  }
  void render() {
    parent.removeAllViews(); rows.clear(); cells.clear();
    LinearLayout card = host.vertical();
    card.setBackground(host.shape(android.graphics.Color.WHITE,24));
    card.setPadding(host.dp(8),host.dp(8),host.dp(8),host.dp(12));
    parent.addView(card);
    LinearLayout controls = host.row();
    controls.addView(host.icon("left","上一周期",() -> {host.selected = month ? host.selected.minusMonths(1) : host.selected.minusWeeks(1); render();}));
    TextView heading = host.text(host.selected.getYear()+" 年 "+host.selected.getMonthValue()+" 月",17,host.GREEN);
    heading.setGravity(Gravity.CENTER); host.weighted(controls,heading);
    controls.addView(host.icon("right","下一周期",() -> {host.selected = month ? host.selected.plusMonths(1) : host.selected.plusWeeks(1); render();}));
    card.addView(controls);
    LinearLayout labels = host.row();
    for(String day:new String[]{"日","一","二","三","四","五","六"}) {
      TextView t=host.text(day,12,host.MUTED);t.setGravity(Gravity.CENTER);host.weighted(labels,t);
    }
    card.addView(labels);
    LocalDate first = host.selected.withDayOfMonth(1);
    LocalDate start = first.minusDays(first.getDayOfWeek().getValue()%7);
    selectedWeek = (int)(java.time.temporal.ChronoUnit.DAYS.between(start,host.selected)/7);
    for(int r=0;r<6;r++) {
      LinearLayout line=host.row(); line.setGravity(Gravity.TOP); line.setClipChildren(true);
      rows.add(line); card.addView(line,new LinearLayout.LayoutParams(-1,host.dp(88)));
      LocalDate weekStart = start.plusDays(r*7);
      // Use the same slot for a spanning task on every covered day of this week.
      ArrayList<JSONObject> spans = new ArrayList<>();
      for(int j=0;j<host.store.tasks().length();j++) {
        JSONObject t=host.store.tasks().optJSONObject(j);
        if(!t.optBoolean("done") && TaskDates.special(t) && !t.optBoolean("undated")
            && !TaskDates.end(t).isBefore(weekStart) && !TaskDates.start(t).isAfter(weekStart.plusDays(6))) spans.add(t);
      }
      spans.sort(Comparator.comparing(t -> t.optString("id")));
      for(int c=0;c<7;c++) {
        LocalDate date=weekStart.plusDays(c);
        LinearLayout cell=host.vertical(); cell.setPadding(0,host.dp(4),0,0);
        line.addView(cell,new LinearLayout.LayoutParams(0,host.dp(188),1)); cells.add(cell);
        TextView number=host.text(""+date.getDayOfMonth(),13,date.equals(host.selected)?host.GREEN:host.INK);
        number.setGravity(Gravity.CENTER);cell.addView(number,new LinearLayout.LayoutParams(-1,host.dp(23)));
        ArrayList<JSONObject> actual=onDate(date), ordered=new ArrayList<>();
        for(JSONObject t:spans) ordered.add(TaskDates.occurs(t,date)?t:null);
        for(JSONObject t:actual) if(!spans.contains(t)) ordered.add(t);
        int shown=0, firstShown=0;
        for(int n=0;n<Math.min(5,ordered.size());n++) {
          JSONObject task=ordered.get(n);
          TextView chip=host.text(task==null?"":task.optString("title"),10,Color.WHITE);
          chip.setSingleLine(true);chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
          chip.setPadding(host.dp(3),0,host.dp(2),0);
          LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,host.dp(20));cp.topMargin=host.dp(2);
          cell.addView(chip,cp);
          if(n>=2)chip.setTag("extra");
          if(task!=null) {
            shown++;
            if(n<2)firstShown++;
            int color=host.priorityColor(task.optInt("priority"));
            chip.setBackground(host.shape(task.optBoolean("done")?TimelineView.pale(color):color,TaskDates.special(task)?0:3));
            if(task.optBoolean("done"))chip.setPaintFlags(chip.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
            chip.setOnClickListener(v -> host.taskDetails(task));
            chip.setOnLongClickListener(v -> {
              JSONObject before=host.snapshot();
              host.put(task,"done",!task.optBoolean("done"));
              host.put(task,"completedDate",LocalDate.now().toString());
              if(host.persist(before))render();
              return true;
            });
          }
        }
        // Count badge overlays the last visible row, below the date, for either extent.
        if(actual.size()>firstShown) {
          TextView more=host.text("+"+Math.max(0,actual.size()-2),10,host.MUTED);
          more.setTag(new int[]{actual.size()-firstShown,actual.size()-shown});
          cell.addView(more,3,new LinearLayout.LayoutParams(-1,host.dp(17)));
          more.setOnClickListener(v -> openDay(date));
        }
        cell.setOnClickListener(v -> openDay(date));
      }
    }
    setCollapse(collapse);
  }
  void openDay(LocalDate day) {
    if(host.calendarPopup!=null)host.calendarPopup.dismiss();
    host.selected=day;host.overview=false;host.show();
  }
}
