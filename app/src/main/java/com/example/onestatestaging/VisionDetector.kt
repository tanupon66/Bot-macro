
package com.example.onestatestaging

import android.graphics.Bitmap
import kotlin.math.*

data class Target(val x:Int, val y:Int, val score:Float, val purpleNear:Float)
data class ContinueButton(val x:Int, val y:Int, val score:Float)

class VisionDetector {
    private fun rgb(c:Int): IntArray = intArrayOf((c shr 16) and 255,(c shr 8) and 255,c and 255)

    private fun isRed(c:Int):Boolean {
        val (r,g,b)=rgb(c)
        return r > 175 && g < 115 && b < 105 && r > g*1.55
    }
    private fun isPurple(c:Int):Boolean {
        val (r,g,b)=rgb(c)
        return r > 135 && b > 120 && g < 150 && (r+b) > g*2.05
    }
    private fun isYellow(c:Int):Boolean {
        val (r,g,b)=rgb(c)
        return r > 180 && g > 135 && b < 95 && r > b*2.0
    }

    fun detectTarget(bm:Bitmap):Target? {
        val w=bm.width; val h=bm.height
        // Ignore top-left minimap and top HUD. Search actual world area.
        val x0=(w*0.20).toInt(); val y0=(h*0.12).toInt()
        val x1=(w*0.98).toInt(); val y1=(h*0.94).toInt()
        val step=max(2,w/740)

        var sx=0L; var sy=0L; var n=0
        for (y in y0 until y1 step step) for (x in x0 until x1 step step) {
            if (isRed(bm.getPixel(x,y))) { sx+=x; sy+=y; n++ }
        }
        if (n < 8) return null
        val cx=(sx/n).toInt(); val cy=(sy/n).toInt()

        // Reject giant red areas; target pin is compact.
        val density=(n.toFloat()*step*step)/(w*h).toFloat()
        if (density > 0.012f) return null

        val rx=(w*0.07).toInt(); val ry=(h*0.08).toInt()
        var purple=0; var total=0
        val px0=max(x0,cx-rx); val px1=min(x1,cx+rx)
        val py0=max(y0,cy-ry); val py1=min(y1,cy+ry)
        for(y in py0 until py1 step step) for(x in px0 until px1 step step) {
            total++; if(isPurple(bm.getPixel(x,y))) purple++
        }
        val pp=if(total==0) 0f else purple.toFloat()/total.toFloat()
        val score=min(1f, n.toFloat()/80f)
        return Target(cx,cy,score,pp)
    }

    fun detectContinueButton(bm:Bitmap):ContinueButton? {
        val w=bm.width; val h=bm.height
        // Screenshot shows yellow continue button in left/mid part.
        val x0=(w*0.05).toInt(); val x1=(w*0.45).toInt()
        val y0=(h*0.22).toInt(); val y1=(h*0.55).toInt()
        val step=max(2,w/740)

        var sx=0L; var sy=0L; var n=0
        for(y in y0 until y1 step step) for(x in x0 until x1 step step) {
            if(isYellow(bm.getPixel(x,y))) { sx+=x; sy+=y; n++ }
        }
        if(n < 60) return null
        val cx=(sx/n).toInt(); val cy=(sy/n).toInt()
        return ContinueButton(cx,cy,min(1f,n.toFloat()/500f))
    }
}
