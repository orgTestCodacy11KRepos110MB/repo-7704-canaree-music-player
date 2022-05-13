package dev.olog.feature.lyrics.offline

import android.text.Spannable

sealed class Lyrics {
    class Normal(val lyrics: Spannable) : Lyrics()
    class Synced(val lyrics: List<Pair<Long, Spannable>>) : Lyrics()
}