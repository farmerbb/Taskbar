# Add project specific ProGuard rules here

-keepattributes LineNumberTable,SourceFile

-applymapping mapping.txt

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

-dontwarn java.lang.invoke.*

-keep class **.R$string

-keep class moe.banana.support.ToastCompat { *; }
-keep class com.mikepenz.iconics.Iconics$IconicsBuilder { *; }
-keep class com.mikepenz.iconics.Iconics$IconicsBuilderString { *; }