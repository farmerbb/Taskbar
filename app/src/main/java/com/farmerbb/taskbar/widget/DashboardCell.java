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

package com.farmerbb.taskbar.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

public class DashboardCell extends FrameLayout {

    public DashboardCell(Context context) {
        super(context);
    }

    public DashboardCell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DashboardCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public interface OnInterceptedLongPressListener {
        void onInterceptedLongPress(DashboardCell cell);
    }

    // Use this instance of the interface to deliver action events
    private OnInterceptedLongPressListener listener;

    long beginTime;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch(ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                beginTime = ev.getEventTime();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                long eventLength = ev.getEventTime() - beginTime;

                if(eventLength > ViewConfiguration.getLongPressTimeout()) {
                    if(listener != null)
                        listener.onInterceptedLongPress(this);

                    cancelLongPress();
                    return true;
                }

                break;
        }

        return false;
    }

    public void setOnInterceptedLongPressListener(OnInterceptedLongPressListener listener) {
        this.listener = listener;
    }
}