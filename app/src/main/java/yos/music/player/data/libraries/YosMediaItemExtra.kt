package com.lyricsflow.player.data.libraries

val LyricsflowMediaItem.artistsList: List<String>?
    get() = this.artists?.toMultipleArtists()

val LyricsflowMediaItem.artistsName: String?
    get() = this.artistsList?.toArtistsString()