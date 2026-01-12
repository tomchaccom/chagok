package com.example.myapplication.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import androidx.core.net.toUri
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * 간단한 비동기 이미지 로더 (Coil/Glide 없이 구현)
 * - 메모리 캐시(LruCache)
 * - 백그라운드 디코딩(Executor)
 * - ImageView 재사용 처리(tag)
 */
object ImageLoader {
    private val cache: LruCache<String, Bitmap>
    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val viewKeyMap = java.util.WeakHashMap<ImageView, String>()
    @Volatile
    private var paused: Boolean = false

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8 // 사용 가능한 메모리의 1/8
        cache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    fun loadInto(imageView: ImageView, uriStr: String?, placeholderRes: Int = android.R.drawable.ic_menu_report_image, reqWidth: Int? = null, reqHeight: Int? = null) {
        if (uriStr.isNullOrEmpty()) {
            imageView.setImageResource(placeholderRes)
            return
        }
        // 일시중지 상태면 새 로드를 시작하지 않고 placeholder만 설정
        if (paused) {
            imageView.setImageResource(placeholderRes)
            return
        }
        val key = uriStr
        // WeakHashMap으로 현재 로드중인 키를 관리
        synchronized(viewKeyMap) { viewKeyMap[imageView] = key }

        // android.resource는 배경 스레드에서 스트림을 열어 디코딩(이미지뷰에서 UI쓰레드 디코딩을 피함)
        // (빠른 경로로 UI에서 setImageResource 하는 대신, 아래 백그라운드 처리에서 openRawResource로 처리합니다)

        // 캐시 조회
        cache.get(key)?.let { bmp ->
            imageView.setImageBitmap(bmp)
            return
        }

        // placeholder 먼저 보여주기
        imageView.setImageResource(placeholderRes)

        // 백그라운드에서 디코드
        executor.execute {
            var bitmap: Bitmap? = null
            try {
                val ctx = imageView.context
                val uri = uriStr.toUri()
                val resolver = ctx.contentResolver
                var input: InputStream? = null
                try {
                    input = when (uri.scheme) {
                        "content", "file" -> resolver.openInputStream(uri)
                        "android.resource" -> {
                            // 시도: 리소스 id 찾기
                            val lastSeg = uriStr.substringAfterLast('/')
                            val nameNoExt = lastSeg.substringBeforeLast('.', lastSeg)
                            val sanitized = nameNoExt.replace(Regex("[^a-z0-9_]+"), "_").lowercase()
                            val resId = ctx.resources.getIdentifier(sanitized, "drawable", ctx.packageName)
                            if (resId != 0) ctx.resources.openRawResource(resId) else null
                        }
                        "http", "https" -> {
                            // 간단한 네트워크 로드 (주의: 네트워크 권한 및 긴 지연)
                            val conn = java.net.URL(uriStr).openConnection()
                            conn.connectTimeout = 5000
                            conn.readTimeout = 5000
                            conn.getInputStream()
                        }
                        else -> null
                    }
                    if (input != null) {
                        // 이미지뷰 크기에 맞춰 샘플링 (외부에서 명시된 크기 우선)
                        val (reqW, reqH) = if (reqWidth != null && reqHeight != null) Pair(reqWidth, reqHeight) else getTargetSize(imageView)
                        val opts = BitmapFactory.Options()
                        opts.inJustDecodeBounds = true
                        input.mark(1024 * 1024)
                        BitmapFactory.decodeStream(input, null, opts)
                        // 재열기/리셋
                        input.reset()

                        opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH)
                        opts.inJustDecodeBounds = false
                        bitmap = BitmapFactory.decodeStream(input, null, opts)
                    }
                } finally {
                    input?.close()
                }

                bitmap?.let { bmp ->
                    cache.put(key, bmp)
                    // UI thread에서 설정하되 ImageView의 태그가 요청 키와 일치할 때만 설정
                    mainHandler.post {
                        val currentTag = synchronized(viewKeyMap) { viewKeyMap[imageView] }
                        if (key == currentTag) {
                            imageView.setImageBitmap(bmp)
                        }
                    }
                }
            } catch (_: Exception) {
                // 실패시 아무 작업 없이 남겨둠(placeholder 유지)
            }
        }
    }

    private fun getTargetSize(view: ImageView): Pair<Int, Int> {
        val w = if (view.width > 0) view.width else view.context.resources.displayMetrics.widthPixels / 3
        val h = if (view.height > 0) view.height else 200
        return Pair(w, h)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun setPaused(p: Boolean) {
        paused = p
    }
}
