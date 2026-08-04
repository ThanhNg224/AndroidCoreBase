# Consumer ProGuard/R8 rules for apps that depend on :core with minification enabled.
# Hilt, Retrofit, OkHttp, kotlinx.serialization and Compose all ship their own consumer rules inside
# their artifacts; this file only covers :core's own classes that a library reflects into by name.
#
# There are currently none. The previous `-keep` on core.storage.database.** existed for Room, and
# :core no longer ships a database (docs/MODERNIZATION.md F7/D5) — a consumer declares its own
# `@Database`, so that keep rule belongs in their build, not here.
#
# Exercised for real as of 2026-07-29: `:app` builds release with R8 (`isMinifyEnabled` +
# `isShrinkResources`) and a signed minified build was validated on device — live network calls with
# kotlinx.serialization, Hilt, DataStore, per-app locale and the Compose interop all work with no
# additions to this file. See docs/MODERNIZATION.md F10.

# Keep generic signature and class structure metadata if needed for reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod
