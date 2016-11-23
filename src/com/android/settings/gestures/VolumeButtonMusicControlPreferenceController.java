/*
 * Copyright (C) 2019 Paranoid Android
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

import static android.provider.Settings.System.VOLBTN_MUSIC_CONTROLS;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

public class VolumeButtonMusicControlPreferenceController extends GesturePreferenceController {

    private final int ON = 1;
    private final int OFF = 0;

    private static final String PREF_KEY_VIDEO = "volume_button_music_control_video";

    public VolumeButtonMusicControlPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "volume_button_music_control");
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(), VOLBTN_MUSIC_CONTROLS,
                isChecked ? ON : OFF, UserHandle.USER_CURRENT);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), VOLBTN_MUSIC_CONTROLS,
                0, UserHandle.USER_CURRENT) != 0;
    }
}
