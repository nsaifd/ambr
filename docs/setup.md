# Turning on Google Drive backup and Ambr Pro

The app builds and runs without these steps. Backup and the Pro purchase need two one-time setups in Google's consoles.

## 1. Google Drive backup (free)

Ambr asks for the `drive.appdata` scope, a private folder in the user's own Drive. Google only grants it to apps it knows.

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create a project named "Ambr".
2. Go to **APIs & Services → Library**, search for **Google Drive API** and click **Enable**.
3. Go to **APIs & Services → OAuth consent screen**. Choose **External**, fill in the app name (Ambr) and your email, and add the scope `.../auth/drive.appdata`. While testing, add your own Gmail under **Test users**.
4. Go to **APIs & Services → Credentials → Create credentials → OAuth client ID**, type **Android**:
   - Package name: `app.ambr`
   - SHA-1 for the debug APK from GitHub Actions: `BB:24:09:4D:D7:C8:33:AB:BA:7B:41:DF:71:A5:5B:BA:9A:DC:13:C4`
   (from `app/debug.keystore`, which is committed so this never changes)
5. For the Play Store release, add a second Android client with the SHA-1 shown in **Play Console → Setup → App integrity → App signing key certificate**.

No client ID goes into the code; Google matches the package name and SHA-1.

## 2. Ambr Pro (Google Play Billing)

See `docs/payments.md` for why Play Billing. Steps:

1. Create a Google Play developer account (one-time USD 25) and a payments profile.
2. Create the app `app.ambr` in Play Console and upload a release build to the **Internal testing** track. Play only shows products for apps it has seen.
3. **Monetize → Products → In-app products → Create product**: ID `ambr_pro`, name "Ambr Pro", price ৳499. Activate it.
4. **Setup → License testing**: add your Gmail so test purchases cost nothing.

Until then, debug builds show "Unlock Pro (debug build only)" on the Pro screen so every Pro feature can be tried.

## Support email

`SUPPORT_EMAIL` in `app/src/main/java/app/ambr/ui/you/InfoScreens.kt` is blank. Set it to the address that should receive feedback.
