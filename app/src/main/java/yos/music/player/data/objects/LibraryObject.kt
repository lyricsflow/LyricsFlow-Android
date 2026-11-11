package com.lyricsflow.player.data.objects

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.lyricsflow.player.data.libraries.Folder
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem

@Stable
object LibraryObject {
    @Stable
    private val targetAlbumName = mutableStateOf("")
    fun setTargetAlbumName(name: String) {
        targetAlbumName.value = name
    }

    fun getTargetAlbumName(): String {
        return targetAlbumName.value
    }

    @Stable
    private val targetArtistName = mutableStateOf("")

    fun setTargetArtistName(name: String) {
        targetArtistName.value = name
    }

    fun getTargetArtistName(): String {
        return targetArtistName.value
    }

    @Stable
    private val targetArtistSongsSearchOnOpen = mutableStateOf(false)

    fun setArtistSongsSearchOnOpen(searchOnOpen: Boolean) {
        targetArtistSongsSearchOnOpen.value = searchOnOpen
    }

    fun consumeArtistSongsSearchOnOpen(): Boolean {
        val shouldSearchOnOpen = targetArtistSongsSearchOnOpen.value
        targetArtistSongsSearchOnOpen.value = false
        return shouldSearchOnOpen
    }

    @Stable
    private val targetList: MutableState<List<LyricsflowMediaItem>> = mutableStateOf(emptyList())
    @Stable
    private val targetListTitle = mutableStateOf("")

    /**
     * Identifier of the playlist whose contents are being shown in
     * [com.lyricsflow.player.ui.pages.library.NormalMusic], or null if the
     * page is showing some other list (Songs, Album, Artist, Folder).
     *
     * Set this alongside [setTargetListWithTitle] when navigating from
     * a playlist so the detail page can light up playlist-only actions
     * (Edit, Pin, Add-this-to-playlist, Play Next, Delete) without
     * needing to rediscover which playlist it's rendering. Per PRD §5.2.
     */
    @Stable
    val targetPlayListId: MutableState<String?> = mutableStateOf(null)

    fun setTargetListWithTitle(title: String, list: List<LyricsflowMediaItem>, playListId: String? = null) {
        targetListTitle.value = title
        targetList.value = list
        targetPlayListId.value = playListId
    }
    fun getTargetListWithTitle(): Pair<String, List<LyricsflowMediaItem>> {
        return Pair(targetListTitle.value, targetList.value)
    }

    @Stable
    val targetOnlineAlbumId = mutableStateOf("")
    @Stable
    val targetOnlineAlbumTitle = mutableStateOf("")
    @Stable
    val targetOnlineAlbumArtist = mutableStateOf("")
    @Stable
    val targetOnlineAlbumArtwork = mutableStateOf("")

    fun openOnlineAlbum(
        albumId: String,
        title: String = "",
        artist: String = "",
        artworkUrl: String = ""
    ) {
        targetOnlineAlbumId.value = albumId
        targetOnlineAlbumTitle.value = title
        targetOnlineAlbumArtist.value = artist
        targetOnlineAlbumArtwork.value = artworkUrl
    }

    @Stable
    val targetOnlineArtistId = mutableStateOf("")
    @Stable
    val targetOnlineArtistName = mutableStateOf("")
    @Stable
    val targetOnlineArtistArtwork = mutableStateOf("")

    fun openOnlineArtist(
        artistId: String,
        name: String = "",
        artworkUrl: String = ""
    ) {
        targetOnlineArtistId.value = artistId
        targetOnlineArtistName.value = name
        targetOnlineArtistArtwork.value = artworkUrl
    }

    @Stable
    val targetOnlinePlaylistId = mutableStateOf("")
    @Stable
    val targetOnlinePlaylistName = mutableStateOf("")
    @Stable
    val targetOnlinePlaylistArtwork = mutableStateOf("")

    fun openOnlinePlaylist(
        playlistId: String,
        name: String = "",
        artworkUrl: String = ""
    ) {
        targetOnlinePlaylistId.value = playlistId
        targetOnlinePlaylistName.value = name
        targetOnlinePlaylistArtwork.value = artworkUrl
    }
}
