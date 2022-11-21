/*
 * Copyright (C) 2022 Yet Another AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

/**
 * This class allows choosing a vibration pattern for notifications
 */
public class NotificationVibrationPatternPreferenceController extends VibrationPatternPreferenceController {

    private static final String KEY_VIB_PATTERN = "notification_vibration_pattern";
    private static final String KEY_CUSTOM_VIB_CATEGORY = "custom_notification_vibration_pattern";
    private static final long[] DEFAULT_VIBRATE_PATTERN = {0, 250, 250, 250};
    private static final int VIBRATE_PATTERN_MAXLEN = 8 * 2 + 1; // up to eight bumps

    private final long[] mDefaultPattern;
    private final int[] mDefaultPatternAmp;

    public NotificationVibrationPatternPreferenceController(Context context) {
        super(context);
        mDefaultPattern = getLongArray(context.getResources(),
                com.android.internal.R.array.config_defaultNotificationVibePattern,
                VIBRATE_PATTERN_MAXLEN,
                DEFAULT_VIBRATE_PATTERN);

        // making a full amp array according to what we have in config
        int[] ampArr = new int[mDefaultPattern.length];
        for (int i = 0; i < mDefaultPattern.length; i++) {
            if (i % 2 == 0) ampArr[i] = 0;
            else ampArr[i] = 255;
        }
        mDefaultPatternAmp = ampArr;
    }

    @Override
    public boolean isAvailable() {
        return mVibrator.hasVibrator();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIB_PATTERN;
    }

    @Override
    protected String getCustomPreferenceKey() {
        return KEY_CUSTOM_VIB_CATEGORY;
    }

    @Override
    protected String getSettingsKey() {
        return Settings.System.NOTIFICATION_VIBRATION_PATTERN;
    }

    @Override
    protected long[] getDefaultPattern() {
        return mDefaultPattern;
    }

    @Override
    protected int[] getDefaultPatternAmp() {
        return mDefaultPatternAmp;
    }

    private static long[] getLongArray(Resources resources, int resId, int maxLength, long[] def) {
        int[] ar = resources.getIntArray(resId);
        if (ar == null) return def;
        final int len = ar.length > maxLength ? maxLength : ar.length;
        long[] out = new long[len];
        for (int i = 0; i < len; i++) out[i] = ar[i];
        return out;
    }
}
