/*
 * Copyright (C) 2020 PixelExperience
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
 * limitations under the License
 */

package com.android.settings.gestures;

import android.content.Context;

import com.android.internal.custom.hardware.LineageHardwareManager;

import com.android.settings.core.BasePreferenceController;

public class TouchGestureSettingsPreferenceController extends BasePreferenceController {

    public static final String KEY = "touchscreen_gesture_settings";

    private final LineageHardwareManager mHardware;

    public TouchGestureSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mHardware = LineageHardwareManager.getInstance(context);
    }

    public TouchGestureSettingsPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mHardware.isSupported(LineageHardwareManager.FEATURE_TOUCHSCREEN_GESTURES)){
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }
}
