/*
 *    Copyright 2018 Sage Bionetworks
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package org.sagebase.crf.step.active;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created by liujoshua on 2/19/2018.
 *
 * The `HeartBeatUtil` is an internal class used to allow tracking the timestamp for a video feed
 * relative to a stored value marking the system time when the video was started.
 *
 */

class HeartBeatUtil {

    private double timestampZeroReference = -1;
    private double uptimeZeroReference = -1;

    HeartBeatSample getHeartBeatSample(double timestamp, Bitmap bitmap) {

        Date timestampDate = null;
        if (timestampZeroReference < 0) {
            // set timestamp reference, which timestamps are measured relative to
            timestampZeroReference = timestamp;
            uptimeZeroReference = System.nanoTime() * 1e-9;
            timestampDate = new Date(System.currentTimeMillis());
        }

        double relativeTimestamp = ((timestamp - timestampZeroReference) / 1_000);
        double uptime = uptimeZeroReference + relativeTimestamp;

        long redCount = 0;
        long r = 0, g = 0, b = 0;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int intArray[] = new int[width * height];
        bitmap.getPixels(intArray, 0, width, 0, 0, width, height);
        for (int i = 0; i < intArray.length; i++) {
            double red = (intArray[i] >> 16) & 0xFF; // Color.red
            double green = (intArray[i] >> 8) & 0xFF; // Color.green
            double blue = (intArray[i] & 0xFF); // Color.blue

            if (isRed(red, green, blue)) {
                redCount++;
            }

            r += red;
            g += green;
            b += blue;
        }

        // Per the need to match iOS data (which gives RGB data as 0.0 - 1.0, normalize these values
        float red = ((float)r / (float)intArray.length) / 255.0f;
        float green = ((float)g / (float)intArray.length) / 255.0f;
        float blue = ((float)b / (float)intArray.length) / 255.0f;
        double redLevel = (double)redCount / (double)(width * height);
        boolean isCoveringLens = isCoveringLens(red, green, blue, redLevel);

        return new HeartBeatSample(
                uptime = uptime,
                relativeTimestamp = relativeTimestamp,
                red = red,
                green = green,
                blue = blue,
                redLevel = redLevel,
                isCoveringLens = isCoveringLens,
                timestampDate = timestampDate
        );
    }

    private static final double RED_THRESHOLD = 40;

    boolean isRed(double r, double g, double b) {
        if ((r < g) || (r < b)) {
            return false;
        }
        double min = Math.min(g, b);
        double delta = r - min;
        if (delta < RED_THRESHOLD) {
            return false;
        } else {
            return true;
        }
    }

    // The minimum "red level" (number of pixels that are "red" dominant) to qualify as having the lens covered.
    private static final double MIN_RED_LEVEL = 0.9;

    // Look for the hue to be in the red zone and the saturation to be fairly high.
    private static final float LOW_HUE = (float)30.0;
    private static final float HIGH_HUE = (float)350.0;
    private static final float MIN_SATURATION = (float)0.7;

    /// Is the user's finger covering the lens?
    boolean isCoveringLens(float red, float green, float blue, double redLevel) {

        // If the red level isn't high enough then exit with false. This is written using early
        // exit `guard` syntax.
        if ((redLevel >= MIN_RED_LEVEL) && (red > green) && (red > blue)) { }
        else {
            return false;
        }

        // Calculate hue and saturation.
        float minValue = Math.min(green, blue);
        float maxValue = red;
        float delta = maxValue - minValue;
        float hue = 60 * ((green - blue) / delta);
        if (hue < 0) {
            hue += 360;
        }
        float saturation = delta / maxValue;

        return (hue <= LOW_HUE || hue >= HIGH_HUE) && (saturation >= MIN_SATURATION);
    }
}
