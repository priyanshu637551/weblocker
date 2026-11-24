package com.example.wablocker

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream

class BlockerVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopBlocking()
            return START_NOT_STICKY
        }
        startBlocking()
        return START_STICKY
    }

    private fun startBlocking() {
        if (tun != null) return
        val builder = Builder()
            .setSession("WhatsApp Blocker")
            .addAddress("10.255.0.1", 32)
            .addAddress("fd00::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)

        // Only WhatsApp is sent into this VPN; every other app is untouched.
        var added = 0
        for (pkg in TARGETS) {
            try { builder.addAllowedApplication(pkg); added++ }
            catch (_: PackageManager.NameNotFoundException) { }
        }
        if (added == 0) { // Safety: never capture all apps
            running = false
            stopSelf()
            return
        }

        tun = builder.establish()
        running = tun != null
        val fd = tun ?: return

        // Read and discard every packet = black hole.
        worker = Thread {
            val input = FileInputStream(fd.fileDescriptor)
            val buf = ByteArray(32767)
            try { while (!Thread.interrupted() && input.read(buf) >= 0) { } }
            catch (_: Exception) { }
        }.also { it.start() }
    }

    private fun stopBlocking() {
        worker?.interrupt(); worker = null
        tun?.close(); tun = null
        running = false
        stopSelf()
    }

    override fun onRevoke() { stopBlocking() }
    override fun onDestroy() { stopBlocking(); super.onDestroy() }

    companion object {
        const val ACTION_STOP = "com.example.wablocker.STOP"
        val TARGETS = listOf("com.whatsapp", "com.whatsapp.w4b")
        @Volatile var running = false
    }
}
