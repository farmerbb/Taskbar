// Mirror of https://github.com/LSPosed/AndroidHiddenApiBypass/blob/main/stub/src/main/java/dalvik/system/VMRuntime.java

package dalvik.system;

@SuppressWarnings("unused")
public class VMRuntime {
    public static VMRuntime getRuntime() {
        throw new IllegalArgumentException("stub");
    }
    public native void setHiddenApiExemptions(String[] signaturePrefixes);
}
