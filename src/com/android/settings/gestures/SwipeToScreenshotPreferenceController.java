/*
 * Copyright (C) 2019 The PixelExperience Project
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

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.UserHandle;

import com.android.settings.R;

public class SwipeToScreenshotPreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "swipe_to_screenshot_video";

    public SwipeToScreenshotPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return true;
    }

    @Override
    public boolean isChecked() {
        return true;
    }

    private boolean isSwipeToScreenshotGestureEnabled() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.THREE_FINGER_GESTURE, 0, UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(
                isSwipeToScreenshotGestureEnabled() ? R.string.gesture_setting_on : R.string.gesture_setting_off);
    }
}
