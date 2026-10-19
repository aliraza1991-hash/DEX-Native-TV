
package com.dex.assistant

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var tts: TextToSpeech

    private val requestSpeech = 702
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        tts = TextToSpeech(this, this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 22, 36, 24)
            setBackgroundColor(Color.rgb(3, 8, 18))
        }

        val heading = TextView(this).apply {
            text = "D E X     /     COMMAND CENTRE"
            textSize = 22f
            setTextColor(Color.rgb(171, 237, 255))
            gravity = Gravity.CENTER
        }
        root.addView(heading, LinearLayout.LayoutParams(-1, 52))

        val orb = HoloOrbView(this)
        root.addView(orb, LinearLayout.LayoutParams(320, 230).apply {
            gravity = Gravity.CENTER
        })

        status = TextView(this).apply {
            text = "SYSTEM ONLINE  •  VOICE READY"
            textSize = 14f
            setTextColor(Color.rgb(115, 224, 255))
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 36))

        transcript = TextView(this).apply {
            text = "DEX is ready. Select LISTEN to issue a command."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(10, 8, 10, 12)
        }
        root.addView(transcript, LinearLayout.LayoutParams(-1, 72))

        val listen = Button(this).apply {
            text = "LISTEN  /  VOICE COMMAND"
            textSize = 18f
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener { startVoiceInput() }
        }
        root.addView(listen, LinearLayout.LayoutParams(440, 68).apply {
            gravity = Gravity.CENTER
        })

        val note = TextView(this).apply {
            text = "DEX prototype 0.2 • AI backend not connected"
            textSize = 12f
            setTextColor(Color.rgb(120, 145, 164))
            gravity = Gravity.CENTER
        }
        root.addView(note, LinearLayout.LayoutParams(-1, 40))

        setContentView(root)
        listen.requestFocus()
    }

    private fun startVoiceInput() {
        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to DEX")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        status.text = "LISTENING • WAITING FOR SPEECH"
        transcript.text = "Speak now. DEX is waiting for your command."

        try {
            if (intent.resolveActivity(packageManager) == null) {
                status.text = "VOICE SERVICE NOT FOUND"
                transcript.text =
                    "No compatible speech recognition service is installed or enabled."
                return
            }

            startActivityForResult(intent, requestSpeech)
        } catch (_: Exception) {
            status.text = "COULD NOT START VOICE INPUT"
            transcript.text =
                "Check that a speech recognition service is installed and enabled, then try again."
        }
    }

    @Deprecated("Uses the compatible Android speech recognition activity")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != requestSpeech) return

        if (resultCode != RESULT_OK) {
            status.text = "VOICE INPUT CANCELLED"
            transcript.text =
                "No command was received. Select LISTEN and try again."
            return
        }

        val matches = data?.getStringArrayListExtra(
            RecognizerIntent.EXTRA_RESULTS
        )
        val phrase = matches?.firstOrNull()?.trim().orEmpty()

        if (phrase.isBlank()) {
            status.text = "NO SPEECH DETECTED"
            transcript.text =
                "DEX did not receive any words. Check the microphone and try again."
            return
        }

        val response = localDemoResponse(phrase)
        status.text = "COMMAND RECEIVED • DEMO MODE"
        transcript.text = "YOU SAID: $phrase\n\n$response"

        if (ttsReady) {
            tts.speak(
                response,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "dex-response"
            )
        }
    }

    private fun localDemoResponse(input: String): String {
        val q = input.lowercase(Locale.getDefault())

        return when {
            "youtube" in q ->
                "I heard a YouTube request. App launching is not connected yet."

            "time" in q ->
                "The system time is ${
                    SimpleDateFormat(
                        "h:mm a",
                        Locale.getDefault()
                    ).format(Date())
                }."

            "hello" in q || "hi dex" in q ->
                "Hello. DEX is online. The AI service is not connected yet."

            else ->
                "Command received. Connect the secure DEX AI backend to process this request."
        }
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS

        if (ttsReady) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                status.text = "VOICE READY • DEFAULT TTS LANGUAGE UNAVAILABLE"
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

private class HoloOrbView(context: android.content.Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.34f

        for (i in 7 downTo 1) {
            p.shader = RadialGradient(
                cx, cy, r * i / 3f,
                intArrayOf(
                    Color.argb(22 * i, 25, 192, 255),
                    Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r * i / 3f, p)
        }

        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.2f
        p.color = Color.rgb(118, 229, 255)
        canvas.drawCircle(cx, cy, r, p)

        p.strokeWidth = 1f
        p.color = Color.argb(140, 85, 202, 255)
        canvas.drawOval(
            cx - r * 1.28f, cy - r * 0.35f,
            cx + r * 1.28f, cy + r * 0.35f, p
        )

        canvas.save()
        canvas.rotate(-28f, cx, cy)
        canvas.drawOval(
            cx - r * 1.25f, cy - r * 0.48f,
            cx + r * 1.25f, cy + r * 0.48f, p
        )
        canvas.restore()

        p.style = Paint.Style.FILL
        p.color = Color.rgb(207, 250, 255)
        canvas.drawCircle(cx, cy, r * 0.12f, p)

        p.color = Color.argb(160, 91, 223, 255)
        canvas.drawCircle(cx, cy, r * 0.28f, p)

        p.color = Color.rgb(4, 13, 27)
        canvas.drawCircle(cx, cy, r * 0.19f, p)

        p.color = Color.rgb(153, 241, 255)
        p.textAlign = Paint.Align.CENTER
        p.textSize = 14f
        canvas.drawText("DEX CORE", cx, cy + r + 30f, p)
    }
}
