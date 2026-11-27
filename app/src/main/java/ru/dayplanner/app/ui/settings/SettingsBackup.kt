package ru.dayplanner.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.dayplanner.app.R
import ru.dayplanner.app.container
import ru.dayplanner.app.data.Backup
import ru.dayplanner.app.ui.components.*
import java.time.LocalDate

@Composable
fun BackupSection(){ val c=LocalContext.current.applicationContext;var pending by rememberSaveable {mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)};var message by rememberSaveable {mutableIntStateOf(0)}
    fun run(uri:Uri,restore:Boolean){busy=true;message=0;c.container.launch {try { if(restore)Backup.restore(c,uri) else Backup.export(c,uri);message=if(restore) R.string.backup_imported else R.string.backup_exported }catch(e:Exception){message=if(restore) R.string.backup_bad else R.string.backup_failed}finally{busy=false} }}
    val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){it?.let{uri->run(uri,false)}};val load=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){pending=it?.toString()}
    SectionCard(title=stringResource(R.string.backup)){OutlinedButton(enabled=!busy,onClick={save.launch("planner-${LocalDate.now()}.zip")}){Text(stringResource(R.string.backup_export))};OutlinedButton(enabled=!busy,onClick={load.launch(arrayOf("application/zip","application/octet-stream","application/x-zip-compressed"))}){Text(stringResource(R.string.backup_import))};if(busy)LinearProgressIndicator();if(message!=0)Text(stringResource(message))}
    pending?.let {path->ConfirmDialog(stringResource(R.string.backup_import),stringResource(R.string.backup_warning),stringResource(R.string.backup_import),{pending=null;run(Uri.parse(path),true)},{pending=null}) }
}
