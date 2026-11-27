package ru.dayplanner.app.signals

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.dayplanner.app.MainActivity
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.*
import java.time.*

object SignalScheduler {
    private val lock=Mutex()
    fun requestReschedule(context:Context) { val app=context.applicationContext; app.container.launch { refresh(app) } }
    fun exact(c:Context)=Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun intent(c:Context,key:String)=Intent(c,SignalReceiver::class.java).setData(Uri.parse("planner-signal://local/$key"))
    suspend fun refresh(c:Context)=withContext(Dispatchers.IO) { lock.withLock {
        val am=c.getSystemService(AlarmManager::class.java);val prefs=c.getSharedPreferences("armed",0)
        val previous=prefs.getStringSet("keys",emptySet()).orEmpty().toSet();val armed=mutableSetOf<String>();val data=c.container.repository.snapshot();val zone=ZoneId.systemDefault();val now=LocalDateTime.now(zone)
        for(t in data.tasks) for(kind in SignalKind.entries){
            val next=Schedule.nextSignal(t,kind,now,data.skipped,data.completions,horizonDays=maxOf(400L,t.startDay-now.toLocalDate().toEpochDay()+data.skipped.size+data.completions.size+8L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) ?: continue
            val time=next.atZone(zone).toInstant().toEpochMilli();if(time<=System.currentTimeMillis()) continue
            val key="${t.id}/${kind.name}"; val i=intent(c,key).putExtra("day",next.toLocalDate().toEpochDay()).putExtra("at",time)
            val p=PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try { if(!exact(c)) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p) else if(t.signalType==SignalType.ALARM){ val show=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE); am.setAlarmClock(AlarmManager.AlarmClockInfo(time,show),p) } else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p);armed+=key }
            catch(e:SecurityException){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p); armed+=key}
        }
        for(key in previous-armed){PendingIntent.getBroadcast(c,0,intent(c,key),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { am.cancel(it);it.cancel() }}
        prefs.edit().putStringSet("keys",armed).commit();Unit
    } }
}

class ResetReceiver:BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){val pending=goAsync();val app=c.applicationContext;app.container.launch { try { SignalScheduler.refresh(app);ru.dayplanner.app.widget.WidgetUpdater.requestUpdate(app) } finally {pending.finish()} } } }
class SignalReceiver:BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){val segments=i.data?.pathSegments ?: return;val id=segments.getOrNull(0)?.toLongOrNull() ?: return;val kind=runCatching {SignalKind.valueOf(segments[1])}.getOrNull() ?: return;val day=i.getLongExtra("day",Long.MIN_VALUE);val at=i.getLongExtra("at",0);val app=c.applicationContext;val pending=goAsync()
    app.container.launch { try { val data=app.container.repository.snapshot();val t=data.tasks.firstOrNull{it.id==id};if(t!=null&&Schedule.occursOn(t,day,data.skipped)&&DayKey(id,day) !in data.completions && day==LocalDate.now().toEpochDay()){val m=Schedule.signalMinutes(t,kind);if(m!=null && LocalDate.ofEpochDay(day).atTime(m/60,m%60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()==at) Signals.deliver(app,t,kind)};SignalScheduler.refresh(app) } catch(e:Exception){Log.w("Planner","Не удалось обработать сигнал",e)} finally {pending.finish()} }
} }
