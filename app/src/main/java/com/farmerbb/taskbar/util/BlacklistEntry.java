/* Copyright 2016 Braden Farmer
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

import java.io.Serializable;

public class BlacklistEntry implements Serializable {
    static final long serialVersionUID = 2534812642210454191L;

    private String packageName;
    private String label;

    public BlacklistEntry(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }
}