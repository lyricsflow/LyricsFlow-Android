package com.lyricsflow.player.data.libraries

val YosMediaItem.artistsList: List<String>?
    get() = this.artists?.toMultipleArtists()

val YosMediaItem.artistsName: String?
    get() = this.artistsList?.toArtistsString()