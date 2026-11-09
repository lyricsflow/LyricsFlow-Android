package com.lyricsflow.player.ui.pages.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lyricsflow.player.R
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.code.utils.lrc.LyricsFlowCatalogItem
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.MusicLibrary.songs
import com.lyricsflow.player.data.libraries.PlayListLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.artistsName
import com.lyricsflow.player.data.libraries.defaultAlbum
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.ProfileButton
import com.lyricsflow.player.ui.widgets.basic.Title
import com.lyricsflow.player.ui.widgets.basic.YosWrapper
import com.lyricsflow.player.ui.widgets.glass.LiquidGlassCircleButton
import com.lyricsflow.player.ui.widgets.glass.iosPressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class LibraryPinGridEntry(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artworkModel: Any? = null,
    val isArtistCircle: Boolean = false,
    val onClick: () -> Unit
)

private data class LibraryRecentlyAddedEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkModel: Any?,
    val onClick: () -> Unit
)

@Composable
fun Library(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.56f)
    val placeholderBg = Color(0x10000000) withNight Color(0x18FFFFFF)

    val allSongs = songs
    val localAlbums = remember(allSongs) { MusicLibrary.albums }
    val localArtists = remember(allSongs) { MusicLibrary.artists }
    val localPlaylists = PlayListLibrary.playList

    val artistAvatars = remember { mutableStateMapOf<String, String>() }
    var onlineShelves by remember { mutableStateOf<List<LyricsFlowCatalogItem>>(emptyList()) }
    var onlineTrending by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }

    // Resolve actual artist portraits from Apple Music (`fetchArtistAvatarUrl`) & editorial fallback items
    LaunchedEffect(localArtists) {
        val candidateArtists = buildList {
            addAll(SettingsLibrary.followedArtists)
            addAll(localArtists.take(6))
        }.distinct().take(6)

        candidateArtists.forEach { artistName ->
            if (artistName.isNotBlank() && artistAvatars[artistName].isNullOrBlank()) {
                launch(Dispatchers.IO) {
                    val avatar = LyricsFlowApi.fetchArtistAvatarUrl(artistName)
                    if (avatar.isNotBlank()) {
                        artistAvatars[artistName] = avatar
                    }
                }
            }
        }

        if (localAlbums.size < 6) {
            launch(Dispatchers.IO) {
                val editorial = LyricsFlowApi.fetchEditorialNewShelves().flatMap { it.items }
                if (editorial.isNotEmpty()) {
                    onlineShelves = editorial
                }
                val trending = LyricsFlowApi.fetchTrendingTracks(limit = 16)
                if (trending.isNotEmpty()) {
                    onlineTrending = trending
                    trending.take(3).forEach { tr ->
                        if (tr.artist.isNotBlank() && artistAvatars[tr.artist].isNullOrBlank()) {
                            val av = LyricsFlowApi.fetchArtistAvatarUrl(tr.artist, tr.artistId)
                            if (av.isNotBlank()) artistAvatars[tr.artist] = av
                        }
                    }
                }
            }
        }
    }

    // Build the iOS 27 Top 3x2 Pinned / Quick Access Grid (Screenshot 1: Mix of Albums, Playlists, and Circular Artists)
    val pinnedGridEntries = remember(
        localPlaylists,
        localAlbums,
        localArtists,
        artistAvatars.toMap(),
        onlineShelves,
        onlineTrending
    ) {
        val list = mutableListOf<LibraryPinGridEntry>()

        // 1. Pinned or top local playlists
        localPlaylists.sortedByDescending { it.isPinned }.take(2).forEach { pl ->
            val firstSongThumb = allSongs.firstOrNull { it.uri in pl.songDataList }?.thumb
            list.add(
                LibraryPinGridEntry(
                    id = "pl_${pl.listID}",
                    title = pl.name,
                    artworkModel = pl.coverUri ?: firstSongThumb,
                    isArtistCircle = false,
                    onClick = {
                        Vibrator.click(context)
                        val plSongs = pl.songDataList.mapNotNull { uri -> allSongs.firstOrNull { it.uri == uri } }
                        LibraryObject.setTargetListWithTitle(pl.name, plSongs, pl.listID)
                        navController.toUI(UI.NormalMusic)
                    }
                )
            )
        }

        // 2. Top local albums
        localAlbums.take(2).forEach { albName ->
            val thumb = MusicLibrary.Album[albName].firstOrNull()?.thumb
            list.add(
                LibraryPinGridEntry(
                    id = "alb_$albName",
                    title = albName,
                    artworkModel = thumb,
                    isArtistCircle = false,
                    onClick = {
                        Vibrator.click(context)
                        LibraryObject.setTargetAlbumName(albName)
                        navController.toUI(UI.AlbumInfo)
                    }
                )
            )
        }

        // 3. Featured Artist in a Circle (like `MIKE` in Screenshot 1!) using real artist photo
        val featuredArtistName = SettingsLibrary.followedArtists.firstOrNull()
            ?: localArtists.firstOrNull()
            ?: onlineTrending.firstOrNull()?.artist
        if (!featuredArtistName.isNullOrBlank()) {
            val realPhoto = artistAvatars[featuredArtistName]
                ?: MusicLibrary.Artist[featuredArtistName].firstOrNull()?.thumb
            list.add(
                LibraryPinGridEntry(
                    id = "art_$featuredArtistName",
                    title = featuredArtistName,
                    artworkModel = realPhoto,
                    isArtistCircle = true,
                    onClick = {
                        Vibrator.click(context)
                        LibraryObject.openOnlineArtist(
                            artistId = "",
                            name = featuredArtistName,
                            artworkUrl = artistAvatars[featuredArtistName].orEmpty()
                        )
                        navController.toUI(UI.OnlineArtistInfo)
                    }
                )
            )
        }

        // 4. Fill remaining slots up to 6 from remaining local albums or online editorial/trending items
        localAlbums.drop(2).forEach { albName ->
            if (list.size < 6) {
                val thumb = MusicLibrary.Album[albName].firstOrNull()?.thumb
                list.add(
                    LibraryPinGridEntry(
                        id = "alb_more_$albName",
                        title = albName,
                        artworkModel = thumb,
                        isArtistCircle = false,
                        onClick = {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(albName)
                            navController.toUI(UI.AlbumInfo)
                        }
                    )
                )
            }
        }

        onlineShelves.forEach { item ->
            if (list.size < 6 && list.none { it.title == item.title }) {
                list.add(
                    LibraryPinGridEntry(
                        id = "online_${item.id}",
                        title = item.title,
                        artworkModel = item.artworkUrl,
                        isArtistCircle = item.type == "artists",
                        onClick = {
                            Vibrator.click(context)
                            when (item.type) {
                                "playlists" -> {
                                    LibraryObject.openOnlinePlaylist(item.id, item.title, item.artworkUrl)
                                    navController.toUI(UI.OnlinePlaylistInfo)
                                }
                                "artists" -> {
                                    LibraryObject.openOnlineArtist(item.id, item.title, item.artworkUrl)
                                    navController.toUI(UI.OnlineArtistInfo)
                                }
                                else -> {
                                    LibraryObject.openOnlineAlbum(item.id, item.title, item.subtitle, item.artworkUrl)
                                    navController.toUI(UI.OnlineAlbumInfo)
                                }
                            }
                        }
                    )
                )
            }
        }

        onlineTrending.forEach { track ->
            if (list.size < 6 && list.none { it.title == track.album.ifBlank { track.title } }) {
                list.add(
                    LibraryPinGridEntry(
                        id = "trend_pin_${track.id}",
                        title = track.album.ifBlank { track.title },
                        artworkModel = track.artworkUrl,
                        isArtistCircle = false,
                        onClick = {
                            Vibrator.click(context)
                            if (track.albumId.isNotBlank()) {
                                LibraryObject.openOnlineAlbum(track.albumId, track.album.ifBlank { track.title }, track.artist, track.artworkUrl)
                                navController.toUI(UI.OnlineAlbumInfo)
                            } else {
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(track, onlineTrending)
                                }
                            }
                        }
                    )
                )
            }
        }

        // Ensure index 2 (top-right slot of row 1) is an Artist Circle if one exists, matching Screenshot 1
        val circleIdx = list.indexOfFirst { it.isArtistCircle }
        if (circleIdx >= 0 && circleIdx != 2 && list.size >= 3) {
            val circleItem = list.removeAt(circleIdx)
            list.add(2, circleItem)
        }

        list.take(6)
    }

    // Build Recently Added 2-Column Grid (Screenshot 1 bottom section)
    val recentlyAddedEntries = remember(allSongs, onlineShelves, onlineTrending) {
        val list = mutableListOf<LibraryRecentlyAddedEntry>()

        // Local songs/albums sorted by most recent addDate
        allSongs
            .sortedByDescending { it.addDate ?: 0L }
            .distinctBy { it.album ?: it.title }
            .take(12)
            .forEach { song ->
                val albName = song.album ?: defaultAlbum
                list.add(
                    LibraryRecentlyAddedEntry(
                        id = "local_rec_${song.mediaId ?: albName}",
                        title = albName.ifBlank { song.title.orEmpty() },
                        subtitle = song.artistsName,
                        artworkModel = song.thumb,
                        onClick = {
                            Vibrator.click(context)
                            LibraryObject.setTargetAlbumName(albName)
                            navController.toUI(UI.AlbumInfo)
                        }
                    )
                )
            }

        // Supplement with online albums/tracks so Recently Added is always populated
        if (list.size < 8) {
            onlineShelves.filter { it.type == "albums" }.take(8).forEach { item ->
                if (list.none { it.title == item.title }) {
                    list.add(
                        LibraryRecentlyAddedEntry(
                            id = "on_rec_${item.id}",
                            title = item.title,
                            subtitle = item.subtitle,
                            artworkModel = item.artworkUrl,
                            onClick = {
                                Vibrator.click(context)
                                LibraryObject.openOnlineAlbum(item.id, item.title, item.subtitle, item.artworkUrl)
                                navController.toUI(UI.OnlineAlbumInfo)
                            }
                        )
                    )
                }
            }
            onlineTrending.take(8).forEach { tr ->
                if (list.size < 10 && list.none { it.title == tr.title }) {
                    list.add(
                        LibraryRecentlyAddedEntry(
                            id = "tr_rec_${tr.id}",
                            title = tr.album.ifBlank { tr.title },
                            subtitle = tr.artist,
                            artworkModel = tr.artworkUrl,
                            onClick = {
                                Vibrator.click(context)
                                if (tr.albumId.isNotBlank()) {
                                    LibraryObject.openOnlineAlbum(tr.albumId, tr.album.ifBlank { tr.title }, tr.artist, tr.artworkUrl)
                                    navController.toUI(UI.OnlineAlbumInfo)
                                } else {
                                    scope.launch(Dispatchers.IO) {
                                        MediaController.playOnlineTrack(tr, onlineTrending)
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
        list
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        Title(
            title = stringResource(id = R.string.page_library_title),
            rightIconContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // iOS 27 Top-Right Circular Liquid Glass `•••` Button (Screenshot 1)
                    LiquidGlassCircleButton(
                        onClick = {
                            Vibrator.click(context)
                            navController.toUI(UI.LocalUploadTools)
                        },
                        size = 36.dp,
                        surfaceTint = (Color(0xFFE8E8ED) withNight Color(0xFF242429)).copy(alpha = 0.65f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Library Options & TTML Studio",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    ProfileButton { navController.toUI(UI.Settings.Main) }
                }
            }
        ) {
            // 1. iOS 27 Top 3x2 Pinned / Quick Access Grid (Screenshot 1)
            if (pinnedGridEntries.isNotEmpty()) {
                item("library_pinned_grid") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        pinnedGridEntries.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowItems.forEach { entry ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .iosPressScale(pressedScale = 0.95f, onClick = entry.onClick),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val itemShape = if (entry.isArtistCircle) CircleShape else RoundedCornerShape(14.dp)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = itemShape,
                                                    ambientColor = Color.Black.copy(alpha = 0.14f),
                                                    spotColor = Color.Black.copy(alpha = 0.18f)
                                                )
                                                .clip(itemShape)
                                                .background(placeholderBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (entry.artworkModel != null && entry.artworkModel.toString().isNotBlank()) {
                                                AsyncImage(
                                                    model = entry.artworkModel,
                                                    contentDescription = entry.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (entry.isArtistCircle) Icons.Rounded.Person else Icons.Rounded.Album,
                                                    contentDescription = null,
                                                    tint = secondaryColor,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = entry.title,
                                            fontFamily = SFProDisplay,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                            color = textColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                repeat((3 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 2. Apple Music Category List Rows (Playlists, Artists, Albums, Songs)
            item("library_category_links") {
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_playlists),
                        label = stringResource(id = R.string.page_library_playlists)
                    ) {
                        navController.toUI(UI.PlayLists)
                    }
                    LibraryDivider()
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_artists),
                        label = stringResource(id = R.string.page_library_artists)
                    ) {
                        navController.toUI(UI.LocalArtists)
                    }
                    LibraryDivider()
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_album),
                        label = stringResource(id = R.string.page_library_albums)
                    ) {
                        navController.toUI(UI.LocalAlbums)
                    }
                    LibraryDivider()

                    YosWrapper {
                        val targetTitle = stringResource(id = R.string.page_library_songs)
                        val targetList = songs
                        SmallLabelItem(
                            icon = painterResource(id = R.drawable.ic_library_link_icon_songs),
                            label = targetTitle
                        ) {
                            LibraryObject.setTargetListWithTitle(targetTitle, targetList)
                            navController.toUI(UI.NormalMusic)
                        }
                    }

                    LibraryDivider()
                }
            }

            // 3. Recently Added 2-Column Grid (Screenshot 1)
            if (recentlyAddedEntries.isNotEmpty()) {
                item("library_recently_added_header") {
                    Text(
                        text = "Recently Added",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)
                    )
                }

                val rows = recentlyAddedEntries.chunked(2)
                rows.forEachIndexed { rowIdx, pair ->
                    item("library_recently_added_row_$rowIdx") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            pair.forEach { rec ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .iosPressScale(pressedScale = 0.96f, onClick = rec.onClick)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(14.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.14f)
                                            )
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(placeholderBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (rec.artworkModel != null && rec.artworkModel.toString().isNotBlank()) {
                                            AsyncImage(
                                                model = rec.artworkModel,
                                                contentDescription = rec.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Album,
                                                contentDescription = null,
                                                tint = secondaryColor,
                                                modifier = Modifier.size(42.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rec.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (rec.subtitle.isNotBlank()) {
                                        Text(
                                            text = rec.subtitle,
                                            fontFamily = SFProDisplay,
                                            fontSize = 13.sp,
                                            color = secondaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
