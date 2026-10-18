# Règles R8/ProGuard — Godson VPN
# La lib WireGuard embarque ses règles consumer ; on complète ici.

# Journaux retirés en release (aucune donnée sensible ne doit fuir par les logs)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# kotlinx.serialization — conserver les serializers générés
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.godson.vpn.**$$serializer { *; }
-keepclassmembers class com.godson.vpn.** { *** Companion; }
-keepclasseswithmembers class com.godson.vpn.** { kotlinx.serialization.KSerializer serializer(...); }
