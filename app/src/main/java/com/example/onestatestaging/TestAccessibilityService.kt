
package com.example.onestatestaging

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class TestAccessibilityService : AccessibilityService() {
    companion object { @Volatile var instance: TestAccessibilityService? = null }
    override fun onServiceConnected() { instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun tap(x: Float, y: Float, durationMs: Long = 70) {
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, durationMs)).build()
        dispatchGesture(g, null, null)
    }

    fun swipeHold(x1:Float,y1:Float,x2:Float,y2:Float,durationMs:Long=300) {
        val p = Path().apply { moveTo(x1,y1); lineTo(x2,y2) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p,0,durationMs)).build()
        dispatchGesture(g,null,null)
    }
}
