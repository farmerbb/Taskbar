# Add project specific ProGuard rules here

-applymapping mapping.txt

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}