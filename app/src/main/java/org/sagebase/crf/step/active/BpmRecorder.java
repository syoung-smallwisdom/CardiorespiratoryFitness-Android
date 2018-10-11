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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AnyThread;
import android.support.annotation.UiThread;

import com.google.gson.JsonObject;

import org.researchstack.backbone.step.Step;
import org.researchstack.backbone.step.active.recorder.JsonArrayDataRecorder;
import org.researchstack.backbone.utils.FormatHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by liujoshua on 2/19/2018.
 */

public interface BpmRecorder {
    
    interface BpmUpdateListener {

        @UiThread
        void bpmUpdate(HeartRateBPM bpm);
    }
    
    interface IntelligentStartUpdateListener {
        /**
         * @param progress value from 0.0 to 1.0 communicating the progress to being ready
         * @param ready    true if the camera is now collecting data, false otherwise
         */
        void intelligentStartUpdate(float progress, boolean ready);
    }

    
    class HeartBeatJsonWriter extends JsonArrayDataRecorder
            implements HeartbeatSampleTracker
            .HeartRateUpdateListener {
        
        private static final Logger LOG = LoggerFactory.getLogger(HeartBeatJsonWriter.class);
        
        private static final String TIMESTAMP_DATE_KEY = "timestampDate";
        private static final String TIMESTAMP_IN_SECONDS_KEY = "timestamp";
        private static final String UPTIME_IN_SECONDS_KEY = "uptime";
        private static final String RED_KEY = "red";
        private static final String GREEN_KEY = "green";
        private static final String BLUE_KEY = "blue";
        private static final String RED_LEVEL_KEY = "redLevel";

        private static final int INTELLIGENT_START_FRAMES_TO_PASS = 30;
        
        private final JsonObject mJsonObject = new JsonObject();

        /**
         * Intelligent start is a feature that delays recording until
         * an algorithm determines the user's finger is in front of the camera
         * Disabled by default
         */
        private boolean mEnableIntelligentStart = true;
        private boolean mIntelligentStartPassed = false;
        private int mIntelligentStartCounter = 0;
        private boolean isRecordingStarted = false;
        
        private final BpmRecorder.BpmUpdateListener mBpmUpdateListener;
        private final BpmRecorder.IntelligentStartUpdateListener mIntelligentStartListener;
        
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        public HeartBeatJsonWriter(BpmUpdateListener
                                           mBpmUpdateListener, IntelligentStartUpdateListener
                                           mIntelligentStartListener,
                                   String identifier, Step step, File outputDirectory) {
            super(identifier, step, outputDirectory);
            
            this.mBpmUpdateListener = mBpmUpdateListener;
            this.mIntelligentStartListener = mIntelligentStartListener;
        }
        
        @AnyThread
        @Override
        public void onHeartRateSampleDetected(HeartBeatSample sample) {

            if (sample.timestampDate != null) {
                Date timestampReferenceDate = sample.timestampDate;
                mJsonObject.addProperty(TIMESTAMP_DATE_KEY,
                        new SimpleDateFormat(FormatHelper.DATE_FORMAT_ISO_8601, new Locale("en", "us", "POSIX"))
                                .format(timestampReferenceDate));
                LOG.debug("TIMESTAMP Date key: " + mJsonObject.get(TIMESTAMP_DATE_KEY).getAsString());
            } else {
                mJsonObject.remove(TIMESTAMP_DATE_KEY);
            }

            mJsonObject.addProperty(TIMESTAMP_IN_SECONDS_KEY, sample.timestamp);
            mJsonObject.addProperty(UPTIME_IN_SECONDS_KEY, sample.uptime);
            mJsonObject.addProperty(RED_KEY, sample.red);
            mJsonObject.addProperty(GREEN_KEY, sample.green);
            mJsonObject.addProperty(BLUE_KEY, sample.blue);
            mJsonObject.addProperty(RED_LEVEL_KEY, sample.redLevel);

            // TODO: syoung 10/11/2018 Calculate the heart rate on another thread so that the writing
            // thread is not blocked.
            //bpmCalculator.calculateBpm(sample);
//            if (sample.bpm > 0) {
//                mJsonObject.addProperty(HEART_RATE_KEY, sample.bpm);
//                if (mBpmUpdateListener != null) {
//                    mainHandler.post(() ->
//                            mBpmUpdateListener.bpmUpdate(
//                                    new BpmRecorder.BpmUpdateListener.BpmHolder(sample.bpm, (long)sample.t)));
//                }
//            } else {
//                mJsonObject.remove(HEART_RATE_KEY);
//            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("HeartBeatSample: {}", sample);
            }
            
            if (!mEnableIntelligentStart || mIntelligentStartPassed) {
                writeJsonObjectToFile(mJsonObject);
            } else {
                updateIntelligentStart(sample);
            }
        }
        
        private void updateIntelligentStart(HeartBeatSample sample) {
            if (mIntelligentStartPassed) {
                return; // we already computed that we could start
            }

            // If the red factor is large enough, we update the trigger
            if (sample.isCoveringLens()) {
                mIntelligentStartCounter++;
                if (mIntelligentStartCounter >= INTELLIGENT_START_FRAMES_TO_PASS) {
                    mIntelligentStartPassed = true;
                }
                if (mIntelligentStartListener != null) {
                    float progress = (float) mIntelligentStartCounter / (float)
                            INTELLIGENT_START_FRAMES_TO_PASS;
    
                    mainHandler.post(() ->
                            mIntelligentStartListener.intelligentStartUpdate(progress,
                                    mIntelligentStartPassed)
                    );
                }
                
            } else {  // We need thresholds to be passed sequentially otherwise it is restarted
                mIntelligentStartCounter = 0;
            }
        }
        
        @Override
        public void start(Context context) {
            startJsonDataLogging();
            isRecordingStarted = true;
            mIntelligentStartPassed = false;
            mIntelligentStartCounter = 0;
        }
        
        @Override
        public void stop() {
            if (isRecordingStarted) {
                isRecordingStarted = false;
                stopJsonDataLogging();
            }
        }
        
    }
}
