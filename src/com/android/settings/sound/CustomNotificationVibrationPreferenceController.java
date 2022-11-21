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

package com.android.settings.sound;

import android.content.Context;
import android.provider.Settings;

public class CustomNotificationVibrationPreferenceController extends CustomVibrationPreferenceController {
    private static final String DEFAULT_SETTINGS_VALUE = "80,40,0";

    public CustomNotificationVibrationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mVibrator.hasVibrator();
    }

    @Override
    protected String getSettingsKey() {
        return Settings.System.CUSTOM_NOTIFICATION_VIBRATION_PATTERN;
    }

    @Override
    protected String getDefaultValue() {
        return DEFAULT_SETTINGS_VALUE;
    }

    @Override
    protected long getDelay() {
        return 120;
    }
}
