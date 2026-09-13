
package com.example.onestatestaging

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class OverlayService:Service(){
    private lateinit var wm:WindowManager
    private var panel:View?=null
    private var projection:MediaProjection?=null
    private var vd:VirtualDisplay?=null
    private var reader:ImageReader?=null
    private var sw=0; private var sh=0
    private val detector=VisionDetector()
    private lateinit var engine:AutomationEngine
    private val busy=AtomicBoolean(false)
    private val ui=Handler(Looper.getMainLooper())
    private var cfg:AppConfig?=null
    private var logFile:File?=null

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        startForeground(17, notification())
        cfg=AppConfig.load(this)
        engine=AutomationEngine(cfg!!,{sw},{sh}){msg->status(msg)}
        if(panel==null) showOverlay()

        val code=intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED)?:Activity.RESULT_CANCELED
        val data=if(Build.VERSION.SDK_INT>=33)
            intent?.getParcelableExtra("projectionData",Intent::class.java)
        else @Suppress("DEPRECATION") intent?.getParcelableExtra("projectionData")
        if(code==Activity.RESULT_OK && data!=null) startCapture(code,data)
        return START_NOT_STICKY
    }

    private fun startCapture(code:Int,data:Intent){
        val dm=resources.displayMetrics
        sw=dm.widthPixels; sh=dm.heightPixels
        val m=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection=m.getMediaProjection(code,data)
        reader=ImageReader.newInstance(sw,sh,PixelFormat.RGBA_8888,2)
        vd=projection?.createVirtualDisplay(
            "StagingVision",sw,sh,dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface,null,null
        )
        reader!!.setOnImageAvailableListener({ r ->
            if(busy.getAndSet(true)){ r.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            val image=r.acquireLatestImage()
            if(image==null){busy.set(false);return@setOnImageAvailableListener}
            try{
                val plane=image.planes[0]
                val buf=plane.buffer
                val ps=plane.pixelStride
                val rs=plane.rowStride
                val pad=rs-ps*sw
                val tmp=Bitmap.createBitmap(sw+pad/ps,sh,Bitmap.Config.ARGB_8888)
                tmp.copyPixelsFromBuffer(buf)
                val bm=Bitmap.createBitmap(tmp,0,0,sw,sh)
                tmp.recycle()

                val c=AppConfig.load(this)
                cfg=c; engine.updateConfig(c)
                if(c.detect){
                    val t=detector.detectTarget(bm)
                    val pop=detector.detectContinueButton(bm)
                    engine.onFrame(t,pop)
                }
                bm.recycle()
            }catch(e:Throwable){ status("Vision error: ${e.javaClass.simpleName}") }
            finally{ image.close(); busy.set(false) }
        },Handler(Looper.getMainLooper()))
        status("CAPTURE READY ${sw}x${sh}")
    }

    private fun showOverlay(){
        if(!Settings.canDrawOverlays(this)){status("Overlay permission missing");return}
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; setPadding(14,10,14,10); setBackgroundColor(0xD9222326.toInt())
        }
        val head=TextView(this).apply{text="STAGING AUTO";textSize=13f}
        val st=TextView(this).apply{id=android.R.id.text1;text="Idle";textSize=11f}
        val r1=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val on=Button(this).apply{text="AUTO ON"}
        val pause=Button(this).apply{text="PAUSE"}
        val off=Button(this).apply{text="STOP"}
        r1.addView(on);r1.addView(pause);r1.addView(off)
        val r2=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        val dry=Switch(this).apply{text="DRY";isChecked=AppConfig.load(this@OverlayService).dryRun}
        val cont=Switch(this).apply{text="CONT";isChecked=AppConfig.load(this@OverlayService).autoContinue}
        r2.addView(dry);r2.addView(cont)
        box.addView(head);box.addView(st);box.addView(r1);box.addView(r2)

        val p=WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT
        )
        p.gravity=Gravity.TOP or Gravity.START;p.x=20;p.y=230
        wm.addView(box,p);panel=box

        var sx=0f;var sy=0f;var px=0;var py=0
        head.setOnTouchListener{_,e->
            when(e.action){
                MotionEvent.ACTION_DOWN->{sx=e.rawX;sy=e.rawY;px=p.x;py=p.y;true}
                MotionEvent.ACTION_MOVE->{p.x=px+(e.rawX-sx).toInt();p.y=py+(e.rawY-sy).toInt();wm.updateViewLayout(box,p);true}
                else->false
            }
        }
        on.setOnClickListener{
            patchPrefs(master=true,dry=dry.isChecked,cont=cont.isChecked)
            engine.updateConfig(AppConfig.load(this));engine.start()
        }
        pause.setOnClickListener{engine.pause()}
        off.setOnClickListener{patchPrefs(master=false,dry=dry.isChecked,cont=cont.isChecked);engine.stop()}
        dry.setOnCheckedChangeListener{_,v->patchPrefs(dry=v,cont=cont.isChecked)}
        cont.setOnCheckedChangeListener{_,v->patchPrefs(dry=dry.isChecked,cont=v)}
    }

    private fun patchPrefs(master:Boolean?=null,dry:Boolean?=null,cont:Boolean?=null){
        val e=getSharedPreferences("cfg2",MODE_PRIVATE).edit()
        master?.let{e.putBoolean("master",it)}
        dry?.let{e.putBoolean("dryRun",it)}
        cont?.let{e.putBoolean("autoContinue",it)}
        e.apply()
        if(::engine.isInitialized) engine.updateConfig(AppConfig.load(this))
    }

    private fun status(msg:String){
        ui.post{panel?.findViewById<TextView>(android.R.id.text1)?.text=msg}
        try{
            if(logFile==null){
                val dir=File(filesDir,"logs").apply{mkdirs()}
                logFile=File(dir,"run_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date())+".txt")
            }
            logFile?.appendText("${System.currentTimeMillis()} $msg\n")
        }catch(_:Throwable){}
    }

    private fun notification():Notification{
        val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(
            NotificationChannel("stageauto","Staging Automation",NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this,"stageauto")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Staging automation tester")
            .setContentText("Screen detector active").build()
    }

    override fun onDestroy(){
        try{reader?.close()}catch(_:Throwable){}
        try{vd?.release()}catch(_:Throwable){}
        try{projection?.stop()}catch(_:Throwable){}
        try{panel?.let{wm.removeView(it)}}catch(_:Throwable){}
        if(::engine.isInitialized)engine.stop()
        super.onDestroy()
    }
    override fun onBind(i:Intent?)=null
}
