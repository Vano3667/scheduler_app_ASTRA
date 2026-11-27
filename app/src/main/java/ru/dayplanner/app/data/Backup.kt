package ru.dayplanner.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.dayplanner.app.container
import ru.dayplanner.app.domain.*
import java.io.*
import java.util.UUID
import java.util.zip.*

/** Архив не содержит SQL, сериализованных объектов JVM или путей, используемых при записи на диск. */
object Backup {
    private const val META="planner-v1.json"
    private const val META_LIMIT=16L*1024*1024
    private const val TOTAL=1024L*1024*1024
    private const val FILE_LIMIT=128L*1024*1024
    private const val MAX_ENTRIES=10000
    private const val MAX_ROWS=100000
    private fun row(vararg values:Any?):JSONArray=JSONArray().apply { values.forEach { put(it ?: JSONObject.NULL) } }
    private fun <T> rows(list:List<T>, f:(T)->JSONArray)=JSONArray().apply { require(list.size<=MAX_ROWS);list.forEach { put(f(it)) } }
    private fun <T> decode(obj:JSONObject,key:String,n:Int,f:(JSONArray)->T):List<T>{val a=obj.getJSONArray(key);require(a.length()<=MAX_ROWS);return List(a.length()){f(a.getJSONArray(it).also { r->require(r.length()==n) })}}
    private fun name(i:Int)="photos/$i"
    private fun copy(input:InputStream,out:OutputStream,limit:Long,expectedCrc:Long?=null):Long { val buf=ByteArray(32768);val crc=CRC32();var count=0L;while(true){val n=input.read(buf);if(n<0)break;count+=n;require(count<=limit);out.write(buf,0,n);if(expectedCrc!=null)crc.update(buf,0,n)};require(expectedCrc==null||crc.value==expectedCrc);return count }

    suspend fun export(c:Context,uri:Uri)=withContext(Dispatchers.IO){val box=c.container;val dao=box.db.dao();val tmp=File.createTempFile("export-",".zip",c.cacheDir)
        try { box.db.withTransaction { val photos=dao.getAllPhotos();val names=photos.map { it.fileName }.distinct();require(names.size<MAX_ENTRIES);val ids=names.withIndex().associate{it.value to it.index};val obj=JSONObject().put("format","ru.dayplanner.app/1").put("images",names.size)
            obj.put("tasks",rows(dao.getTasks()){t->row(t.id,t.title,t.description,t.timeMode.name,t.startMinutes,t.endMinutes,t.startDay,t.endDay,t.repeatMode.name,t.weekdaysMask,t.signalStart,t.signalEnd,t.signalType.name,t.createdAt)})
            obj.put("templates",rows(dao.getTemplates()){t->row(t.id,t.title,t.description,t.signalStart,t.signalEnd,t.signalType.name)})
            obj.put("photos",rows(photos){p->row(p.taskId,ids.getValue(p.fileName),p.position)})
            obj.put("done",rows(dao.getCompletions()){row(it.taskId,it.day)});obj.put("skip",rows(dao.getSkipped()){row(it.taskId,it.day)})
            val bytes=obj.toString().toByteArray(Charsets.UTF_8);require(bytes.size<=META_LIMIT);var left=TOTAL-bytes.size
            ZipOutputStream(tmp.outputStream().buffered()).use { z->z.putNextEntry(ZipEntry(META));z.write(bytes);z.closeEntry();names.forEachIndexed { i,n->z.putNextEntry(ZipEntry(name(i)));val size=box.photoStore.file(n).inputStream().use{copy(it,z,minOf(left,FILE_LIMIT))};require(size>0);left-=size;z.closeEntry() } }
        };require(tmp.length()<=TOTAL);requireNotNull(c.contentResolver.openOutputStream(uri,"wt")).use { out->tmp.inputStream().use{it.copyTo(out)} };Unit } finally {tmp.delete()}
    }

