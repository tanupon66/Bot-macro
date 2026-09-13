
package com.example.onestatestaging

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.onestatestaging.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val REQ_CAPTURE = 4001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        loadUi()

        b.btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else b.txtStatus.text = "Status: overlay permission OK"
        }
        b.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        b.btnCapture.setOnClickListener {
            saveUi()
            val m = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(m.createScreenCaptureIntent(), REQ_CAPTURE)
        }
        b.btnStopService.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            b.txtStatus.text = "Status: stopped"
        }
    }

    @Deprecated("Deprecated in Android API, kept for minSdk compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            saveUi()
            val i = Intent(this, OverlayService::class.java)
                .putExtra("resultCode", resultCode)
                .putExtra("projectionData", data)
            startForegroundService(i)
            b.txtStatus.text = "Status: capture service started"
        } else if (requestCode == REQ_CAPTURE) {
            b.txtStatus.text = "Status: screen capture permission denied"
        }
    }

    private fun num(s: String, d: Int) = s.toIntOrNull() ?: d

    private fun saveUi() {
        getSharedPreferences("cfg2", MODE_PRIVATE).edit()
            .putBoolean("master", b.swMaster.isChecked)
            .putBoolean("detect", b.swDetect.isChecked)
            .putBoolean("move", b.swMove.isChecked)
            .putBoolean("autoContinue", b.swAutoContinue.isChecked)
            .putBoolean("stopOneRound", b.swStopOneRound.isChecked)
            .putBoolean("dryRun", b.swDryRun.isChecked)
            .putLong("waitMs", num(b.edWait.text.toString(),3000).toLong())
            .putLong("intervalMs", num(b.edInterval.text.toString(),350).toLong())
            .putInt("deadPct", num(b.edDeadPct.text.toString(),7))
            .putInt("arrivalYPct", num(b.edArrivalY.text.toString(),67))
            .putLong("lostMs", num(b.edLost.text.toString(),2500).toLong())
            .putInt("joyXPct", num(b.edJoyX.text.toString(),12))
            .putInt("joyYPct", num(b.edJoyY.text.toString(),82))
            .putInt("joyRPct", num(b.edJoyR.text.toString(),8))
            .putInt("contXPct", num(b.edContX.text.toString(),21))
            .putInt("contYPct", num(b.edContY.text.toString(),35))
            .apply()
    }

    private fun loadUi() {
        val c = AppConfig.load(this)
        b.swMaster.isChecked=c.master; b.swDetect.isChecked=c.detect; b.swMove.isChecked=c.move
        b.swAutoContinue.isChecked=c.autoContinue; b.swStopOneRound.isChecked=c.stopOneRound
        b.swDryRun.isChecked=c.dryRun
        b.edWait.setText(c.waitMs.toString()); b.edInterval.setText(c.intervalMs.toString())
        b.edDeadPct.setText(c.deadPct.toString()); b.edArrivalY.setText(c.arrivalYPct.toString())
        b.edLost.setText(c.lostMs.toString())
        b.edJoyX.setText(c.joyXPct.toString()); b.edJoyY.setText(c.joyYPct.toString())
        b.edJoyR.setText(c.joyRPct.toString())
        b.edContX.setText(c.contXPct.toString()); b.edContY.setText(c.contYPct.toString())
    }
}
