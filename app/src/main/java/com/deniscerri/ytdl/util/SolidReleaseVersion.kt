package com.deniscerri.ytdl.util

/** Mirrors the fixed CI numbering in app/build.gradle; ABI offsets occupy the last two digits. */
object SolidReleaseVersion {
    fun apkVersionCode(tag: String): Long? {
        val match = Regex("""^v\d+\.\d+\.\d+-solid\.(\d+)\.(\d+)$""").matchEntire(tag)
            ?: return null
        val run = match.groupValues[1].toLongOrNull() ?: return null
        val attempt = match.groupValues[2].toLongOrNull() ?: return null
        if (run !in 0..189999L || attempt !in 0..99L) return null
        val base = 2000000L + run * 100 + attempt
        if (base > 20999999L) return null
        return base * 100
    }
}
