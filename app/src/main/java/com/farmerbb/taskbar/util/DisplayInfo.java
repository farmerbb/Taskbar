/* Copyright 2018 Braden Farmer
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

public class DisplayInfo {
    public int width;
    public int height;
    public int currentDensity;
    public int defaultDensity;
    public boolean displayDefaultsToFreeform;

    DisplayInfo(int width, int height, int currentDensity, int defaultDensity, boolean displayDefaultsToFreeform) {
        this.width = width;
        this.height = height;
        this.currentDensity = currentDensity;
        this.defaultDensity = defaultDensity;
        this.displayDefaultsToFreeform = displayDefaultsToFreeform;
    }
}
