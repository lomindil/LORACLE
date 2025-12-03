// Inside com.example.loracle.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Acquire the WakeLock
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // Set a tag name for the lock for debugging
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loracle::AlarmWakeLock")
        wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */) // Acquire for a max time

        try {
            Log.i("AlarmReceiver", "Alarm Fired and WakeLock acquired!")
            
            // 2. Perform your alarm actions (e.g., show notification/start service)
            Toast.makeText(context, "⏰ WAKE UP! (Alarm Fired)", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Alarm error: ${e.message}")
        } finally {
            // 3. Release the WakeLock immediately after work is done
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.i("AlarmReceiver", "WakeLock released.")
            }
        }
    }
}