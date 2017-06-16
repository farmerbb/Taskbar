/* Copyright 2017 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.util;

import android.content.Context;

import java.lang.reflect.Method;

class ToastCompatImpl implements ToastInterface {
    private Class toastCompatClass;
    private Object toast;

    ToastCompatImpl(Context context, String message, int length) throws Exception {
        toastCompatClass = Class.forName("moe.banana.support.ToastCompat");

        Method method = toastCompatClass.getMethod("makeText", Context.class, CharSequence.class, int.class);
        toast = method.invoke(null, context, message, length);
    }

    @Override
    public void show() {
        try {
            Method method = toastCompatClass.getMethod("show");
            method.invoke(toast);
        } catch (Exception e) { /* Gracefully fail */ }
    }

    @Override
    public void cancel() {
        try {
            Method method = toastCompatClass.getMethod("cancel");
            method.invoke(toast);
        } catch (Exception e) { /* Gracefully fail */ }
    }
}
