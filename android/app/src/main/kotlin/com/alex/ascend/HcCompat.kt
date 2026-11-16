package com.alex.ascend

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Health Connect through Google's androidx library – used on Android 9–13, where Health Connect
 * is a separate app (Samsung Health syncs into it). Android 14+ uses the built-in version (Health.java).
 */
object HcCompat {
    private const val HC_PACKAGE = "com.google.android.apps.healthdata"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
    private val EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val SLEEP = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val WATER = HealthPermission.getReadPermission(HydrationRecord::class)
    private val PERMS = setOf(STEPS, EXERCISE, SLEEP, WATER)

    /** "ok", "install" (Health Connect app missing) or "update" (needs an update). */
    @JvmStatic
    fun availability(ctx: Context): String = when (HealthConnectClient.getSdkStatus(ctx, HC_PACKAGE)) {
        HealthConnectClient.SDK_AVAILABLE -> "ok"
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "update"
        else -> "install"
    }

    /** Intent that opens Health Connect's permission screen (use with startActivityForResult). */
    @JvmStatic
    fun permissionIntent(ctx: Context): Intent =
        PermissionController.createRequestPermissionResultContract(HC_PACKAGE).createIntent(ctx, PERMS)

    /** Opens the Play Store page for Health Connect. */
    @JvmStatic
    fun openStore(ctx: Context) {
        val uri = Uri.parse("market://details?id=$HC_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
        val i = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(i) } catch (e: Exception) {
            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$HC_PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Reads today's data and sends it to the page. */
    @JvmStatic
    fun read(act: MainActivity) {
        val avail = availability(act)
        if (avail != "ok") { send(act, JSONObject().put("status", avail)); return }
        scope.launch {
            val out = JSONObject()
            try {
                val client = HealthConnectClient.getOrCreate(act, HC_PACKAGE)
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.intersect(PERMS).isEmpty()) { send(act, JSONObject().put("status", "denied")); return@launch }
                val z = ZoneId.systemDefault()
                val today = LocalDate.now(z)
                val now = Instant.now()
                val day = TimeRangeFilter.between(today.atStartOfDay(z).toInstant(), now)
                out.put("status", "ok").put("date", today.toString())

                if (STEPS in granted) runCatching {
                    val r = client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), day))
                    out.put("steps", r[StepsRecord.COUNT_TOTAL] ?: 0L)
                }
                if (EXERCISE in granted) runCatching {
                    val r = client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), day))
                    out.put("exerciseMin", (r[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL] ?: Duration.ZERO).toMinutes())
                }
                if (WATER in granted) runCatching {
                    val r = client.aggregate(AggregateRequest(setOf(HydrationRecord.VOLUME_TOTAL), day))
                    out.put("waterL", r[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0)
                }
                if (SLEEP in granted) runCatching {
                    val nightStart = ZonedDateTime.of(today.minusDays(1), LocalTime.of(15, 0), z).toInstant()
                    val recs = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(nightStart, now))).records
                    val longest = recs.maxByOrNull { Duration.between(it.startTime, it.endTime) }
                    if (longest != null) {
                        val t = longest.startTime.atZone(z).toLocalTime()
                        out.put("sleepStart", String.format(Locale.US, "%02d:%02d", t.hour, t.minute))
                        out.put("sleepHours", Math.round(Duration.between(longest.startTime, longest.endTime).toMinutes() / 6.0) / 10.0)
                    }
                }
                send(act, out)
            } catch (e: Exception) {
                send(act, JSONObject().put("status", "error"))
            }
        }
    }

    private fun send(act: MainActivity, json: JSONObject) {
        act.runJs("window.ascendHealth&&ascendHealth(" + JSONObject.quote(json.toString()) + ")")
    }
}
