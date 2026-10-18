package com.godson.vpn.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.godson.vpn.GodsonApplication
import com.godson.vpn.MainActivity
import com.godson.vpn.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Service du tunnel VPN Godson (VpnService officiel Android).
 *
 * - Mode DÉMO (AppMode.isDemo) : le service refuse toute mise sous tension du
 *   tunnel — aucune connexion n'est possible, aucun état trompeur n'est publié.
 * - Mode RÉEL : le tunnel WireGuard est piloté par [VpnController] via GoBackend ;
 *   le système démarre ce service comme interface VPN. Ce service assure la
 *   notification de premier plan (type specialUse, Android 14+) et la révocation.
 *
 * Journalisation : états et erreurs uniquement — jamais de clé, jamais de contenu.
 */
class GodsonVpnService : VpnService() {

    override fun onCreate() {
        super.onCreate()
        createStatusChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                promoteToForeground(getString(R.string.notif_connecting))
                connectTunnel()
            }
            ACTION_DISCONNECT -> {
                stopTunnelAndCloseSession()
                tearDown()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Le système révoque le VPN (utilisateur ou politique) → retour honnête à Déconnecté. */
    override fun onRevoke() {
        stopTunnelAndCloseSession()
        tearDown()
        val container = (application as GodsonApplication).container
        container.vpnStateMachine.transitionTo(VpnConnectionState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun connectTunnel() {
        if (com.godson.vpn.data.AppMode.isDemo) {
            // Mode DÉMO : aucun tunnel n'est établi — aucun état trompeur n'est publié.
            Log.i(TAG, "Mode démo : aucune connexion réelle possible.")
            tearDown()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        // Mode RÉEL : l'établissement est l'affaire de VpnController (consentement
        // obtenu, session récupérée, GoBackend.setState). Ce service reste la
        // couche de premier plan demandée par Android pour l'interface VPN.
        promoteToForeground(getString(R.string.notif_connecting))
    }

    /** Fermeture propre : tunnel DOWN + révocation de la session distante. */
    private fun stopTunnelAndCloseSession() {
        val container = (application as GodsonApplication).container
        if (!com.godson.vpn.data.AppMode.isDemo) {
            MainScope().launch { container.vpnController.disconnectReal() }
        }
    }

    private fun tearDown() {
        // Purge des ressources locales du service (la config éphémère vit dans
        // VpnController et est purgée par disconnectReal()).
    }

    private fun promoteToForeground(message: String) {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createStatusChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_STATUS,
            getString(R.string.notif_channel_status),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_status_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "GodsonVpnService"
        const val ACTION_CONNECT = "com.godson.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.godson.vpn.action.DISCONNECT"
        const val CHANNEL_STATUS = "vpn_status"
        const val NOTIFICATION_ID = 41
    }
}
