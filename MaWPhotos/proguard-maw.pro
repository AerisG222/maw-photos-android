-printusage build/proguard-usage.txt

-keep class us.mikeandwan.photos.api.** {
    *;
}

# Hilt EntryPoints and their generated code
-keep @dagger.hilt.EntryPoint interface * { *; }
-keep @dagger.hilt.InstallIn interface * { *; }

# Glance ActionCallbacks - must keep the class AND the no-arg constructor
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
    *;
}

# Keep the specific classes and all their members
-keep class us.mikeandwan.photos.ui.widgets.RefreshWidgetAction {
    public <init>();
    *;
}
-keep class us.mikeandwan.photos.ui.widgets.RandomPhotoWidgetReceiver {
    public <init>();
    *;
}
-keep class us.mikeandwan.photos.ui.widgets.RandomPhotoWidget {
    public <init>();
    *;
}

# this was output as part of build
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
