package ru.dayplanner.app.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ru.dayplanner.app.R
import ru.dayplanner.app.signals.*
import ru.dayplanner.app.ui.components.SectionCard

@Composable
fun PermissionsSection(alarm:Boolean=true){val c=LocalContext.current;val nm=c.getSystemService(NotificationManager::class.java);var revision by remember {mutableIntStateOf(0)};val notify=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){revision++};LifecycleEventEffect(Lifecycle.Event.ON_RESUME){revision++;SignalScheduler.requestReschedule(c)};val uri=Uri.fromParts("package",c.packageName,null)
    key(revision){SectionCard(title=stringResource(R.string.signal_permissions)){
        if(!nm.areNotificationsEnabled()){Text(stringResource(R.string.need_notice));if(Build.VERSION.SDK_INT>=33)TextButton(onClick={notify.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text(stringResource(R.string.grant))}}
        TextButton(onClick={runCatching{c.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,c.packageName))}}){Text(stringResource(R.string.system_notice))}
        if(Build.VERSION.SDK_INT>=31&&!SignalScheduler.exact(c)){Text(stringResource(R.string.need_exact));TextButton(onClick={runCatching{c.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,uri))}}){Text(stringResource(R.string.grant))}}
        if(alarm && Build.VERSION.SDK_INT>=34 && !Signals.full(c)){Text(stringResource(R.string.need_full));TextButton(onClick={runCatching{c.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,uri))}}){Text(stringResource(R.string.grant))}}
        if(nm.areNotificationsEnabled()&&SignalScheduler.exact(c)&&(!alarm||Signals.full(c))) Text(stringResource(R.string.signals_ok))
    }}
}
