/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.gestures;

import static android.provider.Settings.System.TORCH_LONG_PRESS_POWER_GESTURE;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

import com.android.settings.R;

public class PowerButtonTorchGesturePreferenceController extends GesturePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;

    private final String PREF_KEY_VIDEO = "gesture_quick_torch_video";

    public PowerButtonTorchGesturePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean supportLongPressPowerWhenNonInteractive = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportLongPressPowerWhenNonInteractive);
        return supportLongPressPowerWhenNonInteractive && mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) ? AVAILABLE :
                        UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "power_button_torch");
    }

    @Override
    public String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), TORCH_LONG_PRESS_POWER_GESTURE,
                isChecked ? ON : OFF);
    }

    @Override
    public boolean isChecked() {
        return isEnabled(mContext);
    }

    public static boolean isEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), TORCH_LONG_PRESS_POWER_GESTURE, OFF) != OFF;
    }

    public static String getPrefSummary(Context context) {
        return context.getString(
                isEnabled(context) ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }
}
