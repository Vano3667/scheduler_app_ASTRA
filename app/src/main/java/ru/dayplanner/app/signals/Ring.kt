package ru.dayplanner.app.signals

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.dayplanner.app.*
import ru.dayplanner.app.domain.*
import ru.dayplanner.app.ui.theme.*

object Signals {
    const val ACTIVE=7701
    fun full(c:Context)=Build.VERSION.SDK_INT<34 || c.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    fun channels(c:Context){val nm=c.getSystemService(NotificationManager::class.java)
        for((key,name) in listOf("notice" to R.string.notice_channel,"alarm" to R.string.ring_channel,"ring" to R.string.ring_service_channel)){
            val ch=NotificationChannel(key,c.getString(name),NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true)
            if(key=="ring")ch.setSound(null,null) else if(key=="alarm")ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());nm.createNotificationChannel(ch)
        }
    }
    fun deliver(c:Context,t:Task,k:SignalKind){channels(c);val nm=c.getSystemService(NotificationManager::class.java);if(!nm.areNotificationsEnabled())return
        val alarm=t.signalType==SignalType.ALARM;val label=c.getString(if(k==SignalKind.START)R.string.signal_when_start else R.string.signal_when_end);val title=t.title
        if(alarm && full(c) && SignalScheduler.exact(c) && nm.getNotificationChannel("ring").importance>=NotificationManager.IMPORTANCE_HIGH){ try { c.startForegroundService(Intent(c,RingService::class.java).putExtra("title",title).putExtra("label",label));return } catch(_:RuntimeException){} }
        val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        nm.notify("${t.id}:${k.name}",1,Notification.Builder(c,if(alarm) "alarm" else "notice").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setCategory(if(alarm) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER).setContentTitle(title).setContentText(label).setAutoCancel(true).setContentIntent(open).build())
    }
}
class RingService:Service(){
    private var player:Ringtone?=null
    override fun onBind(i:Intent?)=null
    override fun onStartCommand(i:Intent?,flags:Int,id:Int):Int {
        if(i==null||i.action=="stop"){stopSelf();return START_NOT_STICKY};Signals.channels(this)
        val title=i.getStringExtra("title").orEmpty();val label=i.getStringExtra("label").orEmpty()
        val screen=PendingIntent.getActivity(this,1,Intent(this,AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("title",title).putExtra("label",label),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,2,Intent(this,RingService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
        val builder=Notification.Builder(this,"ring").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setContentTitle(title).setContentText(label).setContentIntent(screen).setFullScreenIntent(screen,true).addAction(Notification.Action.Builder(null,getString(R.string.ring_stop),stop).build());if(Build.VERSION.SDK_INT>=31) builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        startForeground(Signals.ACTIVE,builder.build()); player?.stop();val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE); player=RingtoneManager.getRingtone(this,uri)?.apply {audioAttributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();isLooping=true;play()};return START_NOT_STICKY
    }
    override fun onDestroy(){player?.stop();stopForeground(STOP_FOREGROUND_REMOVE);sendBroadcast(Intent("ru.dayplanner.app.ALARM_STOPPED").setPackage(packageName));super.onDestroy()}
}
class AlarmActivity:ComponentActivity(){
    private val closed=object:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){finishAndRemoveTask()}}
    override fun onCreate(state:Bundle?){super.onCreate(state);androidx.core.content.ContextCompat.registerReceiver(this,closed,IntentFilter("ru.dayplanner.app.ALARM_STOPPED"),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { PlannerTheme(AppStyle.NORMAL){Surface(Modifier.fillMaxSize()){ Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text(intent.getStringExtra("label").orEmpty());Text(intent.getStringExtra("title").orEmpty(),style=MaterialTheme.typography.headlineLarge);Spacer(Modifier.height(24.dp));Button(onClick={stopService(Intent(this@AlarmActivity,RingService::class.java));finishAndRemoveTask()}){Text(stringResource(R.string.ring_stop))} } }} }
    }; override fun onDestroy(){unregisterReceiver(closed);super.onDestroy()}
}
