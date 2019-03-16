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

package com.android.settings.display;

import android.app.Activity;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import androidx.preference.Preference;
import java.util.Objects;

import com.android.internal.util.custom.cutout.CutoutUtils;

public class DisplayCutoutPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    public DisplayCutoutPreferenceController(Context context, String key) {
        super(context, key);
    }

    private int getConfig(){
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DISPLAY_CUTOUT_HIDDEN, 0);
    }

    @Override
    public int getAvailabilityStatus() {
        return CutoutUtils.hasCutout(mContext, true) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Integer.valueOf((String) newValue) == getConfig()) {
            return true;
        }
        return true;
    }

}