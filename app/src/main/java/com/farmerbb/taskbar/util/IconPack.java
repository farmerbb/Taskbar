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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class IconPack {
    private String packageName;
    private String name;

    private boolean mIsLoading = false;
    private boolean mLoaded = false;
    private Map<String, String> mPackagesDrawables = new HashMap<>();

    private List<Bitmap> mBackImages = new ArrayList<>();
    private Bitmap mMaskImage = null;
    private Bitmap mFrontImage = null;
    private float mFactor = 0.5f;
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

    private void load(Context mContext) {
        if(!mIsLoading) {
            mIsLoading = true;

            SharedPreferences pref = U.getSharedPreferences(mContext);
            boolean loadMasks = pref.getBoolean("icon_pack_use_mask", false);

            // Load appfilter.xml from the icon pack package
            try {
                XmlPullParser xpp = null;

                int appfilterid = getResources(mContext).getIdentifier("appfilter", "xml", packageName);
                if(appfilterid > 0) {
                    xpp = getResources(mContext).getXml(appfilterid);
                } else {
                    // No resource found, try to open it from assets folder
                    try {
                        InputStream appfilterstream = getResources(mContext).getAssets().open("appfilter.xml");

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
                            if(loadMasks) {
                                if(xpp.getName().equals("iconback")) {
                                    for(int i = 0; i < xpp.getAttributeCount(); i++) {
                                        if(xpp.getAttributeName(i).startsWith("img")) {
                                            String drawableName = xpp.getAttributeValue(i);
                                            Bitmap iconback = loadBitmap(mContext, drawableName);
                                            if(iconback != null)
                                                mBackImages.add(iconback);
                                        }
                                    }
                                } else if(xpp.getName().equals("iconmask")) {
                                    if(xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                        String drawableName = xpp.getAttributeValue(0);
                                        mMaskImage = loadBitmap(mContext, drawableName);
                                    }
                                } else if(xpp.getName().equals("iconupon")) {
                                    if(xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("img1")) {
                                        String drawableName = xpp.getAttributeValue(0);
                                        mFrontImage = loadBitmap(mContext, drawableName);
                                    }
                                } else if(xpp.getName().equals("scale")) {
                                    if(xpp.getAttributeCount() > 0 && xpp.getAttributeName(0).equals("factor")) {
                                        mFactor = Float.valueOf(xpp.getAttributeValue(0));
                                    }
                                }
                            }

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
            } catch (XmlPullParserException | IOException e) { /* Gracefully fail */ }

            mIsLoading = false;
        }
    }

    @SuppressWarnings("deprecation")
    private Bitmap loadBitmap(Context context, String drawableName) {
        int id = getResources(context).getIdentifier(drawableName, "drawable", packageName);
        if(id > 0) {
            Drawable bitmap = getResources(context).getDrawable(id);
            if(bitmap instanceof BitmapDrawable)
                return ((BitmapDrawable) bitmap).getBitmap();
        }
        return null;
    }
    @SuppressWarnings("deprecation")
    private Drawable loadDrawable(Context context, String drawableName) {
        int id = getResources(context).getIdentifier(drawableName, "drawable", packageName);
        if(id > 0) {
            return getResources(context).getDrawable(id);
        }
        return null;
    }

    Drawable getDrawableIconForPackage(Context mContext, String componentName) {
        if(!mLoaded) load(mContext);

        String drawable = mPackagesDrawables.get(componentName);
        if(drawable != null) {
            return loadDrawable(mContext, drawable);
        } else {
            // Try to get a resource with the component filename
            if(componentName != null) {
                int start = componentName.indexOf("{") + 1;
                int end = componentName.indexOf("}", start);
                if(end > start) {
                    drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                    if(getResources(mContext).getIdentifier(drawable, "drawable", packageName) > 0)
                        return loadDrawable(mContext, drawable);
                }
            }
        }

        return null;
    }

    Bitmap getIconForPackage(Context mContext, String componentName, Bitmap defaultBitmap) {
        if(!mLoaded) load(mContext);

        String drawable = mPackagesDrawables.get(componentName);
        if(drawable != null) {
            Bitmap BMP = loadBitmap(mContext, drawable);
            if(BMP == null) {
                return generateBitmap(componentName, defaultBitmap);
            } else {
                return BMP;
            }
        } else {
            // Try to get a resource with the component filename
            if(componentName != null) {
                int start = componentName.indexOf("{") + 1;
                int end = componentName.indexOf("}", start);
                if(end > start) {
                    drawable = componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace(".", "_").replace("/", "_");
                    if(getResources(mContext).getIdentifier(drawable, "drawable", packageName) > 0)
                        return loadBitmap(mContext, drawable);
                }
            }
        }

        return generateBitmap(componentName, defaultBitmap);
    }

    private Bitmap generateBitmap(String componentName, Bitmap defaultBitmap) {
        // If no support images in the icon pack, return the bitmap itself
        if(mBackImages.size() == 0) return defaultBitmap;

        Random r = new Random(generateSeed(componentName));
        int backImageInd = r.nextInt(mBackImages.size());
        Bitmap backImage = mBackImages.get(backImageInd);
        int w = backImage.getWidth();
        int h = backImage.getHeight();

        // Create a bitmap for the result
        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(result);

        // Draw the background first
        mCanvas.drawBitmap(backImage, 0, 0, null);

        // Create a mutable mask bitmap with the same mask
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(defaultBitmap, (int) (w * mFactor), (int) (h * mFactor), true);

        if(mMaskImage != null) {
            // Draw the scaled bitmap with mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(mMaskImage, 0, 0, new Paint());

            // Paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
            mCanvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);
        } else {
            // Draw the scaled bitmap with the back image as mask
            Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas maskCanvas = new Canvas(mutableMask);
            maskCanvas.drawBitmap(backImage, 0, 0, new Paint());

            // Paint the bitmap with mask into the result
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            mCanvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
            mCanvas.drawBitmap(mutableMask, 0, 0, paint);
            paint.setXfermode(null);

        }

        // Paint the front
        if(mFrontImage != null) {
            mCanvas.drawBitmap(mFrontImage, 0, 0, null);
        }

        return result;
    }

    private long generateSeed(String packageName) {
        long seed = 0;

        for(int i = 0; i < packageName.length(); i++) {
            char ch = packageName.charAt(i);
            seed = seed + (long) ch;
        }

        return seed;
    }
    
    private Resources getResources(Context context) {
        if(iconPackres == null)
            try {
                iconPackres = context.getPackageManager().getResourcesForApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                iconPackres = context.getResources();
            }

        return iconPackres;
    }
}