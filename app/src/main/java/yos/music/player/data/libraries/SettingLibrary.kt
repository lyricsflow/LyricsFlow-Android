package com.lyricsflow.player.data.libraries

import androidx.compose.runtime.Stable
import com.funny.data_saver.core.mutableDataSaverListStateOf
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.lyricsflow.player.data.SettingsSaver

@Stable
object SettingsLibrary {

    @Stable
    private var followedArtistsSaver by mutableDataSaverListStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_followed_artists",
        initialValue = listOf<LyricsflowStringWrapper>()
    )

    val FollowedArtists: List<String>
        get() = followedArtistsSaver.map { it.value }

    fun isArtistFollowed(artistName: String): Boolean
    {
        return FollowedArtists.contains(artistName)
    }

    fun followArtist(artistName: String)
    {
        if (!isArtistFollowed(artistName)) {
            followedArtistsSaver = followedArtistsSaver + LyricsflowStringWrapper(artistName)
        }
    }

    fun unfollowArtist(artistName: String)
    {
        followedArtistsSaver = followedArtistsSaver - LyricsflowStringWrapper(artistName)
    }

    fun toggleArtistFollowed(artistName: String)
    {
        if (isArtistFollowed(artistName)) {
            unfollowArtist(artistName)
        } else {
            followArtist(artistName)
        }
    }

    @Stable
    private var savedAlbumsSaver by mutableDataSaverListStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_saved_albums",
        initialValue = listOf<LyricsflowStringWrapper>()
    )

    val SavedAlbums: List<String>
        get() = savedAlbumsSaver.map { it.value }

    fun isAlbumSaved(albumKey: String): Boolean
    {
        return albumKey.isNotBlank() && SavedAlbums.contains(albumKey)
    }

    fun saveAlbum(albumKey: String)
    {
        if (albumKey.isNotBlank() && !isAlbumSaved(albumKey)) {
            savedAlbumsSaver = savedAlbumsSaver + LyricsflowStringWrapper(albumKey)
        }
    }

    fun unsaveAlbum(albumKey: String)
    {
        if (albumKey.isNotBlank()) {
            savedAlbumsSaver = savedAlbumsSaver - LyricsflowStringWrapper(albumKey)
        }
    }

    fun toggleAlbumSaved(albumKey: String)
    {
        if (isAlbumSaved(albumKey)) {
            unsaveAlbum(albumKey)
        } else {
            saveAlbum(albumKey)
        }
    }

    /**
     * Persistent Search History (max 5 recent queries, empty by default)
     */
    @Stable
    private var searchHistorySaver by mutableDataSaverListStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_search_history_v1",
        initialValue = listOf<LyricsflowStringWrapper>()
    )

    val SearchHistory: List<String>
        get() = searchHistorySaver.map { it.value }

    fun addSearchHistoryQuery(query: String)
    {
        val clean = query.trim()
        if (clean.isEmpty()) return
        val filtered = searchHistorySaver.filterNot { it.value.equals(clean, ignoreCase = true) }
        searchHistorySaver = (listOf(LyricsflowStringWrapper(clean)) + filtered).take(5)
    }

    fun removeSearchHistoryQuery(query: String)
    {
        val clean = query.trim()
        searchHistorySaver = searchHistorySaver.filterNot { it.value.equals(clean, ignoreCase = true) }
    }

    fun clearSearchHistory()
    {
        searchHistorySaver = emptyList()
    }

    /**
     * Online Streaming Audio Quality (Lossless ALAC & Dolby Atmos)
     */
    @Stable
    var EnableLosslessStreaming by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_enable_lossless_streaming",
        initialValue = false
    )

    @Stable
    var EnableDolbyAtmos by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_enable_dolby_atmos",
        initialValue = false
    )

    /**
     * Skipped app update version tag
     */
    @Stable
    var SkippedUpdateVersion by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_skipped_update_version",
        initialValue = ""
    )

    /**
     * 是否显示音量条
     */
    @Stable
    var NowPlayingShowVolumeBar by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_nowplaying_show_volume_bar",
        initialValue = true
    )

    /**
     * 应用主题
     */
    @Stable
    var CustomTheme by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_theme",
        initialValue = "Auto"
    )

    /**
     * 是否已设置过屏幕圆角大小
     */
    @Stable
    var ScreenCornerSet by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_corner_set",
        initialValue = false
    )

    /**
     * 屏幕圆角大小
     */
    @Stable
    var ScreenCorner by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_corner",
        initialValue = "30"
    )

    /**
     * 歌曲排序
     */
    @Stable
    var SongSort by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "lyricsflow_player_song_sort",
        initialValue = SongSortEnum.MUSIC_TITLE.ordinal
    )

    @Stable
    enum class SongSortEnum {
        MUSIC_TITLE, MUSIC_DURATION, ARTIST_NAME, MODIFIED_DATE
    }

    /**
     * 启用降序
     */
    @Stable
    var EnableDescending by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "lyricsflow_player_enable_descending",
        initialValue = false
    )

    /**
     * 歌词界面 - 翻译
     */
    @Stable
    var NowPlayingTranslation by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "now_playing_translation",
        initialValue = true
    )

    /**
     * 每次启动时刷新媒体库
     */
    @Stable
    var RefreshEveryTime by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_refresh_everytime",
        initialValue = false
    )

    @Stable
    var AnimatedAlbumCovers by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_animated_album_covers",
        initialValue = true
    )

    @Stable
    var AnimatedAlbumCoverBlacklist by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_animated_album_cover_blacklist",
        initialValue = ""
    )

    @Stable
    var AnimatedAlbumCoversUseApi by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_animated_album_covers_use_api",
        initialValue = true
    )

    fun isAnimatedAlbumCoverBlacklisted(albumName: String): Boolean
    {
        val normalizedAlbumName = albumName.trim()
        if (normalizedAlbumName.isEmpty()) {return false}

        return AnimatedAlbumCoverBlacklist
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { it.equals(normalizedAlbumName, ignoreCase = true) }
    }

    @Stable
    var ArtistSplitSeparators by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_artist_split_separators",
        initialValue = ","
    )

    /**
     * 歌词字体字重 (default 800 / ExtraBold)
     */
    @Stable
    var LyricFontWeight by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_weight_v2",
        initialValue = "ExtraBold"
    )

    /**
     * Numeric lyric font weight (300..900, default 800)
     */
    @Stable
    var LyricFontWeightNumeric by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_weight_numeric",
        initialValue = 800
    )

    /**
     * Lyric font width percentage (75..125, default 100)
     */
    @Stable
    var LyricFontWidth by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_font_width",
        initialValue = 100
    )

    /**
     * Independent lyric display toggles (Translation / Pronunciation / Swap)
     */
    @Stable
    var LyricShowTranslation by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_lyric_show_translation",
        initialValue = true
    )

    @Stable
    var LyricShowPronunciation by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_lyric_show_pronunciation",
        initialValue = true
    )

    @Stable
    var LyricSwapOriginalWithPronunciation by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_lyric_swap_original_with_pronunciation",
        initialValue = false
    )

    @Stable
    var LyricHideOriginalInPronunciationBar by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_lyric_hide_original_in_pronunciation_bar",
        initialValue = false
    )

    /**
     * 歌词平衡行模式
     */
    @Stable
    var LyricLineBalance by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_line_balance",
        initialValue = false
    )

    /**
     * 歌词模糊效果
     */
    @Stable
    var LyricBlurEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_lyric_blur_effect",
        initialValue = true
    )

    /**
     * 播放界面背景动态效果
     */
    @Stable
    var NowplayingBackgroundEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_nowplaying_background_effect",
        initialValue = true
    )

    /**
     * 界面工具栏模糊效果
     */
    @Stable
    var BarBlurEffect by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_ui_blur_effect",
        initialValue = true
    )

    /**
     * 媒体通知-额外的媒体图标
     */
    @Stable
    var NotificationEnableIcon by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_notification_enable_icon",
        initialValue = true
    )

    /**
     * 媒体通知-小一号图标
     */
    @Stable
    var NotificationSmallerIcon by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_performance_notification_smaller_icon",
        initialValue = false
    )

    /**
     * 渐入渐出播放
     */
    @Stable
    var FadePlay by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_fade_in_out",
        initialValue = true
    )

    /**
     * 播放历史
     */
    @Stable
    var ListenHistory by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_play_history",
        initialValue = true
    )

    /**
     * 聆听统计 - 上次选择的时间段
     */
    @Stable
    var StatsSelectedPeriod by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_stats_selected_period",
        initialValue = 0
    )

    /**
     * 状态栏歌词
     */
    @Stable
    var StatusBarLyricEnabled by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "statusBarLyricEnabled",
        initialValue = false
    )

    /**
     * 状态栏歌词 Hook 状态
     */
    @Stable
    var StatusBarLyricHooked by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "statusBarLyricHooked",
        initialValue = false
    )

    /**
     * ExoPlayer行为 - 音频属性
     */
    @Stable
    var AudioAttributes by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_audio_attributes",
        initialValue = true
    )

    /**
     * ExoPlayer解码 - 编解码器
     */
    @Stable
    var Codec by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_codec",
        initialValue = "Auto"
    )

    /**
     * ExoPlayer解码 - 硬件音频轨道播放参数
     */
    @Stable
    var HardwareAudioTrackPlayBackParams by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_hardware_audio_track_playback_params",
        initialValue = false
    )

    /**
     * ExoPlayer解码 - 音频浮点输出
     */
    @Stable
    var AudioFloatOutput by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_audio_exoplayer_audio_float_output",
        initialValue = false
    )

    /**
     * 排除一分钟以内的歌曲
     */
    @Stable
    var EnableExcludeSongsUnderOneMinute by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_library_enable_exclude_songs_under_one_minute",
        initialValue = true
    )

    /**
     * Sleep timer fade-out duration in milliseconds (PRD §5.6.4 FR-ST-9).
     *
     * Accepted UI values: 0 (no fade) / 5000 / 10000 / 30000.
     * Default: 5000 ms (5-second fade) — matches the "polished" expectation
     * for bedtime use without being so long the user wonders if the timer
     * fired.
     */
    @Stable
    var SleepTimerFadeDurationMs by mutableDataSaverStateOf(
        dataSaverInterface = SettingsSaver,
        key = "settings_sleep_timer_fade_duration_ms",
        initialValue = 5000L,
    )
}
