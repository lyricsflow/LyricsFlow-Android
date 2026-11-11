package com.lyricsflow.player.ui.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.lyricsflow.player.data.models.ImageViewModel
import com.lyricsflow.player.ui.pages.library.Library
import com.lyricsflow.player.ui.widgets.basic.LyricsflowWrapper

@Composable
fun HomeNav(
    navController: NavController,
    pagerState: PagerState,
    imageViewModel: ImageViewModel,
    nowPageOnChanged: (String) -> Unit
) =
    LyricsflowWrapper {
        LyricsflowWrapper {
            LaunchedEffect(pagerState.currentPage) {
                nowPageOnChanged(
                    when (pagerState.currentPage) {
                        0 -> "Home"
                        1 -> "New"
                        2 -> "Stats"
                        3 -> "Library"
                        4 -> "Search"
                        else -> "Home"
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            key = { page -> page },
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> Home(navController, imageViewModel)
                1 -> NewBrowse(navController)
                2 -> Stats(navController)
                3 -> Library(navController)
                4 -> Search(navController)
            }
        }
    }
