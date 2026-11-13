package com.perso.fortauto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val l = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        fun btn(t: String, f: () -> Unit) = l.addView(Button(this).apply { text = t; setOnClickListener { f() } })
        btn("1. Activer le service d'accessibilité") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        btn("2. DÉMARRER (5 s de délai, ouvre Fortnite)") {
            val s = AutoService.instance
            if (s == null) Toast.makeText(this, "Service non activé", Toast.LENGTH_LONG).show() else s.start()
        }
        btn("ARRÊTER") { AutoService.instance?.stop() }
        setContentView(l)
    }
}
