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

import static android.provider.Settings.System.LOCKSCREEN_TIMEOUT;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

import com.evolution.settings.preference.SystemSettingListPreference;

public class TimeoutLockscreenPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "TimeoutLockscreenPrefContr";

    /** If there is no setting in the provider, use this. */
    public static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 15000;

    private final String mLockScreenTimeoutKey;

    public TimeoutLockscreenPreferenceController(Context context, String key) {
        super(context);
        mLockScreenTimeoutKey = key;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return mLockScreenTimeoutKey;
    }

    @Override
    public void updateState(Preference preference) {
        final SystemSettingListPreference systemSettingsListPreference = (SystemSettingListPreference) preference;
        final long currentTimeout = Settings.System.getLong(mContext.getContentResolver(),
                LOCKSCREEN_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        systemSettingsListPreference.setValue(String.valueOf(currentTimeout));
        updateTimeoutPreferenceDescription(systemSettingsListPreference,
                Long.parseLong(systemSettingsListPreference.getValue()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        try {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(mContext.getContentResolver(), LOCKSCREEN_TIMEOUT, value);
            updateTimeoutPreferenceDescription((SystemSettingListPreference) preference, value);
        } catch (NumberFormatException e) { }
        return true;
    }

    public static CharSequence getTimeoutDescription(
            long currentTimeout, CharSequence[] entries, CharSequence[] values) {
        if (currentTimeout < 0 || entries == null || values == null
                || values.length != entries.length) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (currentTimeout == timeout) {
                return entries[i];
            }
        }
        return null;
    }

    private void updateTimeoutPreferenceDescription(SystemSettingListPreference preference,
                                                    long currentTimeout) {
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        final String summary;

        final CharSequence timeoutDescription = getTimeoutDescription(
                currentTimeout, entries, values);
        summary = timeoutDescription == null
                ? ""
                : mContext.getString(R.string.screen_timeout_summary, timeoutDescription);
        preference.setSummary(summary);
    }
}
