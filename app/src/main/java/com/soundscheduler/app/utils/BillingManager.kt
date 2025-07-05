package com.soundscheduler.app.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private var billingClient: BillingClient

    init {
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle reconnection strategy if needed
            }
        })
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val skuList = listOf(productId)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList[0])
                    .build()
                billingClient.launchBillingFlow(activity, billingFlowParams)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                unlockPremium()
                            }
                        }
                    } else {
                        unlockPremium()
                    }
                }
            }
        }
    }

    private fun unlockPremium() {
        PremiumManager.setPremium(true)
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { _, purchasesList ->
            var premiumFound = false
            purchasesList.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    premiumFound = true
                    unlockPremium()
                }
            }
            if (!premiumFound) {
                PremiumManager.setPremium(false)
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}