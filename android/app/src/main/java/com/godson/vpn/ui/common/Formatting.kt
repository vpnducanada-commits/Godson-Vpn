package com.godson.vpn.ui.common

import java.util.Locale

/** Formatage partagé (durées, volumes). Aucune valeur inventée : les champs absents restent « — ». */
object Formatting {

    fun duration(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            String.format(Locale.FRANCE, "%d:%02d:%02d", h, m, sec)
        } else {
            String.format(Locale.FRANCE, "%d:%02d", m, sec)
        }
    }

    /** Date lisible pour l'historique de session (donnée réelle de l'appareil). */
    fun shortDateTime(epochMillis: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
        return String.format(
            Locale.FRANCE, "%02d/%02d %02d:%02d",
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE)
        )
    }
}
