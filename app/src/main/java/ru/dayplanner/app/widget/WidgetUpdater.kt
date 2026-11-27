@file:Suppress("DEPRECATION")
package ru.dayplanner.app.widget

import android.app.*
import android.appwidget.*
import android.content.*
import android.net.Uri
import android.os.*
import android.text.*
import android.text.style.StrikethroughSpan
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.dayplanner.app.*
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.signals.SignalScheduler
import ru.dayplanner.app.ui.Fmt
import ru.dayplanner.app.ui.editor.EditorScreen
import ru.dayplanner.app.ui.task.TaskScreen
import ru.dayplanner.app.ui.effects.EffectsHost
import ru.dayplanner.app.ui.theme.PlannerTheme
import java.time.*

object WidgetUpdater {
    private const val MIDNIGHT="ru.dayplanner.app.widget.MIDNIGHT"
    fun requestUpdate(context:Context){val c=context.applicationContext;val mgr=AppWidgetManager.getInstance(c);val ids=mgr.getAppWidgetIds(ComponentName(c,PlannerWidget::class.java));val am=c.getSystemService(AlarmManager::class.java);val tick=PendingIntent.getBroadcast(c,0,Intent(c,PlannerWidget::class.java).setAction(MIDNIGHT),PendingIntent.FLAG_IMMUTABLE)
        if(ids.isEmpty()){am.cancel(tick);return};val today=LocalDate.now();val next=today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();try{if(SignalScheduler.exact(c))am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick)}catch(_:SecurityException){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,tick)}
        for(id in ids){val rv=RemoteViews(c.packageName,R.layout.widget);rv.setTextViewText(R.id.header,Fmt.fullDate(today.toEpochDay()));val adapter=Intent(c,RowsService::class.java).setData(Uri.parse("planner-widget://local/list/$id"));rv.setRemoteAdapter(R.id.rows,adapter);rv.setEmptyView(R.id.rows,R.id.empty)
            fun entry(action:String,mutable:Boolean=false):PendingIntent{val i=Intent(c,WidgetEntryActivity::class.java).setAction(action);val flag=if(!mutable) PendingIntent.FLAG_IMMUTABLE else if(Build.VERSION.SDK_INT>=31)PendingIntent.FLAG_MUTABLE else 0;return PendingIntent.getActivity(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT or flag)}
            rv.setOnClickPendingIntent(R.id.header,entry("today"));rv.setOnClickPendingIntent(R.id.add,entry("add"));rv.setPendingIntentTemplate(R.id.rows,entry("row",true));mgr.updateAppWidget(id,rv)
        };mgr.notifyAppWidgetViewDataChanged(ids,R.id.rows)
    }
    fun isTick(i:Intent)=i.action==MIDNIGHT||i.action==Intent.ACTION_DATE_CHANGED
}
class PlannerWidget:AppWidgetProvider(){override fun onReceive(c:Context,i:Intent){super.onReceive(c,i);if(WidgetUpdater.isTick(i))WidgetUpdater.requestUpdate(c)};override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){WidgetUpdater.requestUpdate(c)}; override fun onDisabled(c:Context){WidgetUpdater.requestUpdate(c)} }
class RowsService:RemoteViewsService(){override fun onGetViewFactory(i:Intent):RemoteViewsFactory=Rows(applicationContext)}
private class Rows(private val c:Context):RemoteViewsService.RemoteViewsFactory {
    private var items:List<Occurrence> = emptyList();override fun onCreate(){};override fun onDestroy(){items=emptyList()};override fun onDataSetChanged(){val token=Binder.clearCallingIdentity();try {items=c.container.repository.snapshotBlocking().occurrencesOn(LocalDate.now().toEpochDay())}finally{Binder.restoreCallingIdentity(token)}}
    override fun getCount()=items.size;override fun getViewAt(index:Int):RemoteViews?{val o=items.getOrNull(index)?:return null;val line=listOf(TimeText.label(o.task),o.task.title).filter{it.isNotEmpty()}.joinToString("  ");val text=SpannableString(line);if(o.done)text.setSpan(StrikethroughSpan(),0,line.length,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return RemoteViews(c.packageName,R.layout.widget_row).apply{setTextViewText(R.id.row_text,text);setOnClickFillInIntent(R.id.row_text,Intent().putExtra("id",o.task.id).putExtra("day",o.day))}}
    override fun getLoadingView():RemoteViews?=null;override fun getViewTypeCount()=1;override fun getItemId(i:Int)=items.getOrNull(i)?.task?.id?:-1L;override fun hasStableIds()=true
}
class WidgetEntryActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val id=intent.getLongExtra("id",0);val day=intent.getLongExtra("day",LocalDate.now().toEpochDay());if(intent.action=="today"||(intent.action=="row"&&id<=0)){startActivity(Intent(this,MainActivity::class.java));finish();return};enableEdgeToEdge();setContent {val style by container.settings.style.collectAsStateWithLifecycle();var editor by rememberSaveable{mutableStateOf(intent.action=="add")};val close:()->Unit={if(id==0L||!editor)finish() else editor=false};PlannerTheme(style){EffectsHost(style){if(editor)EditorScreen(id,day,close) else TaskScreen(id,day,{finish()},{finish()},{editor=true})}};BackHandler(onBack=close) }}}
