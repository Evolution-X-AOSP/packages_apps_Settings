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

package com.android.settings.gestures;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import com.evolution.settings.preference.CustomSeekBarPreference;

public class QuickMutePreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String KEY = "volume_button_quick_mute_delay";

    private CustomSeekBarPreference mDelayPref;
    private final Context mContext;

    public QuickMutePreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final int value = Settings.System.getInt(
                mContext.getContentResolver(), KEY, 800);
        mDelayPref = screen.findPreference(KEY);
        mDelayPref.setValue(value);
        mDelayPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDelayPref) {
            final int value = (Integer) newValue;
            Settings.System.putInt(
                    mContext.getContentResolver(), KEY, value);
            return true;
        }
        return false;
    }
}
