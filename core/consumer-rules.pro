# Consumer ProGuard/R8 rules for apps that depend on :core with minification enabled.
# Hilt, Retrofit and OkHttp already ship their own consumer rules inside their AARs; this file only
# covers :core's own classes that a library reflects into by name.
#
# There are currently none. The previous `-keep` on core.storage.database.** existed for Room, and
# :core no longer ships a database (docs/MODERNIZATION.md F7/D5) — a consumer declares its own
# `@Database`, so that keep rule belongs in their build, not here.
#
# NOTE: nothing in this file has ever been exercised — `:app` builds release with R8 off (F10), so
# validate against a real minified build before trusting it.

# Keep generic signature and class structure metadata if needed for reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod
