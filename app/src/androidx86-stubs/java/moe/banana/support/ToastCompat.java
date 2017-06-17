// Stub class used for Android-x86 builds

package moe.banana.support;

import android.content.Context;

public class ToastCompat {

    public static ToastCompat makeText(Context context, String message, int length) {
        return new ToastCompat();
    }

    public void show() {}

    public void cancel() {}
}