# Global attributes
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable

# Catgirl codebase - Prevent ANY obfuscation or stripping
-keep class catgirl.** { *; }
-keep interface catgirl.** { *; }
-dontwarn catgirl.**

# RxJava 1.x
-keep class rx.** { *; }
-keep interface rx.** { *; }
-dontwarn rx.**

# RxAndroid
-keep class rx.android.** { *; }
-dontwarn rx.android.**

# Retrofit 2
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp 3
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Realm
-keep class io.realm.** { *; }
-dontwarn io.realm.**
-keep class io.realm.annotations.** { *; }

# Dagger 2
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class **_MembersInjector { *; }
-keep class **_Factory { *; }
-keep class **_Provide*Factory { *; }

# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **_ViewBinding { *; }
-keepclasseswithmembernames class * { @butterknife.BindView <fields>; }
-keepclasseswithmembernames class * { @butterknife.OnClick <methods>; }

# Picasso
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

# NineOldAndroids
-keep class com.nineoldandroids.** { *; }
-dontwarn com.nineoldandroids.**

# Common Android / Support Library
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Standard Java types used in Retrofit/Gson
-keep class java.util.Map { *; }
-keep class java.util.List { *; }
-keep class java.util.ArrayList { *; }
-keep class java.util.HashMap { *; }
