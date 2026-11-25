package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.EmergencyReport
import com.example.data.model.EmergencyType
import com.example.data.model.LocationSnapshot
import com.example.data.model.ReportStatus
import com.example.data.model.SupportedCountry

@Composable
fun OsmEmergencyMapView(
    country: SupportedCountry,
    reports: List<EmergencyReport>,
    userGps: LocationSnapshot?,
    focusPoint: Pair<Double, Double>?,
    focusZoom: Double = 11.5,
    onMarkerSelected: (EmergencyReport) -> Unit,
    modifier: Modifier = Modifier,
    photoMarker: Pair<Double, Double>? = null,
    photoMarkerLabel: String? = null
) {
    MapViewWrapper(
        country = country,
        reports = reports,
        userGps = userGps,
        focusPoint = focusPoint,
        focusZoom = focusZoom,
        enableLocationOverlay = photoMarker == null,
        showPermissionOverlay = false,
        onMarkerSelected = onMarkerSelected,
        photoMarker = photoMarker,
        photoMarkerLabel = photoMarkerLabel,
        modifier = modifier
    )
}

internal fun createUserGpsDrawable(context: Context): BitmapDrawable {
    val size = (28 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f
    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(75, 56, 189, 248)
    }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(14, 165, 233)
    }
    canvas.drawCircle(cx, cy, size * 0.45f, halo)
    canvas.drawCircle(cx, cy, size * 0.28f, border)
    canvas.drawCircle(cx, cy, size * 0.20f, dot)
    return BitmapDrawable(context.resources, bmp)
}

internal fun createPhotoPinDrawable(context: Context): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (40 * d).toInt().coerceAtLeast(64)
    val h = (48 * d).toInt().coerceAtLeast(76)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(250, 204, 21) }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(10, 22, 40)
        style = Paint.Style.STROKE
        strokeWidth = w * 0.06f
    }
    val cx = w / 2f
    val r = w * 0.36f
    canvas.drawCircle(cx, r + stroke.strokeWidth, r, fill)
    canvas.drawCircle(cx, r + stroke.strokeWidth, r, stroke)
    val path = Path().apply {
        moveTo(cx - r * 0.55f, r * 1.4f)
        lineTo(cx, h.toFloat() - 2f)
        lineTo(cx + r * 0.55f, r * 1.4f)
        close()
    }
    canvas.drawPath(path, fill)
    val camPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(10, 22, 40) }
    canvas.drawRect(cx - r * 0.45f, r * 0.72f, cx + r * 0.45f, r * 1.28f, camPaint)
    return BitmapDrawable(context.resources, bmp)
}

internal fun createEmergencyMarkerDrawable(
    context: Context,
    type: EmergencyType,
    status: ReportStatus,
    isTest: Boolean
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val w = (46 * d).toInt().coerceAtLeast(72)
    val h = (56 * d).toInt().coerceAtLeast(88)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgColor = when {
        status != ReportStatus.ACTIVE -> android.graphics.Color.rgb(100, 116, 139)
        type == EmergencyType.RESCUE -> android.graphics.Color.rgb(220, 38, 38)
        type == EmergencyType.ROBBERY -> android.graphics.Color.rgb(185, 28, 28)
        type == EmergencyType.KIDNAPPING -> android.graphics.Color.rgb(234, 88, 12)
        else -> android.graphics.Color.rgb(220, 38, 38)
    }

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isTest) android.graphics.Color.rgb(250, 204, 21) else android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = w * 0.065f
    }

    val cx = w / 2f
    val r = w * 0.40f
    val cy = r + borderPaint.strokeWidth

    // Pin pointer
    val pointer = Path().apply {
        moveTo(cx - r * 0.55f, cy + r * 0.65f)
        lineTo(cx, h - 2f)
        lineTo(cx + r * 0.55f, cy + r * 0.65f)
        close()
    }
    canvas.drawPath(pointer, fillPaint)
    canvas.drawCircle(cx, cy, r, fillPaint)
    canvas.drawCircle(cx, cy, r, borderPaint)

    val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(10, 22, 40)
        style = Paint.Style.FILL
    }

    when (type) {
        EmergencyType.RESCUE -> {
            // RED PERSON SYMBOL: crisp white human silhouette inside red pin
            canvas.drawCircle(cx, cy - r * 0.34f, r * 0.22f, iconPaint)
            val body = RectF(cx - r * 0.34f, cy - r * 0.05f, cx + r * 0.34f, cy + r * 0.50f)
            canvas.drawRoundRect(body, r * 0.22f, r * 0.22f, iconPaint)
        }
        EmergencyType.ROBBERY -> {
            // MASK SYMBOL: bandit/domino mask with eye cutouts
            val maskRect = RectF(cx - r * 0.58f, cy - r * 0.26f, cx + r * 0.58f, cy + r * 0.26f)
            canvas.drawRoundRect(maskRect, r * 0.22f, r * 0.22f, darkPaint)
            canvas.drawCircle(cx - r * 0.24f, cy, r * 0.14f, iconPaint)
            canvas.drawCircle(cx + r * 0.24f, cy, r * 0.14f, iconPaint)
        }
        EmergencyType.KIDNAPPING -> {
            // CAR + WARNING SYMBOL: car silhouette with yellow warning badge
            val roof = RectF(cx - r * 0.36f, cy - r * 0.32f, cx + r * 0.36f, cy + r * 0.04f)
            canvas.drawRoundRect(roof, r * 0.12f, r * 0.12f, iconPaint)
            val chassis = RectF(cx - r * 0.54f, cy - r * 0.06f, cx + r * 0.54f, cy + r * 0.32f)
            canvas.drawRoundRect(chassis, r * 0.12f, r * 0.12f, iconPaint)
            canvas.drawCircle(cx - r * 0.30f, cy + r * 0.34f, r * 0.13f, darkPaint)
            canvas.drawCircle(cx + r * 0.30f, cy + r * 0.34f, r * 0.13f, darkPaint)
            val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(250, 204, 21)
                textAlign = Paint.Align.CENTER
                textSize = r * 0.55f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("!", cx, cy + r * 0.22f, warnPaint)
        }
    }

    if (isTest) {
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(250, 204, 21)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(10, 22, 40)
            textAlign = Paint.Align.CENTER
            textSize = w * 0.20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val pill = RectF(w * 0.12f, 0f, w * 0.88f, h * 0.23f)
        canvas.drawRoundRect(pill, 8f, 8f, badgePaint)
        canvas.drawText("TEST", cx, h * 0.18f, textPaint)
    }

    return BitmapDrawable(context.resources, bmp)
}
