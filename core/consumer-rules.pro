# Consumer ProGuard/R8 rules for apps that depend on :core with minification enabled.
# Room, Hilt, Retrofit, and OkHttp already ship their own consumer rules inside their AARs;
# these rules only cover :core's own classes that those libraries reflect into.

# Room accesses the database/entity/DAO classes and their fields by name via generated code.
-keep class com.thanhng224.androidxmlbase.core.storage.database.** { *; }

# Keep annotations so Room/Hilt/Retrofit annotation processing metadata survives on release builds.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
