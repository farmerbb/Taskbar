# Add project specific ProGuard rules here

-keepattributes LineNumberTable,SourceFile

-applymapping mapping.txt

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

-dontwarn java.lang.invoke.*

-keep class **.R$string

-keepclassmembers class **.R$bool {
    <fields>;
}

-keepclassmembers class **.R$integer {
    <fields>;
}
