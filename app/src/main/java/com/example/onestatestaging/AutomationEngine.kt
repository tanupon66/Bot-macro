
package com.example.onestatestaging

import android.os.Handler
import android.os.Looper
import kotlin.math.*

enum class AutoState { OFF, SEARCHING, MOVING, ARRIVAL_WAIT, WAIT_NEXT, CONTINUE, PAUSED, ERROR }

class AutomationEngine(
    private var cfg: AppConfig,
    private val screenW:()->Int,
    private val screenH:()->Int,
    private val log:(String)->Unit
) {
    private val h=Handler(Looper.getMainLooper())
    private var state=AutoState.OFF
    private var lastTarget=0L
    private var rounds=0

    fun updateConfig(c:AppConfig){ cfg=c }
    fun start(){ state=AutoState.SEARCHING; lastTarget=System.currentTimeMillis(); log("SEARCHING") }
    fun pause(){ state=AutoState.PAUSED; h.removeCallbacksAndMessages(null); log("PAUSED") }
    fun stop(){ state=AutoState.OFF; h.removeCallbacksAndMessages(null); log("STOPPED") }
    fun state()=state

    fun onFrame(target:Target?, cont:ContinueButton?) {
        if(!cfg.master || state==AutoState.OFF || state==AutoState.PAUSED) return
        val now=System.currentTimeMillis()

        if(cont!=null && cont.score>0.2f) {
            if(cfg.autoContinue) {
                state=AutoState.CONTINUE
                val x=if(cont.x>0) cont.x else screenW()*cfg.contXPct/100
                val y=if(cont.y>0) cont.y else screenH()*cfg.contYPct/100
                log("CONTINUE popup ${if(cfg.dryRun)"[DRY]":""}")
                if(!cfg.dryRun) TestAccessibilityService.instance?.tap(x.toFloat(),y.toFloat())
                rounds++
                if(cfg.stopOneRound) { stop(); return }
                h.postDelayed({ if(state!=AutoState.OFF){state=AutoState.SEARCHING;log("SEARCHING round ${rounds+1}")}},1000)
            } else {
                state=AutoState.PAUSED
                log("Popup found • Auto Continue OFF")
            }
            return
        }

        if(target==null) {
            if(now-lastTarget > cfg.lostMs && state!=AutoState.ARRIVAL_WAIT) {
                state=AutoState.SEARCHING
                log("TARGET LOST • waiting")
            }
            return
        }
        lastTarget=now

        val w=screenW(); val hgt=screenH()
        val dead=w*cfg.deadPct/100
        val arrivalY=hgt*cfg.arrivalYPct/100

        // Require marker low enough + some purple around it to reduce false arrival.
        val arrived = target.y >= arrivalY && target.purpleNear >= 0.018f
        if(arrived) {
            if(state!=AutoState.ARRIVAL_WAIT && state!=AutoState.WAIT_NEXT) {
                state=AutoState.ARRIVAL_WAIT
                log("ARRIVED • wait ${cfg.waitMs}ms")
                h.removeCallbacksAndMessages(null)
                h.postDelayed({
                    if(state==AutoState.ARRIVAL_WAIT){
                        state=AutoState.WAIT_NEXT
                        log("WAIT NEXT TARGET")
                    }
                },cfg.waitMs)
            }
            return
        }

        if(!cfg.move) { state=AutoState.SEARCHING; log("TARGET ${target.x},${target.y} • Move OFF"); return }

        state=AutoState.MOVING
        val dx=(target.x-w/2).toFloat()
        val nx=(dx/(w/2f)).coerceIn(-1f,1f)
        val joyX=w*cfg.joyXPct/100f
        val joyY=hgt*cfg.joyYPct/100f
        val r=w*cfg.joyRPct/100f

        // Forward with proportional left/right steering.
        var tx=joyX + nx*r
        if(abs(dx)<dead) tx=joyX
        val ty=joyY-r
        log("MOVE x=${target.x} y=${target.y} purple=${"%.3f".format(target.purpleNear)} ${if(cfg.dryRun)"[DRY]":""}")
        if(!cfg.dryRun) TestAccessibilityService.instance?.swipeHold(joyX,joyY,tx,ty,cfg.intervalMs)
    }
}
