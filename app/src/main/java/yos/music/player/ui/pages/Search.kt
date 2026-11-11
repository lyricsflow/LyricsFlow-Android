package com.lyricsflow.player.ui.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.insets.statusBarsPadding
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.code.utils.lrc.LyricsFlowCatalogItem
import com.lyricsflow.player.code.utils.lrc.LyricsFlowCatalogSearchResponse
import com.lyricsflow.player.code.utils.lrc.LyricsflowParsedLyricData
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.artistsName
import com.lyricsflow.player.data.libraries.defaultAlbum
import com.lyricsflow.player.data.libraries.defaultArtistsName
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.ProfileButton
import com.lyricsflow.player.ui.widgets.basic.ShadowImageWithCache
import com.lyricsflow.player.ui.widgets.glass.LocalFloatingTabBarScrollConnection
import com.lyricsflow.player.ui.widgets.glass.liquidGlassCapsule
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BrowseCategoryItem(
    val title: String,
    val subtitle: String,
    val query: String,
    val colors: List<Color>
)

private val AppleMusicBrowseCategories = listOf(
    BrowseCategoryItem(
        title = "Pop",
        subtitle = "Hits & Essentials",
        query = "Pop Hits",
        colors = listOf(Color(0xFFFF2D55), Color(0xFFFF5E3A))
    ),
    BrowseCategoryItem(
        title = "Hip-Hop",
        subtitle = "Rap Life & Flows",
        query = "Hip Hop",
        colors = listOf(Color(0xFF5856D6), Color(0xFFAF52DE))
    ),
    BrowseCategoryItem(
        title = "R&B / Soul",
        subtitle = "Smooth Grooves",
        query = "SZA R&B",
        colors = listOf(Color(0xFF007AFF), Color(0xFF5AC8FA))
    ),
    BrowseCategoryItem(
        title = "Dance & Electronic",
        subtitle = "Club & Synth",
        query = "Electronic Dance",
        colors = listOf(Color(0xFF34C759), Color(0xFF30B0C7))
    ),
    BrowseCategoryItem(
        title = "Rock",
        subtitle = "Anthems & Classics",
        query = "Rock Classics",
        colors = listOf(Color(0xFFFF3B30), Color(0xFF8E24AA))
    ),
    BrowseCategoryItem(
        title = "Arabic",
        subtitle = "Tarab & Pop",
        query = "Arabic Hits",
        colors = listOf(Color(0xFFD4AF37), Color(0xFF8D6E63))
    ),
    BrowseCategoryItem(
        title = "Latin",
        subtitle = "Urbano & Reggaeton",
        query = "Latin Hits",
        colors = listOf(Color(0xFFFF9500), Color(0xFFFF2D55))
    ),
    BrowseCategoryItem(
        title = "Alternative & Indie",
        subtitle = "Fresh Sounds",
        query = "Indie Alternative",
        colors = listOf(Color(0xFF5E5CE6), Color(0xFF00C7BE))
    ),
    BrowseCategoryItem(
        title = "Chill & Relax",
        subtitle = "Ambient & Acoustic",
        query = "Chill Acoustic",
        colors = listOf(Color(0xFF32ADE6), Color(0xFF2E3192))
    ),
    BrowseCategoryItem(
        title = "Top Hits",
        subtitle = "Global Charts",
        query = "Top 100",
        colors = listOf(Color(0xFFFA2D48), Color(0xFF7928CA))
    ),
    BrowseCategoryItem(
        title = "Workout",
        subtitle = "Pure Energy",
        query = "Workout Phonk",
        colors = listOf(Color(0xFFFF6B00), Color(0xFFE60023))
    ),
    BrowseCategoryItem(
        title = "Jazz & Classical",
        subtitle = "Timeless Instrumental",
        query = "Jazz Classics",
        colors = listOf(Color(0xFF4A5568), Color(0xFF1A202C))
    )
)

