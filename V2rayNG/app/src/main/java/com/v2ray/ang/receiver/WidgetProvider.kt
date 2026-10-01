package com.v2ray.ang.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.CoreServiceManager
import com.v2ray.ang.core.LauncherManager

class WidgetProvider : AppWidgetProvider() {
    /**
     * This method is called every time the widget is updated.
     * It updates the widget background based on the V2Ray service running state.
     *
     * @param context The Context in which the receiver is running.
     * @param appWidgetManager The AppWidgetManager instance.
     * @param appWidgetIds The appWidgetIds for which an update is needed.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetBackground(context, appWidgetManager, appWidgetIds, CoreServiceManager.isRunning())
    }

    /**
     * Updates the widget background based on whether the V2Ray service is running.
     *
     * @param context The Context in which the receiver is running.
     * @param appWidgetManager The AppWidgetManager instance.
     * @param appWidgetIds The appWidgetIds for which an update is needed.
     * @param isRunning Boolean indicating if the V2Ray service is running.
     */
    private fun updateWidgetBackground(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, isRunning: Boolean) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_switch)

        // Toggle service intent for the entire widget
        val toggleIntent = Intent(context, WidgetProvider::class.java).apply {
            action = AppConfig.BROADCAST_ACTION_WIDGET_CLICK
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        remoteViews.setOnClickPendingIntent(R.id.layout_switch, togglePendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.layout_background, togglePendingIntent)

        // Get currently selected server remarks or app name
        val selectedGuid = com.v2ray.ang.handler.MmkvManager.getSelectServer()
        val serverConfig = selectedGuid?.let { com.v2ray.ang.handler.MmkvManager.decodeServerConfig(it) }
        val serverName = serverConfig?.remarks?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)

        if (isRunning) {
            remoteViews.setTextViewText(R.id.text_server, serverName)
            remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_stop_24dp)
            remoteViews.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.widget_power_active)
        } else {
            remoteViews.setTextViewText(R.id.text_server, context.getString(R.string.app_name))
            remoteViews.setInt(R.id.image_switch, "setImageResource", R.drawable.ic_play_24dp)
            remoteViews.setInt(R.id.layout_background, "setBackgroundResource", R.drawable.widget_power_inactive)
        }

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     * It handles widget click actions and updates the widget background based on the V2Ray service state.
     *
     * @param context The Context in which the receiver is running.
     * @param intent The Intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (AppConfig.BROADCAST_ACTION_WIDGET_CLICK == intent.action) {
            if (CoreServiceManager.isRunning()) {
                LauncherManager.stopService(context)
            } else {
                LauncherManager.startServiceFromToggle(context)
            }
        } else if (AppConfig.BROADCAST_ACTION_ACTIVITY == intent.action) {
            AppWidgetManager.getInstance(context)?.let { manager ->
                when (intent.getIntExtra("key", 0)) {
                    AppConfig.MSG_STATE_RUNNING, AppConfig.MSG_STATE_START_SUCCESS -> {
                        updateWidgetBackground(
                            context, manager, manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java)),
                            true
                        )
                    }

                    AppConfig.MSG_STATE_NOT_RUNNING, AppConfig.MSG_STATE_START_FAILURE, AppConfig.MSG_STATE_STOP_SUCCESS -> {
                        updateWidgetBackground(
                            context, manager, manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java)),
                            false
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun updateWidget(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val provider = WidgetProvider()
                provider.updateWidgetBackground(context, manager, ids, CoreServiceManager.isRunning())
            }
        }
    }
}
