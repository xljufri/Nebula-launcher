package com.nebula.launcher.feature.home.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import com.nebula.launcher.core.model.RadialAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ShortcutRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getShortcutsForPackage(packageName: String): List<RadialAction> {
        val actions = mutableListOf<RadialAction>()

        actions.add(
            RadialAction(
                id = "info",
                label = "Info",
                icon = Icons.Default.Info,
                action = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        )

        actions.add(
            RadialAction(
                id = "uninstall",
                label = "Uninstall",
                icon = Icons.Default.Delete,
                action = {
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )
        )

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            if (launcherApps.hasShortcutHostPermission()) {
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
                }
                val userHandle = Process.myUserHandle()
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                
                shortcuts?.take(4)?.forEach { shortcut ->
                    actions.add(
                        RadialAction(
                            id = shortcut.id,
                            label = shortcut.shortLabel?.toString() ?: "Shortcut",
                            icon = Icons.Default.Star,
                            action = {
                                launcherApps.startShortcut(shortcut, null, null)
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore if no permission or error
        }

        return actions
    }
}
