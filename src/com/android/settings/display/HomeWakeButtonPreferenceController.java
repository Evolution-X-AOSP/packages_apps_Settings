/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.SwitchPreference;
import androidx.preference.Preference;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

import com.evolution.settings.preference.SystemSettingSwitchPreference;

public class HomeWakeButtonPreferenceController extends AbstractPreferenceController implements
        Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String HOME_WAKE_BUTTON = "home_wake_button";

    public HomeWakeButtonPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return HOME_WAKE_BUTTON;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_HomeWakeButton);
    }

    @Override
    public void updateState(Preference preference) {
        int value = Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.HOME_WAKE_BUTTON, 0);
        ((SwitchPreference) preference).setChecked(value != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.System.putInt(
                mContext.getContentResolver(), Settings.System.HOME_WAKE_BUTTON, value ? 1 : 0);
        return true;
    }
}
