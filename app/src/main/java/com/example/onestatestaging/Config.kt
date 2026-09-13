
package com.example.onestatestaging

import android.content.Context

data class AppConfig(
    val master: Boolean,
    val detect: Boolean,
    val move: Boolean,
    val autoContinue: Boolean,
    val stopOneRound: Boolean,
    val dryRun: Boolean,
    val waitMs: Long,
    val intervalMs: Long,
    val deadPct: Int,
    val arrivalYPct: Int,
    val lostMs: Long,
    val joyXPct: Int,
    val joyYPct: Int,
    val joyRPct: Int,
    val contXPct: Int,
    val contYPct: Int
) {
    companion object {
        fun load(c: Context): AppConfig {
            val p = c.getSharedPreferences("cfg2", Context.MODE_PRIVATE)
            return AppConfig(
                p.getBoolean("master", false),
                p.getBoolean("detect", true),
                p.getBoolean("move", true),
                p.getBoolean("autoContinue", false),
                p.getBoolean("stopOneRound", false),
                p.getBoolean("dryRun", true),
                p.getLong("waitMs", 3000L),
                p.getLong("intervalMs", 350L),
                p.getInt("deadPct", 7),
                p.getInt("arrivalYPct", 67),
                p.getLong("lostMs", 2500L),
                p.getInt("joyXPct", 12),
                p.getInt("joyYPct", 82),
                p.getInt("joyRPct", 8),
                p.getInt("contXPct", 21),
                p.getInt("contYPct", 35)
            )
        }
    }
}
