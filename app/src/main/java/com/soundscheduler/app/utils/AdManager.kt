package com.soundscheduler.app.utils

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

object AdManager {

    private var adView: AdView? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }

    fun loadBannerAd(activity: Activity, container: FrameLayout) {
        if (PremiumManager.isPremium()) return

        adView = AdView(activity)
        adView?.adUnitId = "ca-app-pub-3940256099942544/6300978111" // test ad unit ID
        adView?.setAdSize(com.google.android.gms.ads.AdSize.BANNER)
        container.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)
    }

    fun showAd(activity: Activity) {
        if (PremiumManager.isPremium()) return
        // You may integrate interstitials here if desired
    }

    fun destroy() {
        adView?.destroy()
    }
}