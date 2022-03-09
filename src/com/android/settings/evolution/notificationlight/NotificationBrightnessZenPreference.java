/*
 * Copyright (C) 2017-2022 The LineageOS Project
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

package com.android.settings.evolution.notificationlight;

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;

public class NotificationBrightnessZenPreference extends BrightnessPreference {
    private static final String TAG = "NotificationBrightnessZenPreference";

    private final ContentResolver mResolver;

    public NotificationBrightnessZenPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mResolver = context.getContentResolver();
    }

    @Override
    protected int getBrightnessSetting() {
        return Settings.System.getIntForUser(mResolver,
                Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                LIGHT_BRIGHTNESS_MAXIMUM, UserHandle.USER_CURRENT);
    }

    @Override
    protected void setBrightnessSetting(int brightness) {
        Settings.System.putIntForUser(mResolver,
                Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                brightness, UserHandle.USER_CURRENT);
    }
}
