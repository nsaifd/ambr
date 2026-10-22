package app.ambr.platform

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PurchaseState { Idle, Buying, Pending, Success, Failed, Unavailable }

/**
 * Ambr Pro is one Google Play in-app product, bought once. No server: Play tells us what the
 * account owns, which also restores Pro on a new phone. See docs/payments.md.
 */
class Billing(context: Context, private val settings: Settings) : BillingClientStateListener {
    companion object { const val PRODUCT_ID = "ambr_pro" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PurchaseState.Idle)
    val state: StateFlow<PurchaseState> = _state.asStateFlow()
    private val _price = MutableStateFlow<String?>(null)
    /** Localised price from Play, e.g. "BDT 499.00". Null until loaded. */
    val price: StateFlow<String?> = _price.asStateFlow()
    private var product: ProductDetails? = null

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchases(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() { if (!client.isReady) client.startConnection(this) }

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode != BillingResponseCode.OK) { _state.value = PurchaseState.Unavailable; return }
        if (_state.value == PurchaseState.Unavailable) _state.value = PurchaseState.Idle
        scope.launch { loadProduct(); restore() }
    }

    override fun onBillingServiceDisconnected() = Unit

    private suspend fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build(),
        )).build()
        product = client.queryProductDetails(params).productDetailsList?.firstOrNull()
        _price.value = product?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    /** Checks what this Google account already owns. Also the "Restore purchase" button. */
    fun restore() = scope.launch {
        if (!client.isReady) { connect(); return@launch }
        val owned = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        val pro = owned.purchasesList.firstOrNull { PRODUCT_ID in it.products }
        when (pro?.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> grant(pro)
            Purchase.PurchaseState.PENDING -> _state.value = PurchaseState.Pending
            else -> Unit
        }
    }

    fun buy(activity: Activity) {
        val details = product
        if (!client.isReady || details == null) { _state.value = PurchaseState.Unavailable; connect(); return }
        _state.value = PurchaseState.Buying
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build(),
        )).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) _state.value = PurchaseState.Failed
    }

    fun reset() { if (_state.value != PurchaseState.Pending) _state.value = PurchaseState.Idle }

    private fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().filter { PRODUCT_ID in it.products }.forEach { p ->
                when (p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> scope.launch { grant(p) }
                    Purchase.PurchaseState.PENDING -> _state.value = PurchaseState.Pending
                    else -> Unit
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            BillingResponseCode.USER_CANCELED -> _state.value = PurchaseState.Idle
            else -> _state.value = PurchaseState.Failed
        }
    }

    private suspend fun grant(p: Purchase) {
        // Play refunds purchases that are not acknowledged within three days.
        if (!p.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        val wasPro = settings.isPro.value
        settings.setPro(true)
        if (!wasPro || _state.value == PurchaseState.Buying || _state.value == PurchaseState.Pending) _state.value = PurchaseState.Success
    }
}
