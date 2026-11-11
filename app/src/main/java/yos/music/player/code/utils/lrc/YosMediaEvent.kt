package com.lyricsflow.player.code.utils.lrc

/**
 * LyricsflowLyricView 媒体事件接口
 *
 * 之后可能在此接口实现更多其它功能
 */
interface LyricsflowMediaEvent {
    /**
     * 进度跳转事件
     * @param position 要跳转到的进度
     */
    fun onSeek(position: Int)
}