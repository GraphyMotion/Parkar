package com.carparking.app.utils

/**
 * Formate une durée écoulée depuis [parkedAtMs] jusqu'à [nowMs] en texte lisible.
 * @param withPrefix si true, préfixe par "il y a " (HomeScreen) ; sinon texte nu (widget).
 */
fun formatElapsedDuration(parkedAtMs: Long, nowMs: Long = System.currentTimeMillis(), withPrefix: Boolean = true): String {
    val diff = nowMs - parkedAtMs
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val prefix = if (withPrefix) "il y a " else ""
    return when {
        days > 0 -> "$prefix${days}j ${hours % 24}h"
        hours > 0 -> "$prefix${hours}h${if (withPrefix) " " else ""}${minutes % 60}min"
        minutes > 0 -> "$prefix${minutes}min"
        else -> if (withPrefix) "à l'instant" else "à l'instant"
    }
}

/**
 * Formate une durée de rappel (en minutes) pour l'affichage des réglages.
 * 0 signifie « pas de rappel ».
 */
fun formatReminderDuration(minutes: Long): String {
    if (minutes <= 0) return "Aucun rappel"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "${m}min"
        m == 0L -> "${h}h"
        else -> "${h}h${m}min"
    }
}
