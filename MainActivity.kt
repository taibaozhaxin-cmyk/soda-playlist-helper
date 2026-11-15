package com.example.sodaplaylist

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private data class Track(val name: String, val uri: Uri)
    private val tracks = mutableListOf<Track>()
    private var player: MediaPlayer? = null
    private lateinit var trackList: LinearLayout
    private lateinit var status: TextView
    private lateinit var playlistInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(20))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(TextView(this).apply {
            text = "汽水歌单助手"
            textSize = 28f
            setTextColor(0xFF171717.toInt())
        })
        content.addView(TextView(this).apply {
            text = "本地音乐管理 · 版本 0.1.0"
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(6), 0, dp(18))
        })

        content.addView(TextView(this).apply {
            text = "汽水音乐歌单链接"
            textSize = 17f
        })
        playlistInput = EditText(this).apply {
            hint = "粘贴你有权访问的歌单链接"
            textSize = 15f
            singleLine = true
        }
        content.addView(playlistInput, matchWrap())
        content.addView(Button(this).apply {
            text = "检查链接"
            setOnClickListener { checkPlaylistLink() }
        }, matchWrap())

        content.addView(TextView(this).apply {
            text = "本地音乐"
            textSize = 20f
            setPadding(0, dp(18), 0, dp(6))
        })
        content.addView(Button(this).apply {
            text = "添加本地音乐（可多选）"
            setOnClickListener { chooseAudio() }
        }, matchWrap())
        content.addView(Button(this).apply {
            text = "停止播放"
            setOnClickListener { stopPlayback() }
        }, matchWrap())

        status = TextView(this).apply {
            text = "请选择本地音频文件，点击歌曲名称即可播放。"
            textSize = 14f
            setTextColor(0xFF555555.toInt())
            setPadding(0, dp(12), 0, dp(12))
        }
        content.addView(status)
        trackList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(trackList, matchWrap())

        content.addView(TextView(this).apply {
            text = "说明：当前版本支持本地音频导入和播放。歌单链接检查仅做格式检查，尚未接入汽水音乐官方授权接口；本版本不下载受版权保护或 DRM 限制的音频，也暂未实现 MP3 转码。"
            textSize = 12f
            setTextColor(0xFF777777.toInt())
            setPadding(0, dp(22), 0, dp(12))
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun checkPlaylistLink() {
        val value = playlistInput.text.toString().trim()
        val uri = runCatching { Uri.parse(value) }.getOrNull()
        if (value.isBlank() || uri?.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            status.text = "链接格式不正确，请粘贴完整的 http:// 或 https:// 歌单链接。"
        } else {
            status.text = "链接格式正确。当前版本尚未连接汽水音乐官方授权服务，暂不能自动读取歌单内容。"
        }
    }

    private fun chooseAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_AUDIO)
    }

    @Deprecated("Kept for compatibility with the simple Android project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_AUDIO || resultCode != RESULT_OK || data == null) return

        val selected = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) selected.add(clip.getItemAt(i).uri)
        } ?: data.data?.let { selected.add(it) }

        selected.forEach { uri ->
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = getDisplayName(uri) ?: "音频 ${tracks.size + 1}"
            if (tracks.none { it.uri == uri }) tracks.add(Track(name, uri))
        }
        renderTracks()
        status.text = "已添加 ${tracks.size} 首音频。点击歌曲名称播放。"
    }

    private fun getDisplayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun renderTracks() {
        trackList.removeAllViews()
        tracks.forEachIndexed { index, track ->
            trackList.addView(Button(this).apply {
                text = "${index + 1}. ${track.name}"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { playTrack(track) }
            }, matchWrap())
        }
    }

    private fun playTrack(track: Track) {
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, track.uri)
                setOnPreparedListener {
                    it.start()
                    status.text = "正在播放：${track.name}"
                }
                setOnCompletionListener { status.text = "播放结束：${track.name}" }
                setOnErrorListener { _, _, _ ->
                    status.text = "无法播放该文件，可能是格式不受支持。"
                    stopPlayback()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            status.text = "播放失败：${e.localizedMessage ?: "文件不可用"}"
        }
    }

    private fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    companion object { private const val REQUEST_AUDIO = 100 }
}
