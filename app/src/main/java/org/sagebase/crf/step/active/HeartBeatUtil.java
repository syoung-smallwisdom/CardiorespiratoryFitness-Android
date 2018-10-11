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
 */

public class HeartBeatUtil {

    private double timestampZeroReference = -1;
    private double uptimeZeroReference = -1;

    public HeartBeatSample getHeartBeatSample(double timestamp, Bitmap bitmap) {
        HeartBeatSample sample = new HeartBeatSample();

        if (timestampZeroReference < 0) {
            // set timestamp reference, which timestamps are measured relative to
            timestampZeroReference = timestamp;
            uptimeZeroReference = System.nanoTime() * 1e-9;
            sample.timestampDate = new Date(System.currentTimeMillis());
        }

        double relativeTimestamp = ((timestamp - timestampZeroReference) / 1_000);
        sample.uptime = uptimeZeroReference + relativeTimestamp;
        sample.timestamp = relativeTimestamp;

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

            double h = getRedHueFromRed(red, green, blue);
            if (h >= 0) {
                redCount++;
            }

            r += red;
            g += green;
            b += blue;
        }

        long rawSampleR = r / intArray.length;
        // Per the need to match iOS data (which gives RGB data as 0.0 - 1.0, normalize these values
        sample.red = ((float)r / (float)intArray.length) / 255.0f;
        sample.green = ((float)g / (float)intArray.length) / 255.0f;
        sample.blue = ((float)b / (float)intArray.length) / 255.0f;
        sample.redLevel = (double)redCount / (double)(width * height);

        return sample;
    }

    private static final double RED_THRESHOLD = 40;

    private double getRedHueFromRed(double r, double g, double b) {
        if ((r < g) || (r < b)) {
            return -1;
        }
        double min = Math.min(g, b);
        double delta = r - min;
        if (delta < RED_THRESHOLD) {
            return -1;
        }
        double hue = 60*((g - b) / delta);
        if (hue < 0) {
            hue += 360;
        }
        return hue;
    }
}
