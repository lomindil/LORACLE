package com.example.loracle.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AlarmAgent {

    // Detect phrases like:
    // "set alarm for 7 am", "wake me at 6:30", "set an alarm for 10 pm"
    private val alarmRegex = Regex(
        "(set|wake|schedule).*alarm.*?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
        RegexOption.IGNORE_CASE
    )

    fun tryHandleAlarm(context: Context, userText: String): Boolean {
        println("Checking REGEX")
        val match = alarmRegex.find(userText) ?: return false
        print(match)

        val hour = match.groupValues[2].toInt()
        val minute = match.groupValues[3].ifBlank { "0" }.toInt()
        val ampm = match.groupValues[4]

        var h = hour
        // 12-hour to 24-hour conversion
        if (ampm.equals("pm", true) && hour < 12) h += 12
        if (ampm.equals("am", true) && hour == 12) h = 0

        println("Minute and hour:")
        println(h)
        println(minute)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // *** FIX 1: Ensure alarm is set for the future ***
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            // If the time is in the past (or now), set it for the next day
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        // *************************************************

        Log.d("AlarmAgent", "Alarm scheduled for ${cal.time.toString()}")
        
        setAlarm(context, cal)

        Log.d("AlarmAgent", "Alarm scheduled at $h:$minute $ampm")
        return true
    }

    private fun setAlarm(context: Context, time: Calendar) {
        // We use a request code based on time to allow multiple unique alarms
        val requestCode = time.timeInMillis.toInt() 
        
        val intent = Intent(context, AlarmReceiver::class.java)
        // Add a unique action to the intent for clarity, although not strictly needed for this simple case
        intent.action = "com.example.loracle.ALARM_ACTION" 
        
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode, // Use a unique request code
            intent,
            // FLAG_IMMUTABLE is required for API 23+
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // *** FIX 2: Check for Exact Alarm Permission on modern Android ***
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    time.timeInMillis,
                    pending
                )
            } else {
                // Cannot schedule exact alarm.
                // In a real app, you would prompt the user to grant this permission.
                Log.e("AlarmAgent", "Exact alarm permission denied on Android 12+.")
                // Fallback to inexact alarm if exact is not permitted (may be delayed)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    time.timeInMillis,
                    pending
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use setExactAndAllowWhileIdle for Doze mode on Android 6.0 (M) to 11 (R)
             alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.timeInMillis,
                pending
            )
        } else {
            // setExact is fine on older Android versions
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            time.timeInMillis,
            pending
        )
    }
        // ***************************************************************
    }
}