# 🎌 Nihoru Android App

Native Android app version of [nihoru.top](https://nihoru.top) — The Ultimate Anime List Converter.

---

## 📱 Features

- **Text to MAL Converter** — Paste anime/manga names, get MAL XML instantly
- **AniList API** powered smart matching
- **Auto Season Detection** — Automatically adds sequels/prequels
- **Dark & Light Theme** — Toggle from the navbar
- **Export XML** — Import directly into MyAnimeList
- **Export JSON** — For AniList and other platforms
- **Offline Storage** — List saved locally with Room DB

---

## 🚀 GitHub Actions — Auto Build APK

Every time you push a tag like `v1.0`, GitHub Actions automatically:
1. Builds your app
2. Signs it with your keystore
3. Creates a GitHub Release with the APK attached

### Step 1: Create a Keystore

Run this on any device with Java (or use an online service):

```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nihoru-key
```

### Step 2: Convert to Base64

```bash
# Linux/Mac
base64 -i release-key.jks

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-key.jks"))
```

### Step 3: Add GitHub Secrets

Go to your repo → **Settings** → **Secrets and variables** → **Actions**

| Secret Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64 string from step 2 |
| `KEY_STORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `nihoru-key` |
| `KEY_PASSWORD` | Your key password |

### Step 4: Push a Tag to Release

```bash
git add .
git commit -m "Release v1.0"
git tag v1.0
git push origin main --tags
```

Done! Check **Actions** tab, then **Releases** for your APK. 🎉

---

## 📁 Project Structure

```
app/src/main/
├── java/top/nihoru/app/
│   ├── ui/
│   │   ├── MainActivity.kt          # Main screen with drawer
│   │   ├── home/HomeFragment.kt     # Landing page
│   │   └── converter/
│   │       ├── ConverterFragment.kt # Main converter UI
│   │       ├── ConverterViewModel.kt # Business logic
│   │       └── AnimeAdapter.kt      # RecyclerView adapter
│   ├── data/
│   │   ├── model/AnimeEntry.kt      # Data model
│   │   ├── model/AniListModels.kt   # API response models
│   │   └── repository/NihoruDatabase.kt # Room DB
│   ├── network/AniListApi.kt        # AniList GraphQL API
│   └── utils/Exporter.kt            # XML/JSON export
└── res/
    ├── layout/                      # XML layouts
    ├── drawable/                    # Backgrounds & icons
    ├── values/                      # Colors, strings, themes
    └── navigation/nav_graph.xml     # Navigation
```

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** XML Layouts + ViewBinding
- **Architecture:** MVVM + LiveData
- **Navigation:** Navigation Component
- **Network:** OkHttp (GraphQL)
- **Database:** Room (SQLite)
- **Images:** Glide
- **Build:** Gradle + GitHub Actions

---

## 📄 License

© 2026 Nihoru Tools. Powered by AniList API. Not affiliated with MyAnimeList.
