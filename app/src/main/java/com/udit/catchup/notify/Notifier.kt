package com.udit.catchup.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.udit.catchup.MainActivity
import com.udit.catchup.R
import com.udit.catchup.data.Person

object Notifier {

    private const val CHANNEL_NUDGE = "nudge"
    private const val CHANNEL_NEW_CONTACTS = "new_contacts"
    private const val ID_NUDGE = 1001
    private const val ID_NEW_CONTACTS = 1002
    private const val ID_CYCLE_COMPLETE = 1003

    const val EXTRA_ROUTE = "route"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NUDGE, "Daily catch-up nudge", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "A daily reminder to call one of your friends" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_CONTACTS, "New contacts to file", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Fires when new contacts show up and need details filled in" }
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun showNudge(context: Context, person: Person) {
        if (!canNotify(context)) return
        val text = person.lastContactedAt
            ?.let { "Last caught up ${DateUtils.getRelativeTimeSpanString(it)}" }
            ?: "You two haven't caught up since you started tracking"
        val builder = NotificationCompat.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to catch up with ${person.displayName}")
            .setContentText(text)
            .setContentIntent(openAppIntent(context, "dashboard", 1))
            .setAutoCancel(true)
        if (!person.phoneNumber.isNullOrBlank()) {
            val call = PendingIntent.getActivity(
                context, 2,
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phoneNumber}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Call now", call)
        }
        NotificationManagerCompat.from(context).notify(ID_NUDGE, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun showNewContacts(context: Context, count: Int) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_CONTACTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (count == 1) "1 new contact to file" else "$count new contacts to file")
            .setContentText("Tap to add how you know them")
            .setContentIntent(openAppIntent(context, "triage", 3))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_NEW_CONTACTS, notification)
    }

    @SuppressLint("MissingPermission")
    fun showCycleComplete(context: Context) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("You've called everyone on your list 🎉")
            .setContentText("Open Orbit to start the next round")
            .setContentIntent(openAppIntent(context, "dashboard", 4))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID_CYCLE_COMPLETE, notification)
    }
}
