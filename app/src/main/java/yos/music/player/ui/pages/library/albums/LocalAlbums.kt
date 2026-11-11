package com.lyricsflow.player.ui.pages.library.albums

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyricsflow.player.R
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.SearchTextField
import com.lyricsflow.player.ui.widgets.basic.ShadowImage
import com.lyricsflow.player.ui.widgets.basic.Title
import com.lyricsflow.player.ui.widgets.basic.TitleWithLazyVerticalGrid
import com.lyricsflow.player.ui.widgets.basic.LyricsflowWrapper

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LocalAlbums(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    Column(
        Modifier
            .fillMaxSize()
        /*.statusBarsPadding()*/
    ) {
        val albumsList = remember("LocalAlbums_albumsList") {
            MusicLibrary.albums
        }

        val searchText = remember("LocalAlbums_searchText") {
            mutableStateOf("")
        }

        val hideMusic = remember("LocalAlbums_showMusic") {
            derivedStateOf {
                albumsList.isEmpty()
            }
        }
        if (hideMusic.value) {
            val message =
                stringResource(
                    id = R.string.tip_no_song
                )
            Title(
                title = stringResource(id = R.string.page_library_albums), onBack = {
                    navController.popBackStack()
                }
            ) {
                item("tip_no_song") {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(text = message, fontSize = 18.sp, modifier = Modifier.alpha(0.6f))
                    }
                }
            }
        } else {
            val useSearch = remember { derivedStateOf { searchText.value.isNotEmpty() } }
            val list = remember { mutableStateOf(albumsList) }

            LyricsflowWrapper {
                LaunchedEffect(searchText.value, albumsList) {
                    val query = searchText.value
                    val filteredList = withContext(Dispatchers.IO) {
                        if (query.isNotEmpty()) {
                            MusicLibrary.albums.asSequence().filter { album ->
                                album.contains(query, ignoreCase = true)
                            }.toList()
                        } else {
                            albumsList
                        }
                    }
                    list.value = filteredList
                }
            }

            val keyboardController = LocalSoftwareKeyboardController.current

            TitleWithLazyVerticalGrid(
                title = stringResource(id = R.string.page_library_albums), onBack = {
                    navController.popBackStack()
                },
                stickyContent = {
                    SearchTextField(
                        text = searchText.value,
                        placeholder = stringResource(id = R.string.page_library_search_album),
                        onValueChange = {
                            searchText.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 5.dp, bottom = 12.dp),
                        onSearch = {
                            if (searchText.value.isNotEmpty()) {
                                keyboardController?.hide()
                            }
                        },
                    )
                },
                stickyContentHeight = 61.dp,
            ) {
                itemsIndexed(
                    list.value,
                    key = { idx, album -> "${album}_$idx" }/*,
                    contentType = { _, _ -> "LocalAlbums_item" }*/
                ) { _, album ->
                    AlbumItems(
                        albumName = album,
                        animatedContentScope = animatedContentScope,
                        sharedTransitionScope = sharedTransitionScope
                    ) {
                        LibraryObject.setTargetAlbumName(album)
                        navController.toUI(UI.AlbumInfo)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LazyGridItemScope.AlbumItems(
    albumName: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onClick: () -> Unit
) {
    val songs = MusicLibrary.Album[albumName]
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        /*with(sharedTransitionScope) {*/
        ShadowImage(
            dataLambda = { songs.getOrNull(0)?.thumb },
            contentDescription = "Album",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp)
            /*.sharedElement(
                sharedTransitionScope.rememberSharedContentState(key = "image-$albumName"),
                animatedVisibilityScope = animatedContentScope
            )*/,
            shadowAlpha = 0f,
            cornerRadius = 7.dp,
            imageQuality = ImageQuality.HIGH
        )
        Text(
            text = albumName,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .alpha(0.9f)
            /*.sharedElement(
                sharedTransitionScope.rememberSharedContentState(key = "album_name-$albumName"),
                animatedVisibilityScope = animatedContentScope
            )*/
        )
        /*}*/

        Text(
            text = stringResource(id = R.string.page_library_album_desc, songs.size),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(0.6f),
            lineHeight = 17.sp
        )
    }
}
