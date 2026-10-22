# Taking payment for Ambr Pro

Ambr Pro is a one-time unlock (৳499) inside an Android app. Google Play requires digital unlocks like this to be sold through **Google Play Billing**, so that is the plan.

## Why Google Play Billing

- Required by Google Play policy for in-app digital goods. bKash, Nagad or card gateways cannot be used for this unlock in a Play Store app.
- No server needed: the app asks Google Play which purchases the account owns, which fits Ambr's offline, no-account design.
- Restore works on a new phone automatically, through the user's Google account.
- Google handles payment methods, receipts, refunds and local pricing in taka.

## What it costs

| Item | Cost |
| --- | --- |
| Billing library and integration | Free |
| Google Play developer account | One-time USD 25 |
| Google's service fee per sale | 15% of each sale (for a developer's first USD 1M a year) |

So on a ৳499 sale Ambr receives about ৳424 before local taxes. There is no monthly fee and no server bill.

## Setup steps

1. Create a Google Play developer account and a payments profile (needs a bank account for payouts).
2. In Play Console, add a one-time product `ambr_pro` priced at ৳499.
3. Integrate Play Billing Library in the app: connect, show the purchase sheet, acknowledge the purchase, and check owned purchases on launch for restore.
4. Test with Play Console license testers before launch (no real money moves).

## Optional later

- Server-side purchase verification adds fraud protection. It needs a small server and is not required to launch.
- Selling outside the Play Store (direct APK with bKash or SSLCommerz) would need our own server, licensing and a merchant account. Not recommended for v1.
