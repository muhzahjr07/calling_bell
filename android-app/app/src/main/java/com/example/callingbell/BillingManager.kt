package com.example.callingbell

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.callingbell.data.SharedPrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val prefs: SharedPrefsManager
) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "BillingManager"
        const val PRO_PRODUCT_ID = "callingbell_pro_lifetime"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _proPrice = MutableStateFlow<String?>("$2.99")
    val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    private val _billingStatus = MutableStateFlow<String>("Ready")
    val billingStatus: StateFlow<String> = _billingStatus.asStateFlow()

    private var productDetails: ProductDetails? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        startConnection()
    }

    fun startConnection() {
        _billingStatus.value = "Connecting..."
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient connected successfully.")
                    _billingStatus.value = "Connected"
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingStatus.value = "Setup Failed: ${billingResult.responseCode}"
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Retrying...")
                _billingStatus.value = "Disconnected"
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (queryProductDetailsList.isNotEmpty()) {
                    productDetails = queryProductDetailsList[0]
                    val price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) {
                        _proPrice.value = price
                    }
                    Log.d(TAG, "Fetched Product Details: ${productDetails?.title}, Price: $price")
                } else {
                    Log.w(TAG, "No product details found for $PRO_PRODUCT_ID")
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val details = productDetails
        if (details == null) {
            // Fallback for dev / unconfigured Play Console environment
            Log.w(TAG, "ProductDetails null. Activating PRO locally for testing/dev environment.")
            prefs.isPro = true
            _billingStatus.value = "PRO Activated (Development Mode)"
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            _billingStatus.value = "Checkout Opened"
        } else {
            _billingStatus.value = "Error launching checkout: $responseCode"
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _billingStatus.value = "Purchase Canceled"
        } else {
            _billingStatus.value = "Purchase Error: ${billingResult.responseCode}"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            prefs.isPro = true
            _billingStatus.value = "PRO Unlocked!"
            Log.d(TAG, "Purchase successful: ${purchase.orderId}")

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully.")
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${ackResult.debugMessage}")
                    }
                }
            }
        }
    }

    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundPro = false
                for (purchase in purchasesList) {
                    if (purchase.products.contains(PRO_PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        foundPro = true
                        handlePurchase(purchase)
                    }
                }
                if (foundPro) {
                    _billingStatus.value = "Purchases Restored! PRO Active."
                }
            }
        }
    }
}
