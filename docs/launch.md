# Launching Ambr on Google Play

You have the Play developer account. These are the steps, in order. Steps marked **(once)** are done one time only.

## 1. Make the upload key (once)

Google Play needs every update signed with the same "upload key". Ambr builds the signed file on GitHub, so the key is stored as GitHub secrets:

| Secret name | What it holds |
| --- | --- |
| `UPLOAD_KEYSTORE_BASE64` | the key file, base64 encoded |
| `UPLOAD_KEYSTORE_PASSWORD` | its password |
| `UPLOAD_KEY_ALIAS` | `upload` |
| `UPLOAD_KEY_PASSWORD` | the key's password |

Add them at GitHub → nsaifd/ambr → Settings → Secrets and variables → Actions → New repository secret. Keep a copy of the key file and passwords somewhere safe (Google Drive, password manager). If it is ever lost, Play support can reset it, but that takes days.

## 2. Build the release file

GitHub → Actions → **Release bundle** → Run workflow → type a version (first one: `1.0`) → Run. When it finishes, download **ambr-release-aab** from the run page. Inside is `app-release.aab`, the file Play wants.

## 3. Create the app in Play Console (once)

1. Play Console → **Create app**. Name: Ambr. Default language: English (United States). App, Free. Accept the declarations.
2. **Set up your app** checklist on the dashboard. Fill in each item:
   - **Privacy policy**: a public web link. Put the text of `docs/privacy-policy.md` on a page (Google Sites is free and quick) and paste the link. Add your support email to it first.
   - **App access**: All functionality is available without special access.
   - **Ads**: No ads.
   - **Content rating**: fill the questionnaire (Utility, no violence etc.).
   - **Target audience**: 18 and over (or 13+).
   - **News app**: No. **COVID-19**: No. **Government app**: No. **Financial features**: None.
   - **Data safety**: Data collected: none sent to us. Photos are processed on device. Data is encrypted in transit (Drive backup uses HTTPS). Users can request deletion (in app). Declare "Email address" as collected only if Google asks about the Drive account display; it is not shared.
3. **Store listing**: short description (80 chars), full description, app icon 512×512 PNG, feature graphic 1024×500, at least 2 phone screenshots. Category: Productivity.

## 4. Internal testing first

1. **Testing → Internal testing → Create new release**.
2. Accept **Play App Signing** (Google keeps the final signing key).
3. Upload `app-release.aab`, add release notes, **Save → Review release → Start rollout**.
4. **Testers** tab: add your Gmail (and friends'), copy the opt-in link, open it on your phone and install from Play.

## 5. Turn on Ambr Pro (once, after step 4)

Play only shows products for an app that already has a build uploaded.

1. **Monetize → Products → In-app products → Create product**. Product ID: `ambr_pro` (exactly this). Name: Ambr Pro. Price: BDT 499. **Activate**.
2. **Settings → License testing**: add your Gmail so test purchases are free.
3. Install the internal-testing build from Play and buy Pro to check it.

## 6. Turn on Google Drive backup (once)

The "can't connect" error happens because Google doesn't know the app yet. In [Google Cloud Console](https://console.cloud.google.com/):

1. Create a project "Ambr". **APIs & Services → Library → Google Drive API → Enable**.
2. **OAuth consent screen**: External, app name Ambr, your email, add scope `.../auth/drive.appdata`. Add your Gmail under Test users. When you go live, press **Publish app** (this scope doesn't need Google's review).
3. **Credentials → Create credentials → OAuth client ID → Android**, three times, all with package name `app.ambr`:
   - GitHub debug APK: SHA-1 `BB:24:09:4D:D7:C8:33:AB:BA:7B:41:DF:71:A5:5B:BA:9A:DC:13:C4`
   - Play build: SHA-1 from Play Console → **Test and release → App integrity → App signing key certificate**
   - Upload key: SHA-1 from the same page, **Upload key certificate**

Wait 5–10 minutes, then try Backup again.

## 7. Go live

When internal testing looks good: **Production → Create new release** → add the same (or a newer) `.aab` → choose countries (Bangladesh, plus any others) → **Start rollout**. New developer accounts may first need a **closed test with at least 12 testers for 14 days**; Play Console will say so on the Production page if it applies to you.

## Every later update

Run **Release bundle** again with a higher version (1.0.1, 1.1 …), then upload the new `.aab` to the track you want. The version code goes up by itself.
