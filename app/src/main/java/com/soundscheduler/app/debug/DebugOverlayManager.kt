package com.soundscheduler.app.debug

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.soundscheduler.app.R
import com.soundscheduler.app.utils.PremiumManager
import com.soundscheduler.app.utils.UsageAnalyticsManager

object DebugOverlayManager {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    fun showOverlay(context: Context) {
        if (!PremiumManager.isPremium()) return

        if (overlayView == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_debug, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.END
            windowManager?.addView(overlayView, params)
        }

        val debugText = overlayView!!.findViewById<TextView>(R.id.debugTextView)
        debugText.text = UsageAnalyticsManager.getSummary(context)
    }

    fun hideOverlay(context: Context) {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    fun showToast(context: Context, message: String) {
        if (PremiumManager.isPremium()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}