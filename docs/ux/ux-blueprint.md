# Ambr UX Blueprint

Screens, information architecture and user flows for Ambr v1, worked out from the people who use it and the moments they use it in. Source spec: [Ambr — Product & UX Spec](https://claude.ai/artifact/7edoErE1TBBWBFKhYfXnW9).

Design direction so far: light mode only, Public Sans (English UI), amber brand. Visual style and logo to come.

## 1. Starting point: people and moments

| Person | What they bring | What the UI must do for them |
| --- | --- | --- |
| Nusrat, household keeper (primary) | Saves most items, gets blamed when proof is lost | Fast capture, one place for everything, calm reminders |
| Rafi, gadget buyer | Many small electronics, cares about resale | Serial numbers, proof for claims, sold/given-away archive |
| Abba, occasional user (62) | Rarely opens the app, only on breakdown day | Big targets, no typing needed, one plain answer: covered or not |

Ambr is used in six moments. Each screen exists to serve one of them.

| Moment | Feeling | Time budget | Screens that serve it |
| --- | --- | --- | --- |
| Buying | Excited, in a hurry at the counter | 15 s to save | Camera, Confirm, Saved |
| Quiet months | Forgets the app, and that is fine | 0 s | None. No nagging |
| Near expiry | Unaware | 1 tap | Reminder, Item detail with banner |
| Breakdown day | Panic, blame | 10 s to the answer | Home, "What broke?", Search, Item detail |
| At the service center | Stressed, standing at a counter | 1 tap to show | Claim pack, Show at counter, Share PDF |
| After the repair, or a new phone | Relief, then forgetting | 30 s | Log repair, Backup, Restore |

## 2. UX decisions that change the spec

The spec is a strong base. These changes come from looking at each moment through Abba's and Nusrat's eyes.

1. **"Something broke?" button on Home.** Search assumes the person can type and spell the item. Abba may not. A large button on Home opens a photo grid of everything owned; tap the item and the claim pack opens. Search stays for people who prefer typing.
2. **Breakdown path skips Item detail when it can.** From "Something broke?" the person lands on the claim pack directly, with the coverage answer at its top. Item detail stays the route from Vault and Search.
3. **Camera has a "Type it in" exit.** Not every purchase has a receipt to hand. The camera shows "No receipt? Type it in" so no one is stuck.
4. **Add coverage is a sheet, not a form page.** Most items have one warranty. A second one (compressor 10 years, 4 free services) is added from a bottom sheet so the main confirm screen stays short.
5. **Duplicate check happens before save, as a dialog.** One question, two answers. Never a separate screen.
6. **Restore is offered before Welcome**, only when a Drive backup is found, so a person on a new phone never re-enters 23 items by hand.
7. **Free limit never touches an existing claim.** Item 11 opens the Pro sheet; every saved item keeps its claim pack, share and call.
8. **Notification permission is asked after the first save**, not during onboarding. At that moment the reason ("we'll remind you on 22 Sep") is concrete.
9. **Archive replaces Delete as the default end of life.** "Sold or gave away" keeps the receipt for resale proof; Delete stays in the overflow with a plain warning.

## 3. Screen inventory

36 screens, sheets and dialogs. IDs are used in the flows and in code. `v1` ships at launch; `v2` comes later.

### A. Start
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| A1 | Splash | Screen | Logo while the database opens, under 1 s | v1 |
| A3 | Restore found | Screen | "Restore 23 items from your Google Drive?" Only when a backup exists | v1 |
| A4 | Welcome | Screen | One promise, one action: "Save my first receipt" | v1 |
| A5 | First save celebration | Sheet | "Your fridge is protected until Jan 2028" + reminder permission ask | v1 |
| A6 | App lock | Screen | Fingerprint or PIN gate, only if turned on | v1 (Pro) |

### B. Capture
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| B1 | Camera | Screen | Edge-detected capture, auto shutter, gallery/PDF import, "Type it in" | v1 |
| B2 | Pages review | Screen | Crop, reorder or retake pages before OCR | v1 |
| B3 | Confirm details | Screen | Pre-filled fields, "Check this" on unsure ones, sticky Save | v1 |
| B4 | Add coverage | Sheet | Second warranty: type, length, starts from, parts or free-service count | v1 |
| B5 | Barcode scan | Screen | Scan serial number into B3 | v1 |
| B6 | Duplicate found | Dialog | "Looks like you already saved this receipt on 2 Jun. Save anyway?" | v1 |
| B7 | Saved | Sheet | "Fridge saved. Covered until 22 Oct 2027." Add another / Done | v1 |

### C. Find
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| C1 | Home | Tab | Is anything at risk? Search, "Something broke?", needs attention, protected total | v1 |
| C2 | Search | Screen | Results with status pills; "Not saved yet? Add it" when empty | v1 |
| C3 | What broke? | Screen | Big photo grid of items, tap one to open its claim pack | v1 |
| C4 | Vault | Tab | Everything owned, by room or category, filters and sort | v1 |
| C5 | Archive | Screen | Sold and given-away items, receipts kept | v1 |

### D. Item
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| D1 | Item detail | Screen | Status sentence first, then "Something broke", proof, coverage, details, service history | v1 |
| D2 | Edit item | Screen | Same fields as B3, opened from D1 | v1 |
| D3 | Proof viewer | Screen | Full-screen receipt and card pages, pinch zoom, add page | v1 |
| D4 | Mark as sold | Dialog | Move to Archive with date, keep receipt | v1 |
| D5 | Delete | Dialog | "Delete Fridge and its receipt? You can't undo this." | v1 |

### E. Claim
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| E1 | Claim pack | Screen | Status, receipt, card, serial in large type, store, dates, Call service center | v1 |
| E2 | Show at counter | Screen | Full screen, max brightness, receipt + serial, swipe pages | v1 |
| E3 | Share PDF | Sheet | Preview of the one-page PDF, then Android share (WhatsApp, Messenger, email) | v1 |
| E4 | Log a repair | Screen | Date, what went wrong, outcome, cost, photo | v1 |

### F. You
| ID | Screen | Type | Purpose | Release |
| --- | --- | --- | --- | --- |
| F1 | You | Tab | Backup status on top, then settings list | v1 |
| F2 | Backup & restore | Screen | Google Drive sign-in, Back up now, last backup time, Restore | v1 |
| F3 | Reminders | Screen | Lead time (7/14/30/60 days), quiet hours | v1 |
| F4 | App lock setup | Screen | Turn on fingerprint or PIN | v1 (Pro) |
| F5 | Ambr Pro | Sheet | One-time ৳499; what you get; never locks old items | v1 |
| F6 | Export all | Screen | PDF or CSV of every item | v1 (Pro) |
| F7 | Privacy | Screen | Plain-words promise: data stays on this phone | v1 |
| F8 | Help & feedback | Screen | Short FAQ and a feedback form | v1 |
| F9 | Household | Screen | Invite by QR, members, "Added by" | v2 |

System surfaces: expiry notification (30 and 7 days), home-screen "Ending soon" widget (v2).

## 4. Information architecture

Three tabs and one Add button. Every breakdown-day answer is at most two taps from launch.

```mermaid
flowchart TD
    Launch([Launch]) --> Lock{App lock on?}
    Lock -- yes --> A6[A6 App lock] --> Home
    Lock -- no --> Home

    subgraph Nav[Bottom bar]
        Home[C1 Home]
        Add((Add))
        Vault[C4 Vault]
        You[F1 You]
    end

    Home --> Search[C2 Search]
    Home --> Broke[C3 What broke?]
    Home --> Attention[Needs attention list]
    Vault --> Archive[C5 Archive]

    Search --> Detail[D1 Item detail]
    Attention --> Detail
    Vault --> Detail
    Archive --> Detail
    Broke --> Claim[E1 Claim pack]

    Detail --> Claim
    Detail --> Proof[D3 Proof viewer]
    Detail --> Edit[D2 Edit item]
    Detail --> Repair[E4 Log a repair]
    Claim --> Counter[E2 Show at counter]
    Claim --> Share[E3 Share PDF]
    Claim --> Repair

    Add --> Camera[B1 Camera] --> Pages[B2 Pages review] --> Confirm[B3 Confirm details]
    Camera -- Type it in --> Confirm
    Confirm --> Saved[B7 Saved]

    You --> Backup[F2 Backup & restore]
    You --> Rem[F3 Reminders]
    You --> LockSet[F4 App lock setup]
    You --> Pro[F5 Ambr Pro]
    You --> Export[F6 Export all]
    You --> Privacy[F7 Privacy]
    You --> Help[F8 Help]
```

### What each tab answers

| Tab | Question | Contents, top to bottom |
| --- | --- | --- |
| Home | "Is anything at risk? Where is X?" | Search bar · "Something broke?" button · Needs attention (≤ 30 days) or "All good" · Protected total (18 items · ৳4,85,000) · Recently added · Backup card when off |
| Vault | "Show me everything I own" | By room / By category toggle · Filters: All, Covered, Ends soon, Expired · Item grid · Archive link |
| You | "Is my data safe?" | Backup status · Reminders · App lock · Ambr Pro · Privacy · Export · Help |

### Content model

| Object | Key fields | Notes |
| --- | --- | --- |
| Item | name*, category, room, brand, model, serial, price, purchase date, store, photo, status (active, archived) | Only name is required |
| Proof | item, kind (receipt, card, invoice), pages[], OCR text | Many per item; OCR text indexed for search |
| Coverage | item, type (manufacturer, shop, extended, free services), starts from (purchase, delivery, custom), start, end, scope (parts, service, both), services used / total | Many per item; status uses the longest active one |
| Service contact | brand, center name, phone, address | Pre-filled for known brands |
| Repair | item, date, problem, outcome, cost, notes, photo | Builds service history |
| Reminder | coverage, fire date, kind (30 d, 7 d), state (pending, sent, dismissed) | Max 2 per coverage |

Status rule: **Covered** when the longest active coverage ends in more than 30 days, **Ends soon** within 30 days, **Expired** when none is active, **No proof yet** shown as a note, never as a status.

## 5. User flows

### Flow 1: First launch (target: first item saved in under 60 s)

```mermaid
flowchart TD
    A1[A1 Splash]
    A2 --> Found{Drive backup found?}
    Found -- yes --> A3[A3 Restore found]
    A3 -- Restore --> Home[C1 Home with items]
    A3 -- Start fresh --> A4
    Found -- no --> A4[A4 Welcome]
    A4 -- Save my first receipt --> B1[B1 Camera]
    A4 -- I'll explore first --> Empty[C1 Home, empty state]
    B1 --> B3[B3 Confirm details] --> A5[A5 Celebration + reminder permission]
    A5 --> Home2[C1 Home]
```

Rules: no account, no email. Camera permission is asked only when the camera opens. Notification permission is asked on A5, with the real reminder date in the sentence.

### Flow 2: Add an item (target: under 15 s)

```mermaid
flowchart TD
    Start((Add)) --> B1[B1 Camera]
    B1 -- Shutter or auto --> More{More pages?}
    More -- Add page --> B1
    More -- Done --> B2[B2 Pages review]
    B1 -- Gallery / PDF --> B2
    B1 -- No receipt? Type it in --> B3
    B2 --> OCR[On-device OCR, 1-2 s]
    OCR -- Fields found --> B3[B3 Confirm details]
    OCR -- Nothing readable --> B3e[B3 empty, photo pinned, cursor in Name]
    B3e --> B3
    B3 -- optional --> B4[B4 Add coverage]
    B3 -- optional --> B5[B5 Barcode scan]
    B3 -- Save --> Dup{Same receipt saved before?}
    Dup -- yes --> B6[B6 Duplicate found]
    B6 -- Save anyway --> B7
    B6 -- Cancel --> B3
    Dup -- no --> Limit{11th item on Free?}
    Limit -- yes --> F5[F5 Ambr Pro]
    Limit -- no --> B7[B7 Saved sheet]
    B7 -- Add another --> B1
    B7 -- Done / 4 s --> Back[Back where Add was tapped]
```

Short path: Add → shutter → Done → Save → Done. Five taps.

### Flow 3: Breakdown day (target: answer in under 10 s)

```mermaid
flowchart TD
    Open[Open Ambr] --> C1[C1 Home]
    C1 -- Something broke? --> C3[C3 What broke? photo grid]
    C1 -- types AC / এসি --> C2[C2 Search]
    C3 -- tap item --> E1[E1 Claim pack]
    C2 -- tap item --> D1[D1 Item detail]
    D1 -- Something broke --> E1
    E1 --> State{Covered?}
    State -- Covered / Ends soon --> Act[Call · Share PDF · Show at counter]
    State -- Expired --> Kind["Warranty ended on 3 Jan. You may still get a paid repair." Call stays visible]
    Act --> E2[E2 Show at counter]
    Act --> E3[E3 Share PDF]
    Act --> Visit[Service visit]
    Kind --> Visit
    Visit --> Ask["Next open: How did it go?"] --> E4[E4 Log a repair] --> D1b[D1 with service history]
```

### Flow 4: Expiry reminder

```mermaid
flowchart TD
    T30[30 days before end] --> N1[Notification: Your AC warranty ends on 14 Mar. Anything not working right?]
    N1 -- tap --> D1[D1 Item detail + expiry banner]
    D1 -- Book a check-up --> Call[Calls service center]
    D1 -- All good --> Done[No more reminders for this coverage]
    N1 -- ignored --> T7[7 days before end]
    T7 --> N2[Second and last notification]
    N2 --> D1
    End[End date] --> Silent[Status turns Expired silently]
```

Max 2 notifications per coverage, ever. Lead time is changeable in F3.

### Flow 5: Backup and restore

```mermaid
flowchart TD
    I3[3rd item saved] --> Card[C1 Home backup card]
    I10[10th item saved, still off] --> Card
    Card -- Back up --> G[Google sign-in, pick account] --> F2[F2 Backup on, weekly on Wi-Fi]
    Card -- Not now --> Hide[Card hides until item 10]
    F2 --> Status["F1 shows: Last backup today, 9:14 pm"]
    New[New phone, first launch] --> A3[A3 Restore found] --> Home[C1 Home with all items]
```

### Flow 6: Share with family

```mermaid
flowchart TD
    Q["Family asks: where's the receipt?"] --> Find[C2 Search or C4 Vault] --> D1[D1 Item detail]
    D1 -- Share receipt --> E3[E3 Share PDF] --> App[WhatsApp / Messenger / email]
    V2[v2: F9 Household] --> QR[Invite by QR] --> Same[Everyone sees the same vault]
```

### Flow 7: Free limit and Pro

```mermaid
flowchart TD
    Save11[Save 11th item] --> F5[F5 Ambr Pro sheet]
    F5 -- Buy ৳499 once --> Saved[Item saved, Pro on]
    F5 -- Not now --> Keep[New item not saved; draft kept]
    Old[Any of the first 10 items] --> Claim[Claim pack, share, call: always free]
```

## 6. Screen states every screen must design

| State | Rule | Example copy |
| --- | --- | --- |
| Empty | Say what goes here and offer the first action | "Nothing saved yet. Start with the most expensive thing you own." |
| All clear | Reassure, never an empty list | "All good. Nothing ending soon." |
| Loading | Shimmer on the exact fields being filled | OCR shimmer on Name, Store, Price, Date |
| Unsure | Amber underline + words, never blocks | "Check this" |
| Error | What happened + what to do, no error codes | "Couldn't read this one. Just type the name, the photo is saved." |
| Permission denied | Explain + way forward | "Camera is off for Ambr. Open settings, or add without a photo." |
| Expired | Kind, with a next step | "Warranty ended on 3 Jan. You may still get a paid repair." |

## 7. Design rules carried into the visual design

- Light mode only. Status pills always pair color with a word and an icon: Covered, Ends soon, Expired.
- Type: Public Sans. Body 16 sp minimum; status sentence on D1 and E1 at 24 sp or larger. Layouts reflow at 200% font scale.
- Tap targets at least 48 × 48 dp; primary actions full width at the bottom, within thumb reach.
- Dates as "14 Mar 2027". Money as ৳ with South Asian grouping (৳4,85,000).
- Amber on white only for large text or with a dark outline (WCAG AA).
- One primary action per screen.
