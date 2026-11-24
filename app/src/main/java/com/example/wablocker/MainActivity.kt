package com.example.wablocker

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var button: Button

    private val permission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) startBlock() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 20f; gravity = Gravity.CENTER }
        button = Button(this).apply { setOnClickListener { toggle() } }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(status); addView(button)
        })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun toggle() {
        if (BlockerVpnService.running) {
            startService(Intent(this, BlockerVpnService::class.java)
                .setAction(BlockerVpnService.ACTION_STOP))
            window.decorView.postDelayed({ refresh() }, 300)
        } else {
            val intent = VpnService.prepare(this)
            if (intent != null) permission.launch(intent) else startBlock()
        }
    }

    private fun startBlock() {
        startService(Intent(this, BlockerVpnService::class.java))
        window.decorView.postDelayed({ refresh() }, 300)
    }

    private fun refresh() {
        val on = BlockerVpnService.running
        status.text = if (on) "WhatsApp is BLOCKED\nNo delivery ticks will be sent"
                      else "WhatsApp is allowed"
        button.text = if (on) "Unblock WhatsApp" else "Block WhatsApp"
    }
}
