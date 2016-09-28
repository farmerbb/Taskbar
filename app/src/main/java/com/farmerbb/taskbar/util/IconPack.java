/* Based on code by Richard Ginzburg
 * See http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 *
 * Copyright 2016 Braden Farmer
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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class IconPack {
    private String packageName;
    private String name;

    private boolean mLoaded = false;
    private Map<String, String> mPackagesDrawables = new HashMap<>();
    private int totalIcons;

    private Resources iconPackres = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    void load(Context mContext) {
        // Load appfilter.xml from the icon pack package
        PackageManager pm = mContext.getPackageManager();
        try {
            XmlPullParser xpp = null;

            iconPackres = pm.getResourcesForApplication(packageName);
            int appfilterid = iconPackres.getIdentifier("appfilter", "xml", packageName);
            if(appfilterid > 0) {
                xpp = iconPackres.getXml(appfilterid);
            } else {
                // No resource found, try to open it from assets folder
                try {
                    InputStream appfilterstream = iconPackres.getAssets().open("appfilter.xml");

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    xpp = factory.newPullParser();
                    xpp.setInput(appfilterstream, "utf-8");
                } catch (IOException e) { /* Gracefully fail */ }
            }

            if(xpp != null) {
                int eventType = xpp.getEventType();
                while(eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        if(xpp.getName().equals("item")) {
                            String componentName = null;
                            String drawableName = null;

                            for(int i = 0; i < xpp.getAttributeCount(); i++) {
                                if(xpp.getAttributeName(i).equals("component")) {
                                    componentName = xpp.getAttributeValue(i);
                                } else if(xpp.getAttributeName(i).equals("drawable")) {
                                    drawableName = xpp.getAttributeValue(i);
                                }
                            }
                            if(!mPackagesDrawables.containsKey(componentName)) {
                                mPackagesDrawables.put(componentName, drawableName);
                                totalIcons = totalIcons + 1;
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            }
            mLoaded = true;
        } catch (PackageManager.NameNotFoundException | XmlPullParserException | IOException e) { /* Gracefully fail */ }
    }

    @SuppressWarnings("deprecation")
    private Drawable loadDrawable(String drawableName) {
        int id = iconPackres.getIdentifier(drawableName, "drawable", packageName);
        if(id > 0) {
            return iconPackres.getDrawable(id);
        }
        return null;
    }

    Drawable getDrawableIconForPackage(Context mContext, String componentName) {
        if(!mLoaded) load(mContext);

        String drawable = mPackagesDrawables.get(componentName);
        if(drawable != null) {
            return loadDrawable(drawable);
        } else {
            // Try to get a resource with the component filename
            if(componentName != null) {
                int start = componentName.indexOf("{") + 1;
                int end = componentName.indexOf("}", start);
                if(end > start) {
                    drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                    if(iconPackres.getIdentifier(drawable, "drawable", packageName) > 0)
                        return loadDrawable(drawable);
                }
            }
        }

        return null;
    }
}