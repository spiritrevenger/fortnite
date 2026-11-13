package com.perso.fortauto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class AutoService : AccessibilityService() {
    companion object {
        var instance: AutoService? = null
        // Équivalent anglais de JOUER / OBTENIR / PASSER / REJOUER (mot entier, insensible à la casse)
        val TARGETS = listOf("PLAY AGAIN", "REPLAY", "PLAY", "GET", "SKIP")
        const val REACTION_DELAY_MS = 500L
        const val MOVE_MS = 1000L
        // Position du joystick gauche (fractions de l'écran, mode paysage) : à ajuster
        const val JOY_X = 0.15f
        const val JOY_Y = 0.72f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var running = false

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun start() {
        if (running) return
        running = true
        packageManager.getLaunchIntentForPackage("com.epicgames.fortnite")?.let { startActivity(it) }
        handler.postDelayed({ step(0) }, 5000)
    }

    fun stop() { running = false; handler.removeCallbacksAndMessages(null) }

    private fun retry(phase: Int, ms: Long = 1200) { if (running) handler.postDelayed({ step(phase) }, ms) }

    private fun step(phase: Int) {
        if (!running) return
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                val hw = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
                val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                r.hardwareBuffer.close()
                if (bmp == null) return retry(phase)
                ocr.process(InputImage.fromBitmap(bmp, 0))
                    .addOnSuccessListener { t ->
                        val hit = find(t)
                        if (hit != null) {
                            handler.postDelayed({ tap(hit.first, hit.second) { retry(0, 1000) } }, REACTION_DELAY_MS)
                        } else move(phase)
                    }
                    .addOnFailureListener { retry(phase) }
            }
            override fun onFailure(code: Int) { retry(phase, 1500) }
        })
    }

    private fun find(t: Text): Pair<Float, Float>? {
        for (target in TARGETS) {
            val re = Regex("\\b" + target + "\\b", RegexOption.IGNORE_CASE)
            for (b in t.textBlocks) for (line in b.lines) {
                val box = line.boundingBox ?: continue
                if (re.containsMatchIn(line.text)) return Pair(box.exactCenterX(), box.exactCenterY())
            }
        }
        return null
    }

    private fun tap(x: Float, y: Float, done: () -> Unit) {
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build()
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { done() }
            override fun onCancelled(d: GestureDescription?) { done() }
        }, null)
    }

    // phase 0 = gauche (touche Q), phase 1 = droite (touche D)
    private fun move(phase: Int) {
        val dm = resources.displayMetrics
        val w = maxOf(dm.widthPixels, dm.heightPixels).toFloat()
        val h = minOf(dm.widthPixels, dm.heightPixels).toFloat()
        val sx = w * JOY_X; val sy = h * JOY_Y
        val dx = (if (phase == 0) -1 else 1) * 60 * dm.density
        val p = Path().apply { moveTo(sx, sy); lineTo(sx + dx, sy) }
        val g = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, MOVE_MS)).build()
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { if (running) step(1 - phase) }
            override fun onCancelled(d: GestureDescription?) { retry(phase) }
        }, null)
    }
}
