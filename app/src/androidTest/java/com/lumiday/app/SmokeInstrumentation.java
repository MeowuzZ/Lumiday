package com.lumiday.app;
import android.app.*;import android.os.*;import android.content.*;import org.json.*;import java.time.*;
/** Device tests use the test APK's own data directory; user records are untouched. */
public class SmokeInstrumentation extends Instrumentation {
 void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  java.io.File testDir=getTargetContext().getDir("instrumentation",0);new java.io.File(testDir,"lumiday.json").delete();Store s=new Store(testDir);
  JSONObject t=new JSONObject().put("id","task").put("title","测试任务").put("date","2026-09-09").put("time","18:30").put("done",true);
  JSONObject h=new JSONObject().put("id","habit").put("title","喝水").put("target",3).put("logs",new JSONObject().put("2026-09-09",2));
  s.tasks().put(t);s.habits().put(h);s.save();Store again=new Store(testDir);
  check(again.tasks().getJSONObject(0).getBoolean("done"),"task completion persistence");
  check(again.habits().getJSONObject(0).getJSONObject("logs").getInt("2026-09-09")==2,"partial habit persistence");
  String raw=s.data.toString();JSONObject legacy=new JSONObject(raw);legacy.put("schemaVersion",1);legacy.getJSONArray("habits").getJSONObject(0).remove("logs");
  check(Store.validate(legacy.toString()).getInt("schemaVersion")==2,"v1 migration");
  s.restore(raw);check(s.tasks().length()==1,"restore round trip");
  check(new java.io.File(testDir,"before-restore.json").exists(),"pre-restore snapshot");
  boolean rejected=false;try{s.restore("{\"schemaVersion\":99,\"tasks\":[],\"habits\":[]}");}catch(Exception e){rejected=true;}check(rejected&&s.tasks().length()==1,"future schema protects existing data");
  rejected=false;try{Store.validate(raw.replace("2026-09-09","2026-99-99"));}catch(Exception e){rejected=true;}check(rejected,"invalid dates rejected");
  MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  runOnMainSync(()->{for(int i=0;i<5;i++){a.tab=i;a.show();}a.tab=0;a.show();a.toggleCalendar();});
  Thread.sleep(500);runOnMainSync(()->check(a.expanded,"calendar expand"));
  runOnMainSync(()->a.toggleCalendar());Thread.sleep(500);runOnMainSync(()->check(!a.expanded,"calendar collapse"));
  runOnMainSync(()->{try{JSONObject sample=new JSONObject().put("title","喝水").put("emoji","💧").put("target",3).put("logs",new JSONObject().put(LocalDate.now().toString(),2));a.habitDetail(sample);check(a.stats(sample)[0]==0,"partial does not count as completed");sample.getJSONObject("logs").put(LocalDate.now().toString(),3);check(a.stats(sample)[0]==1,"complete counts");a.show();}catch(Exception e){throw new RuntimeException(e);}});
  result.putString("stream","PASS: persistence, partial progress, migration, restore, snapshot, invalid backups, five screens, calendar animation, habit statistics\n");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
