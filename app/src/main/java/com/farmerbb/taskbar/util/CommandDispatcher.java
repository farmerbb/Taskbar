/* Copyright 2019 Braden Farmer
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
import android.content.Intent;

import com.farmerbb.taskbar.BuildConfig;

import java.util.ArrayList;
import java.util.List;

public class CommandDispatcher {

    private List<String> commands = new ArrayList<>();

    private static CommandDispatcher theInstance;

    private CommandDispatcher() {}

    public static CommandDispatcher getInstance() {
        if(theInstance == null) theInstance = new CommandDispatcher();

        return theInstance;
    }

    boolean addCommand(Context context, String command) {
        if(U.hasSecondScreenSupportLibrary(context) && !command.isEmpty()) {
            commands.add(command);
            return true;
        }

        return false;
    }

    void dispatch(Context context) {
        if(U.hasSecondScreenSupportLibrary(context)) {
            Intent intent = new Intent(BuildConfig.SS_SUPPORT_APPLICATION_ID + ".DISPATCH_COMMANDS");
            intent.setPackage(BuildConfig.SS_SUPPORT_APPLICATION_ID);

            if(!commands.isEmpty())
                intent.putExtra("commands", commands.toArray(new String[commands.size()]));

            context.sendBroadcast(intent);
        }

        theInstance = null;
    }
}