package com.example.opencodeandroid

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 13, 13)
    private val panel = Color.rgb(18, 23, 22)
    private val border = Color.rgb(42, 52, 49)
    private val lime = Color.rgb(184, 243, 107)
    private val muted = Color.rgb(147, 161, 154)
    private lateinit var log: TextView
    private lateinit var status: TextView
    private lateinit var command: EditText
    private var process: Process? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(20, 18, 20, 12)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val mark = TextView(this).apply {
            text = "⌘"
            textSize = 27f
            setTextColor(lime)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            background = rounded(lime, 18)
        }
        header.addView(mark, LinearLayout.LayoutParams(52, 52))
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 0, 0)
        }
        titleBox.addView(label("OpenCode", 21f, Color.WHITE, true))
        titleBox.addView(label("مساعد البرمجة على جهازك", 12f, muted, false))
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        status = label("●  غير متصل", 12f, muted, false).apply {
            setPadding(12, 8, 12, 8)
            background = rounded(border, 18)
        }
        header.addView(status)
        root.addView(header, LinearLayout.LayoutParams(-1, 60))

        val gap = Space(this)
        root.addView(gap, LinearLayout.LayoutParams(1, 18))
        val intro = label("الطرفية", 14f, Color.WHITE, true)
        root.addView(intro)
        root.addView(label("شغّل OpenCode وتابع مخرجاته مباشرة.", 12f, muted, false))
        root.addView(Space(this), LinearLayout.LayoutParams(1, 12))

        val logScroll = ScrollView(this).apply {
            setBackgroundColor(panel)
            isFillViewport = true
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        log = TextView(this).apply {
            text = "OpenCode Mobile v1.0\nجاهز للبدء.\n\nاضغط «تشغيل» لفتح جلسة OpenCode.\n"
            textSize = 13f
            setTextColor(Color.rgb(211, 224, 216))
            typeface = Typeface.MONOSPACE
            setPadding(16, 16, 16, 16)
            setTextIsSelectable(true)
        }
        logScroll.addView(log)
        root.addView(logScroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Space(this), LinearLayout.LayoutParams(1, 14))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        command = EditText(this).apply {
            hint = "اكتب أمرًا أو رسالة..."
            hintTextColor = muted
            setTextColor(Color.WHITE)
            textSize = 14f
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(15, 0, 15, 0)
            background = rounded(border, 12)
            setOnEditorActionListener { _, _, _ -> send(); true }
        }
        row.addView(command, LinearLayout.LayoutParams(0, 50, 1f))
        val send = button("إرسال", lime, bg).apply { setOnClickListener { send() } }
        row.addView(send, LinearLayout.LayoutParams(78, 50).apply { setMargins(8, 0, 0, 0) })
        root.addView(row)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL
        }
        val start = button("▶  تشغيل", lime, bg).apply { setOnClickListener { startOpenCode() } }
        val stop = button("■  إيقاف", border, Color.WHITE).apply { setOnClickListener { stopOpenCode() } }
        actions.addView(start, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(0, 12, 6, 0) })
        actions.addView(stop, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(6, 12, 0, 0) })
        root.addView(actions)
        setContentView(root)
    }

    private fun startOpenCode() {
        if (process?.isAlive == true) return append("\nالجلسة قيد التشغيل بالفعل.\n")
        status.text = "●  جارٍ التشغيل"
        status.setTextColor(lime)
        append("\n$ opencode\nبدء تشغيل OpenCode...\n")
        executor.execute {
            try {
                val p = ProcessBuilder("/system/bin/sh", "-c", findExecutable())
                    .redirectErrorStream(true).start()
                process = p
                BufferedReader(InputStreamReader(p.inputStream)).forEachLine { line ->
                    runOnUiThread { append("$line\n") }
                }
                p.waitFor()
                runOnUiThread {
                    status.text = "●  غير متصل"
                    status.setTextColor(muted)
                    append("\nانتهت الجلسة.\n")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "●  خطأ"
                    status.setTextColor(Color.rgb(255, 125, 125))
                    append("\nتعذر تشغيل OpenCode: ${e.message}\nضع الملف التنفيذي في files/bin/opencode.\n")
                }
            }
        }
    }

    private fun findExecutable(): String {
        val local = "${filesDir.absolutePath}/bin/opencode"
        return "if [ -x '$local' ]; then '$local'; else command -v opencode >/dev/null 2>&1 && opencode || exit 127; fi"
    }

    private fun stopOpenCode() {
        process?.destroy()
        process = null
        status.text = "●  غير متصل"
        status.setTextColor(muted)
        append("\nتم إيقاف الجلسة.\n")
    }

    private fun send() {
        val text = command.text.toString().trim()
        if (text.isEmpty()) return
        append("\n> $text\n")
        command.text.clear()
        try {
            process?.outputStream?.bufferedWriter()?.apply {
                write(text); newLine(); flush()
            } ?: append("لا توجد جلسة نشطة. اضغط «تشغيل» أولًا.\n")
        } catch (e: Exception) { append("تعذر إرسال الأمر.\n") }
    }

    private fun append(text: String) {
        log.append(text)
        (log.parent as? ScrollView)?.post { (log.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun button(text: String, color: Int, textColor: Int) = Button(this).apply {
        this.text = text; textSize = 13f; setTextColor(textColor); isAllCaps = false
        background = rounded(color, 12)
    }

    private fun rounded(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color); cornerRadius = radius.toFloat()
        }

    override fun onDestroy() {
        process?.destroy()
        executor.shutdownNow()
        super.onDestroy()
    }
}