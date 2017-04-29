# Add project specific ProGuard rules here

-keepattributes LineNumberTable,SourceFile

-applymapping mapping.txt

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

-dontwarn java.lang.invoke.*