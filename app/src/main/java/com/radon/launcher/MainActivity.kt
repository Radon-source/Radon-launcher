package com.radon.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var clockText: TextView
    private lateinit var appsRowContainer: LinearLayout
    private lateinit var wallpaperVideoView: VideoView
    private val uiHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            clockText.text = sdf.format(Date())
            uiHandler.postDelayed(this, 10_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        appsRowContainer = findViewById(R.id.appsRowContainer)
        wallpaperVideoView = findViewById(R.id.wallpaperVideoView)

        val settingsButton: Button = findViewById(R.id.settingsButton)
        styleFocusableButton(settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        setupLiveWallpaper()
        populateInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        uiHandler.removeCallbacks(clockRunnable)
        uiHandler.post(clockRunnable)
        if (!wallpaperVideoView.isPlaying) {
            wallpaperVideoView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(clockRunnable)
    }

    @Deprecated("Keep launcher active on TV home screen")
    override fun onBackPressed() {
        // Intentionally absorbed so pressing Back stays in Radon Launcher
    }

    private fun setupLiveWallpaper() {
        val videoFile = File("/sdcard/Download/wallpaper.mp4")
        if (videoFile.exists()) {
            wallpaperVideoView.setVideoPath(videoFile.absolutePath)
            wallpaperVideoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                wallpaperVideoView.start()
            }
        }
    }

    private fun populateInstalledApps() {
        appsRowContainer.removeAllViews()
        val pm = packageManager

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        for (info in resolveInfos) {
            val pkgName = info.activityInfo.packageName
            if (pkgName == packageName) continue

            val label = info.loadLabel(pm).toString()
            val tileButton = Button(this).apply {
                text = label
                textSize = 18f
                setTextColor(Color.WHITE)
                minWidth = 240
                minHeight = 140
                isFocusable = true
                isFocusableInTouchMode = true
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 20
                }
                layoutParams = params
                setOnClickListener {
                    val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                        ?: pm.getLeanbackLaunchIntentForPackage(pkgName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                }
            }
            styleFocusableButton(tileButton)
            appsRowContainer.addView(tileButton)
        }
    }

    private fun styleFocusableButton(view: View) {
        fun applyState(hasFocus: Boolean) {
            val bg = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(if (hasFocus) Color.parseColor("#1D4ED8") else Color.parseColor("#161B22"))
                setStroke(if (hasFocus) 5 else 2, if (hasFocus) Color.parseColor("#60A5FA") else Color.parseColor("#30363D"))
            }
            view.background = bg
            val scale = if (hasFocus) 1.06f else 1.0f
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
        }

        applyState(view.hasFocus())
        view.setOnFocusChangeListener { _, hasFocus -> applyState(hasFocus) }
    }
}