private enum class SearchFilterTab(val label: String) {
    ALL("All"),
    TOP_RESULTS("Top Results"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    LIBRARY("In Library")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Search(navController: NavController) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(SearchFilterTab.ALL) }
    val recentSearches = SettingsLibrary.SearchHistory

    var searchRecommendations by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var catalogResponse by remember { mutableStateOf(LyricsFlowCatalogSearchResponse()) }
    var previewTrack by remember { mutableStateOf<LyricsFlowApi.CatalogTrackResult?>(null) }
    var previewLyrics by remember { mutableStateOf<LyricsflowParsedLyricData?>(null) }
    var isLoadingPreviewLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            searchRecommendations = LyricsFlowApi.fetchTrendingTracks(limit = 8)
        }
    }

    val allLocalSongs = remember(MusicLibrary.songs.size) { MusicLibrary.songs }
    val allLocalArtists = remember(allLocalSongs.size) { MusicLibrary.artists }
    val allLocalAlbumNames = remember(allLocalSongs.size) { MusicLibrary.albums }

    val localMatchingSongs = remember(query, allLocalSongs) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            allLocalSongs.filter { song ->
                song.title?.contains(q, ignoreCase = true) == true ||
                    song.artistsName?.contains(q, ignoreCase = true) == true ||
                    song.album?.contains(q, ignoreCase = true) == true
            }.take(25)
        }
    }

    val localMatchingArtists = remember(query, allLocalArtists) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            allLocalArtists.filter { artist ->
                artist.contains(q, ignoreCase = true)
            }.take(12)
        }
    }

    val localMatchingAlbums = remember(query, allLocalAlbumNames) {
        val q = query.trim()
        if (q.isEmpty()) {
            emptyList()
        } else {
            allLocalAlbumNames.mapNotNull { albumName ->
                val songs = MusicLibrary.Album[albumName]
                val matches = albumName.contains(q, ignoreCase = true) ||
                    songs.firstOrNull()?.artistsName?.contains(q, ignoreCase = true) == true
                if (matches && songs.isNotEmpty()) albumName to songs else null
            }.take(12)
        }
    }

    // 320ms debounced online search against LyricsFlow / Apple Music AMP Catalog (`performCatalogSearch` in `upload.js`)
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            catalogResponse = LyricsFlowCatalogSearchResponse()
            isSearchingOnline = false
            return@LaunchedEffect
        }
        isSearchingOnline = true
        delay(320L)
        catalogResponse = LyricsFlowApi.searchFullCatalog(trimmed, limit = 25)
        isSearchingOnline = false
    }

    val textColor = Color.Black withNight Color.White
    val secondaryTextColor = (Color.Black withNight Color.White).copy(alpha = 0.56f)
    val searchBarBg = Color(0x14000000) withNight Color(0x24FFFFFF)
    val appleAccent = Color(0xFFFA2D48)

    fun handleCatalogItemClick(item: LyricsFlowCatalogItem) {
        Vibrator.click(context)
        if (query.trim().isNotEmpty()) {
            SettingsLibrary.addSearchHistoryQuery(query.trim())
        }
        when (item.type) {
            "artists" -> {
                LibraryObject.openOnlineArtist(
                    artistId = item.id,
                    name = item.title,
                    artworkUrl = item.artworkUrl
                )
                navController.toUI(UI.OnlineArtistInfo)
            }
            "albums" -> {
                LibraryObject.openOnlineAlbum(
                    albumId = item.id,
                    title = item.title,
                    artist = item.subtitle,
                    artworkUrl = item.artworkUrl
                )
                navController.toUI(UI.OnlineAlbumInfo)
            }
            "playlists" -> {
                LibraryObject.openOnlinePlaylist(
                    playlistId = item.id,
                    name = item.title,
                    artworkUrl = item.artworkUrl
                )
                navController.toUI(UI.OnlinePlaylistInfo)
            }
            else -> {
                val track = catalogResponse.songs.firstOrNull { it.id == item.id }
                    ?: LyricsFlowApi.CatalogTrackResult(
                        id = item.id,
                        title = item.title,
                        artist = item.subtitle,
                        album = item.albumName,
                        albumId = item.albumId,
                        artistId = item.artistId,
                        artworkUrl = item.artworkUrl,
                        durationMs = item.durationMs,
                        isExplicit = item.isExplicit
                    )
                scope.launch(Dispatchers.IO) {
                    MediaController.playOnlineTrack(
                        track = track,
                        contextTracks = catalogResponse.songs.ifEmpty { listOf(track) },
                        shuffleModeEnabled = false,
                        appendSimilar = true
                    )
                }
            }
        }
    }

    val scrollConnection = LocalFloatingTabBarScrollConnection.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .let { mod -> if (scrollConnection != null) mod.nestedScroll(scrollConnection) else mod },
            contentPadding = PaddingValues(bottom = 240.dp)
        ) {
            // Apple Music Search Header
            item(key = "search_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        color = textColor
                    )
                    ProfileButton { navController.toUI(UI.Settings.Main) }
                }
            }

            if (query.trim().isEmpty()) {
                // Real Persistent Recently Searched History (Max 5 items, empty by default)
                if (recentSearches.isNotEmpty()) {
                    item(key = "recent_searches_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Searched",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = textColor
                                )
                                Text(
                                    text = "Clear",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = appleAccent,
                                    modifier = Modifier.clickable {
                                        Vibrator.click(context)
                                        SettingsLibrary.clearSearchHistory()
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recentSearches.forEach { term ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = searchBarBg,
                                        modifier = Modifier.clickable {
                                            Vibrator.click(context)
                                            query = term
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null,
                                                tint = appleAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = term,
                                                fontFamily = SFProDisplay,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = textColor
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.Clear,
                                                contentDescription = "Remove",
                                                tint = secondaryTextColor,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        Vibrator.click(context)
                                                        SettingsLibrary.removeSearchHistoryQuery(term)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Real Online Search Recommendations / Trending Suggestions
                if (searchRecommendations.isNotEmpty()) {
                    item(key = "search_trending_recommendations") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Trending Searches",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            searchRecommendations.take(5).forEach { sugTrack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Vibrator.click(context)
                                            SettingsLibrary.addSearchHistoryQuery(sugTrack.title)
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.playOnlineTrack(
                                                    track = sugTrack,
                                                    contextTracks = searchRecommendations,
                                                    shuffleModeEnabled = false,
                                                    appendSimilar = true
                                                )
                                            }
                                        }
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = sugTrack.artworkUrl,
                                        contentDescription = sugTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(searchBarBg)
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = sugTrack.title,
                                            fontFamily = SFProDisplay,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Song · ${sugTrack.artist}",
                                            fontFamily = SFProDisplay,
                                            fontSize = 12.5.sp,
                                            color = secondaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Browse Categories 2-Column Apple Music Gradient Grid
                item(key = "browse_categories_title") {
                    Text(
                        text = "Browse Categories",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp)
                    )
                }

                val chunkedCategories = AppleMusicBrowseCategories.chunked(2)
                items(chunkedCategories, key = { it.first().title }) { rowCategories ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCategories.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(104.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(cat.colors))
                                    .clickable {
                                        Vibrator.click(context)
                                        SettingsLibrary.addSearchHistoryQuery(cat.title)
                                        query = cat.query
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        text = cat.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = cat.subtitle,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
            // Active Search Filter Pills
            item(key = "search_filter_pills") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilterTab.entries.forEach { tab ->
                        val isSelected = selectedFilter == tab
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) appleAccent else searchBarBg,
                            modifier = Modifier.clickable {
                                Vibrator.click(context)
                                selectedFilter = tab
                            }
                        ) {
                            Text(
                                text = tab.label,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (isSelected) Color.White else textColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    if (isSearchingOnline) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                            color = appleAccent
                        )
                    }
                }
            }

            // 1. TOP RESULTS SECTION (Apple Music Horizontal Cards)
            val showTopResults = (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.TOP_RESULTS) &&
                catalogResponse.topResults.isNotEmpty()
            if (showTopResults) {
                item(key = "online_top_results_header") {
                    Text(
                        text = "Top Results",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                items(catalogResponse.topResults.take(4), key = { "top_res_${it.type}_${it.id}" }) { item ->
                    val typeLabel = when (item.type) {
                        "artists" -> "Artist"
                        "albums" -> "Album"
                        "playlists" -> "Playlist"
                        "music-videos" -> "Music Video"
                        else -> "Song"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleCatalogItemClick(item) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.artworkUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(if (item.type == "artists") CircleShape else RoundedCornerShape(10.dp))
                                .background(searchBarBg)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (item.isExplicit) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ExplicitBadge()
                                }
                            }
                            Text(
                                text = if (item.subtitle.isNotBlank()) "$typeLabel · ${item.subtitle}" else typeLabel,
                                fontFamily = SFProDisplay,
                                fontSize = 13.5.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (item.type == "songs") Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = if (item.type == "songs") appleAccent else secondaryTextColor
                        )
                    }
                }
            }

            // 2. ONLINE ARTISTS SECTION (Circular Avatars / Rows -> Opens OnlineArtistView)
            val showOnlineArtists = (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.ARTISTS) &&
                catalogResponse.artists.isNotEmpty()
            if (showOnlineArtists) {
                item(key = "online_artists_header") {
                    Text(
                        text = "Artists",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
                    )
                }
                val artistsToShow = if (selectedFilter == SearchFilterTab.ALL) {
                    catalogResponse.artists.take(5)
                } else {
                    catalogResponse.artists
                }
                items(artistsToShow, key = { "online_artist_${it.id}" }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                LibraryObject.openOnlineArtist(
                                    artistId = artist.id,
                                    name = artist.title,
                                    artworkUrl = artist.artworkUrl
                                )
                                navController.toUI(UI.OnlineArtistInfo)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(searchBarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (artist.artworkUrl.isNotBlank()) {
                                AsyncImage(
                                    model = artist.artworkUrl,
                                    contentDescription = artist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.Person, null, tint = secondaryTextColor)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = artist.title,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.5.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist.subtitle.ifBlank { "Artist" },
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Open Artist",
                            tint = secondaryTextColor
                        )
                    }
                }
            }

            // 3. ONLINE ALBUMS SECTION (Horizontal Shelf in ALL, List in ALBUMS -> Opens OnlineAlbumView)
            val showOnlineAlbums = (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.ALBUMS) &&
                catalogResponse.albums.isNotEmpty()
            if (showOnlineAlbums) {
                if (selectedFilter == SearchFilterTab.ALL) {
                    item(key = "online_albums_shelf") {
                        OnlineAlbumHorizontalShelf(
                            title = "Albums",
                            items = catalogResponse.albums.take(12),
                            textColor = textColor,
                            secondaryColor = secondaryTextColor,
                            onAlbumClick = { album ->
                                Vibrator.click(context)
                                LibraryObject.openOnlineAlbum(
                                    albumId = album.id,
                                    title = album.title,
                                    artist = album.subtitle,
                                    artworkUrl = album.artworkUrl
                                )
                                navController.toUI(UI.OnlineAlbumInfo)
                            }
                        )
                    }
                } else {
                    item(key = "online_albums_list_header") {
                        Text(
                            text = "Albums",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(catalogResponse.albums, key = { "online_album_row_${it.id}" }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Vibrator.click(context)
                                    LibraryObject.openOnlineAlbum(
                                        albumId = album.id,
                                        title = album.title,
                                        artist = album.subtitle,
                                        artworkUrl = album.artworkUrl
                                    )
                                    navController.toUI(UI.OnlineAlbumInfo)
                                }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = album.artworkUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(searchBarBg)
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = album.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (album.isExplicit) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ExplicitBadge()
                                    }
                                }
                                Text(
                                    text = buildString {
                                        append(album.subtitle)
                                        if (album.releaseYear.isNotBlank()) append(" · ${album.releaseYear}")
                                    },
                                    fontFamily = SFProDisplay,
                                    fontSize = 13.sp,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = secondaryTextColor)
                        }
                    }
                }
            }

            // 4. ONLINE SONGS SECTION (Streams directly via MediaController.playOnlineTrack + Lyrics preview button)
            val showOnlineSongs = (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.SONGS) &&
                catalogResponse.songs.isNotEmpty()
            if (showOnlineSongs) {
                item(key = "online_songs_header") {
                    Text(
                        text = "Songs",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                val songsToShow = if (selectedFilter == SearchFilterTab.ALL) {
                    catalogResponse.songs.take(10)
                } else {
                    catalogResponse.songs
                }
                items(songsToShow, key = { "online_song_${it.id}_${it.title}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(
                                        track = item,
                                        contextTracks = catalogResponse.songs,
                                        shuffleModeEnabled = false,
                                        appendSimilar = true
                                    )
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(searchBarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.artworkUrl.isNotBlank()) {
                                AsyncImage(
                                    model = item.artworkUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = secondaryTextColor
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.title,
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (item.isExplicit) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ExplicitBadge()
                                }
                            }
                            Text(
                                text = if (item.album.isNotBlank()) "${item.artist} · ${item.album}" else item.artist,
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = appleAccent.copy(alpha = 0.14f),
                            modifier = Modifier.clickable {
                                Vibrator.click(context)
                                previewTrack = item
                                previewLyrics = null
                                isLoadingPreviewLyrics = true
                                scope.launch(Dispatchers.IO) {
                                    val dummyItem = with(LyricsFlowApi) { item.toLyricsflowMediaItem() }
                                    val fetchedLyrics = LyricsFlowApi.fetchAndParseLyrics(
                                        context = context,
                                        music = dummyItem,
                                        durationMs = item.durationMs
                                    )
                                    previewLyrics = fetchedLyrics
                                    isLoadingPreviewLyrics = false
                                }
                            }
                        ) {
                            Text(
                                text = "Lyrics",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = appleAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 5. ONLINE PLAYLISTS SECTION (-> Opens OnlinePlaylistView)
            val showPlaylists = (selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.PLAYLISTS) &&
                catalogResponse.playlists.isNotEmpty()
            if (showPlaylists) {
                item(key = "online_playlists_header") {
                    Text(
                        text = "Playlists",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                val playlistsToShow = if (selectedFilter == SearchFilterTab.ALL) {
                    catalogResponse.playlists.take(6)
                } else {
                    catalogResponse.playlists
                }
                items(playlistsToShow, key = { "online_pl_${it.id}" }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                LibraryObject.openOnlinePlaylist(
                                    playlistId = pl.id,
                                    name = pl.title,
                                    artworkUrl = pl.artworkUrl
                                )
                                navController.toUI(UI.OnlinePlaylistInfo)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(searchBarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pl.artworkUrl.isNotBlank()) {
                                AsyncImage(
                                    model = pl.artworkUrl,
                                    contentDescription = pl.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.QueueMusic, null, tint = secondaryTextColor)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = pl.title,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pl.subtitle.ifBlank { "Apple Music Playlist" },
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = secondaryTextColor)
                    }
                }
            }

            // 6. LOCAL LIBRARY MATCHES
            val showLocal = selectedFilter == SearchFilterTab.ALL || selectedFilter == SearchFilterTab.LIBRARY
            if (showLocal && (localMatchingSongs.isNotEmpty() || localMatchingAlbums.isNotEmpty() || localMatchingArtists.isNotEmpty())) {
                item(key = "local_library_header") {
                    Text(
                        text = "In Your Device Library",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
                    )
                }

                items(localMatchingArtists.take(4), key = { "local_artist_$it" }) { artistName ->
                    val artistSongs = MusicLibrary.Artist[artistName]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                LibraryObject.setTargetArtistName(artistName)
                                navController.toUI(UI.ArtistInfo)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShadowImageWithCache(
                            dataLambda = { artistSongs.firstOrNull()?.thumb },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            cornerRadius = 24.dp,
                            shadowAlpha = 0f,
                            imageQuality = ImageQuality.LOW
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = artistName,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Local Artist · ${artistSongs.size} songs",
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                }

                items(localMatchingAlbums.take(4), key = { "local_album_${it.first}" }) { (albumName, albumSongs) ->
                    val representative = albumSongs.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                LibraryObject.setTargetAlbumName(albumName)
                                navController.toUI(UI.AlbumInfo)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShadowImageWithCache(
                            dataLambda = { representative?.thumb },
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            cornerRadius = 8.dp,
                            shadowAlpha = 0f,
                            imageQuality = ImageQuality.LOW
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = albumName,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Local Album · ${representative?.artistsName ?: defaultArtistsName}",
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                items(localMatchingSongs.take(10), key = { "local_song_${it.uri}" }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    MediaController.prepare(song, localMatchingSongs)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShadowImageWithCache(
                            dataLambda = { song.thumb },
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            cornerRadius = 8.dp,
                            shadowAlpha = 0f,
                            imageQuality = ImageQuality.LOW
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = song.title ?: defaultTitle,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.artistsName ?: defaultArtistsName} · ${song.album ?: defaultAlbum}",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = appleAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        }

        // Bottom Floating Liquid Glass Search Bar (iOS 27 style above the tab bar / miniplayer)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 150.dp)
                .height(48.dp)
                .liquidGlassCapsule()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Artists, Songs, Lyrics and more",
                            fontFamily = SFProDisplay,
                            fontSize = 15.5.sp,
                            color = secondaryTextColor
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = SFProDisplay,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textColor
                        ),
                        cursorBrush = SolidColor(appleAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val trimmed = query.trim()
                                if (trimmed.isNotEmpty()) {
                                    SettingsLibrary.addSearchHistoryQuery(trimmed)
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(secondaryTextColor.copy(alpha = 0.25f))
                            .clickable {
                                query = ""
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = textColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive LyricsFlow Live Lyrics Preview Bottom Sheet for Online Catalog Tracks
    previewTrack?.let { selectedTrack ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { previewTrack = null },
            sheetState = sheetState,
            containerColor = Color(0xFF141416),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .padding(horizontal = 22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        if (selectedTrack.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = selectedTrack.artworkUrl,
                                contentDescription = selectedTrack.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = selectedTrack.title,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedTrack.artist} — ${selectedTrack.album}",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = appleAccent,
                        modifier = Modifier.clickable {
                            val trackToPlay = selectedTrack
                            previewTrack = null
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(trackToPlay, catalogResponse.songs)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }

                when {
                    isLoadingPreviewLyrics -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = appleAccent)
                        }
                    }
                    previewLyrics == null || previewLyrics!!.entries.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No synced lyrics found for this track.",
                                fontFamily = SFProDisplay,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                    else -> {
                        val parsed = previewLyrics!!
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(parsed.entries.size) { lineIdx ->
                                val lineText = parsed.entries[lineIdx].dropLast(1).joinToString("") { it.second }
                                val subText = parsed.subtitles.getOrNull(lineIdx)
                                    ?: parsed.transliterations.getOrNull(lineIdx)
                                val bgList = listOfNotNull(parsed.backgroundLines.getOrNull(lineIdx))

                                if (lineText.isNotBlank()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = lineText,
                                            fontFamily = SFProDisplay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 23.sp,
                                            lineHeight = 30.sp,
                                            color = Color.White
                                        )
                                        if (!subText.isNullOrBlank()) {
                                            Text(
                                                text = subText,
                                                fontFamily = SFProDisplay,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                color = Color.White.copy(alpha = 0.58f),
                                                modifier = Modifier.padding(top = 3.dp)
                                            )
                                        }
                                        bgList.forEach { bg ->
                                            Text(
                                                text = "(${bg.text})",
                                                fontFamily = SFProDisplay,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp,
                                                color = Color.White.copy(alpha = 0.62f),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (parsed.songwriters.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Written by ${parsed.songwriters.joinToString(", ")}",
                                        fontFamily = SFProDisplay,
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.42f),
                                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
