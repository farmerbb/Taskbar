/* Copyright 2020 Braden Farmer
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
import android.content.SharedPreferences;
import android.view.Surface;
import android.view.WindowManager;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarPosition {

    private TaskbarPosition() {}

    private static Integer cachedRotation;

    /**
     * Transfer origin position based on rotation.
     *
     * @param position The origin position the caller set.
     * @param rotation The current rotation.
     * @return The new position transferred based on rotation.
     */
    private static String transferPositionWithRotation(String position, int rotation) {
        switch(position) {
            case POSITION_BOTTOM_LEFT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_BOTTOM_LEFT;
                    case Surface.ROTATION_90:
                        return POSITION_BOTTOM_VERTICAL_RIGHT;
                    case Surface.ROTATION_180:
                        return POSITION_TOP_RIGHT;
                    case Surface.ROTATION_270:
                        return POSITION_TOP_VERTICAL_LEFT;
                }
                break;
            case POSITION_BOTTOM_VERTICAL_LEFT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_BOTTOM_VERTICAL_LEFT;
                    case Surface.ROTATION_90:
                        return POSITION_BOTTOM_RIGHT;
                    case Surface.ROTATION_180:
                        return POSITION_TOP_VERTICAL_RIGHT;
                    case Surface.ROTATION_270:
                        return POSITION_TOP_LEFT;
                }
                break;
            case POSITION_BOTTOM_RIGHT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_BOTTOM_RIGHT;
                    case Surface.ROTATION_90:
                        return POSITION_TOP_VERTICAL_RIGHT;
                    case Surface.ROTATION_180:
                        return POSITION_TOP_LEFT;
                    case Surface.ROTATION_270:
                        return POSITION_BOTTOM_VERTICAL_LEFT;
                }
                break;
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_BOTTOM_VERTICAL_RIGHT;
                    case Surface.ROTATION_90:
                        return POSITION_TOP_RIGHT;
                    case Surface.ROTATION_180:
                        return POSITION_TOP_VERTICAL_LEFT;
                    case Surface.ROTATION_270:
                        return POSITION_BOTTOM_LEFT;
                }
                break;
            case POSITION_TOP_LEFT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_TOP_LEFT;
                    case Surface.ROTATION_90:
                        return POSITION_BOTTOM_VERTICAL_LEFT;
                    case Surface.ROTATION_180:
                        return POSITION_BOTTOM_RIGHT;
                    case Surface.ROTATION_270:
                        return POSITION_TOP_VERTICAL_RIGHT;
                }
                break;
            case POSITION_TOP_VERTICAL_LEFT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_TOP_VERTICAL_LEFT;
                    case Surface.ROTATION_90:
                        return POSITION_BOTTOM_LEFT;
                    case Surface.ROTATION_180:
                        return POSITION_BOTTOM_VERTICAL_RIGHT;
                    case Surface.ROTATION_270:
                        return POSITION_TOP_RIGHT;
                }
                break;
            case POSITION_TOP_RIGHT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_TOP_RIGHT;
                    case Surface.ROTATION_90:
                        return POSITION_TOP_VERTICAL_LEFT;
                    case Surface.ROTATION_180:
                        return POSITION_BOTTOM_LEFT;
                    case Surface.ROTATION_270:
                        return POSITION_BOTTOM_VERTICAL_RIGHT;
                }
                break;
            case POSITION_TOP_VERTICAL_RIGHT:
                switch(rotation) {
                    case Surface.ROTATION_0:
                        return POSITION_TOP_VERTICAL_RIGHT;
                    case Surface.ROTATION_90:
                        return POSITION_TOP_LEFT;
                    case Surface.ROTATION_180:
                        return POSITION_BOTTOM_VERTICAL_LEFT;
                    case Surface.ROTATION_270:
                        return POSITION_BOTTOM_RIGHT;
                }
                break;
        }
        return POSITION_BOTTOM_LEFT;
    }

    public static boolean isVertical(String position) {
        switch(position) {
            case POSITION_TOP_VERTICAL_LEFT:
            case POSITION_TOP_VERTICAL_RIGHT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isVertical(Context context) {
        return isVertical(getTaskbarPosition(context));
    }

    public static boolean isLeft(String position) {
        switch(position) {
            case POSITION_TOP_LEFT:
            case POSITION_TOP_VERTICAL_LEFT:
            case POSITION_BOTTOM_LEFT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLeft(Context context) {
        return isLeft(getTaskbarPosition(context));
    }

    public static boolean isRight(String position) {
        switch(position) {
            case POSITION_TOP_RIGHT:
            case POSITION_TOP_VERTICAL_RIGHT:
            case POSITION_BOTTOM_RIGHT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isRight(Context context) {
        return isRight(getTaskbarPosition(context));
    }

    public static boolean isBottom(String position) {
        switch(position) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_BOTTOM_RIGHT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                return true;
            default:
                return false;
        }
    }

    public static boolean isBottom(Context context) {
        return isBottom(getTaskbarPosition(context));
    }

    public static boolean isVerticalLeft(String position) {
        return isVertical(position) && isLeft(position);
    }

    public static boolean isVerticalLeft(Context context) {
        return isVerticalLeft(getTaskbarPosition(context));
    }

    public static boolean isVerticalRight(String position) {
        return isVertical(position) && isRight(position);
    }

    public static boolean isVerticalRight(Context context) {
        return isVerticalRight(getTaskbarPosition(context));
    }

    public static String getTaskbarPosition(Context context) {
        SharedPreferences pref = U.getSharedPreferences(context);
        String position = pref.getString(PREF_POSITION, POSITION_BOTTOM_LEFT);

        if(pref.getBoolean(PREF_ANCHOR, false)) {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            int rotation = cachedRotation != null
                    ? cachedRotation
                    : windowManager.getDefaultDisplay().getRotation();

            String finalPosition = transferPositionWithRotation(position, rotation);
            return finalPosition == null ? position : finalPosition;
        }

        return position;
    }

    public static void setCachedRotation(int cachedRotation) {
        TaskbarPosition.cachedRotation = cachedRotation;
    }
}