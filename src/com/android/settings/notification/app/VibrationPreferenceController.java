/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Vibrator;

import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

public class VibrationPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_VIBRATE = "vibrate";
    private final Vibrator mVibrator;
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    public VibrationPreferenceController(Context context, NotificationBackend backend,
            NotificationSettings.DependentFieldListener dependentFieldListener) {
        super(context, backend);
        mVibrator = context.getSystemService(Vibrator.class);
        mDependentFieldListener = dependentFieldListener;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIBRATE;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable() || mChannel == null) {
            return false;
       }
        return checkCanBeVisible(NotificationManager.IMPORTANCE_DEFAULT)
                && !isDefaultChannel()
                && mVibrator != null
                && mVibrator.hasVibrator();
    }

    @Override
    boolean isIncludedInFilter() {
        return mPreferenceFilter.contains(NotificationChannel.EDIT_VIBRATION);
    }

    public void updateState(Preference preference) {
        if (mChannel != null) {
            RestrictedSwitchPreference pref = (RestrictedSwitchPreference) preference;
            pref.setDisabledByAdmin(mAdmin);
            pref.setEnabled(!pref.isDisabledByAdmin());
            pref.setChecked(mChannel.shouldVibrate());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mChannel != null) {
            final boolean vibrate = (Boolean) newValue;
            mChannel.enableVibration(vibrate);
            saveChannel();
            mDependentFieldListener.onFieldValueChanged();
        }
        return true;
    }
}
