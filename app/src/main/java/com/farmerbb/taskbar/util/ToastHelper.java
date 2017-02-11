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

import moe.banana.support.ToastCompat;

class ToastHelper {

    private ToastCompat lastToast;

    private static ToastHelper theInstance;

    private ToastHelper() {}

    public static ToastHelper getInstance() {
        if(theInstance == null) theInstance = new ToastHelper();

        return theInstance;
    }

    ToastCompat getLastToast() {
        return lastToast;
    }

    void setLastToast(ToastCompat lastToast) {
        this.lastToast = lastToast;
    }
}