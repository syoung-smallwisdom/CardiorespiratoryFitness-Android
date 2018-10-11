/*
 *    Copyright 2017 Sage Bionetworks
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

import com.google.common.base.MoreObjects;
import java.util.Date;

import javax.annotation.Nullable;

/**
 * Created by TheMDP on 10/17/17.
 */

public class HeartBeatSample {
    public double timestamp;
    public float red;
    public float green;
    public float blue;
    public double redLevel;
    public int bpm;
    public double uptime;

    @Nullable
    public Date timestampDate;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("red", red)
                .add("green", green)
                .add("blue", blue)
                .add("redLevel", redLevel)
                .add("bpm", bpm)
                .add("uptime", uptime)
                .toString();
    }

    // The minimum "red level" (number of pixels that are "red" dominant) to qualify as having the lens covered.
    private static final double MIN_RED_LEVEL = 0.9;

    // Look for the hue to be in the red zone and the saturation to be fairly high.
    private static final float LOW_HUE = (float)30.0;
    private static final float HIGH_HUE = (float)350.0;
    private static final float MIN_SATURATION = (float)0.7;

    /// Is the user's finger covering the lens?
    public boolean isCoveringLens() {

        // If the red level isn't high enough then exit with false.
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