    suspend fun restore(c:Context,uri:Uri)=withContext(Dispatchers.IO){val box=c.container;val dao=box.db.dao();val tmp=File.createTempFile("import-",".zip",c.cacheDir);val staged=mutableListOf<File>();var committed=false
        try { requireNotNull(c.contentResolver.openInputStream(uri)).use{input->tmp.outputStream().use {copy(input,it,TOTAL)}}
            ZipFile(tmp).use { z->require(z.size() in 1..MAX_ENTRIES);val meta=requireNotNull(z.getEntry(META));require(meta.size in 1..META_LIMIT)
                val bytes=ByteArrayOutputStream();z.getInputStream(meta).use { require(copy(it,bytes,META_LIMIT,meta.crc)==meta.size) };val obj=JSONObject(bytes.toString("UTF-8"));require(obj.getString("format")=="ru.dayplanner.app/1");val n=obj.getInt("images");require(n in 0 until MAX_ENTRIES && z.size()==n+1)
                fun date(x:Long)=x.also{require(it in 0L..79999L)}
                val tasks=decode(obj,"tasks",14){r->Task(r.getLong(0),r.getString(1),r.getString(2),TimeMode.valueOf(r.getString(3)),r.getInt(4),r.getInt(5),date(r.getLong(6)),if(r.isNull(7))null else date(r.getLong(7)),RepeatMode.valueOf(r.getString(8)),r.getInt(9),r.getBoolean(10),r.getBoolean(11),SignalType.valueOf(r.getString(12)),r.getLong(13)).also { t->require(t.id>0&&t.title.isNotBlank()&&t.startMinutes in 0..1439&&t.endMinutes in 0..1439&&(t.timeMode!=TimeMode.RANGE||t.endMinutes>t.startMinutes)&&(t.endDay==null||t.endDay>=t.startDay)&&(t.repeatMode!=RepeatMode.WEEKDAYS||t.weekdaysMask in 1..127)) }}
                val taskIds=tasks.map {it.id}.toSet();require(taskIds.size==tasks.size);val templates=decode(obj,"templates",6){r->Template(r.getLong(0),r.getString(1),r.getString(2),r.getBoolean(3),r.getBoolean(4),SignalType.valueOf(r.getString(5))).also{require(it.id>0&&it.title.isNotBlank())}};require(templates.distinctBy {it.id}.size==templates.size)
                val done=decode(obj,"done",2){r->Completion(r.getLong(0),date(r.getLong(1))).also{require(it.taskId in taskIds)}};val skip=decode(obj,"skip",2){r->SkippedDay(r.getLong(0),date(r.getLong(1))).also{require(it.taskId in taskIds)}};require(done.distinct().size==done.size&&skip.distinct().size==skip.size)
                val refs=decode(obj,"photos",3){r->Triple(r.getLong(0),r.getInt(1),r.getInt(2)).also{require(it.first in taskIds&&it.second in 0 until n&&it.third>=0)}};require(refs.map{it.second}.toSet().size==n);require(refs.distinctBy{it.first to it.second}.size==refs.size)
                var left=TOTAL-meta.size
                for(index in 0 until n){ val entry=requireNotNull(z.getEntry(name(index)));require(entry.size in 1..minOf(left,FILE_LIMIT));val f=File(box.photoStore.dir,"p_${UUID.randomUUID()}.jpg");require(f.createNewFile());staged+=f;z.getInputStream(entry).use { input->f.outputStream().use { out->require(copy(input,out,entry.size,entry.crc)==entry.size) } };left-=entry.size }
                val photos=refs.map{ Photo(taskId=it.first,fileName=staged[it.second].name,position=it.third) };val old=box.db.withTransaction { val names=dao.getAllPhotos().map{it.fileName}.distinct();dao.deleteAllTasks();dao.deleteAllTemplates();dao.insertTasks(tasks);dao.insertTemplates(templates);dao.insertCompletions(done);dao.insertSkippedList(skip);dao.insertPhotos(photos);names };committed=true;box.notifyChanged();old.forEach{box.photoStore.delete(it)}
            };Unit
        } finally { if(!committed) staged.forEach{it.delete()};tmp.delete() }
    }
}
