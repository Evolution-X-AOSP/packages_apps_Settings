/*
 * Copyright (C) 2020 The Android Open Source Project
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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.PrimarySwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for One-handed mode shortcut settings
 */
public class OneHandedEnablePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private PrimarySwitchPreference mPreference;
    private final OneHandedSettingsUtils mOneHandedUtils;
    private final OneHandedSettingsUtils.TogglesCallback mCallback = uri -> {
        if (OneHandedSettingsUtils.ONE_HANDED_MODE_ENABLED_URI.equals(uri)
                || OneHandedSettingsUtils.SHOW_NOTIFICATION_ENABLED_URI.equals(uri)) {
            if (mPreference != null) updateState(mPreference);
        }
    };

    public OneHandedEnablePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mOneHandedUtils = new OneHandedSettingsUtils(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateState(mPreference);
    }

    @Override
    public boolean isChecked() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean enabled = OneHandedSettingsUtils.isOneHandedModeEnabled(mContext);
        String summary;
        if (enabled) {
            final String enabledStr = OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext)
                    ? mContext.getString(R.string.one_handed_action_show_notification_title)
                    : mContext.getString(R.string.one_handed_action_pull_down_screen_title);
            summary = mContext.getString(R.string.gesture_setting_on)
                    + " (" + enabledStr + ")";
        } else {
            summary = mContext.getString(R.string.gesture_setting_off);
        }
        preference.setSummary(summary);
    }

    @Override
    public int getAvailabilityStatus() {
        return OneHandedSettingsUtils.isSupportOneHandedMode() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        mOneHandedUtils.registerToggleAwareObserver(mCallback);
    }

    @Override
    public void onStop() {
        mOneHandedUtils.unregisterToggleAwareObserver();
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
