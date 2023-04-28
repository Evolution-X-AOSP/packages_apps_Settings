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

import static android.provider.Settings.Secure.DOZE_ENABLED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.evolution.settings.preference.SecureSettingSwitchPreference;

public class AmbientDisplayNotificationsPreferenceController extends
        TogglePreferenceController implements Preference.OnPreferenceChangeListener {

    private final int ON = 1;
    private final int OFF = 0;

    @VisibleForTesting
    static final String KEY_AMBIENT_DISPLAY_NOTIFICATIONS = "ambient_display_notification";
    private static final String KEY_DOZE_FOR_NOTIFICATIONS = "doze_for_notifications";
    private static final int MY_USER = UserHandle.myUserId();

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private AmbientDisplayConfiguration mConfig;
    private SecureSettingSwitchPreference mDozeForNotifPref;

    public AmbientDisplayNotificationsPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    /**
     * Set AmbientDisplayConfiguration for this controller, please call in onAttach of fragment
     *
     * @param config AmbientDisplayConfiguration for this controller
     */
    public AmbientDisplayNotificationsPreferenceController setConfig(
            AmbientDisplayConfiguration config) {
        mConfig = config;
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mDozeForNotifPref = (SecureSettingSwitchPreference)
                screen.findPreference(KEY_DOZE_FOR_NOTIFICATIONS);
        if (mDozeForNotifPref != null)
            mDozeForNotifPref.setEnabled(isChecked());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DISPLAY_NOTIFICATIONS.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return getAmbientConfig().pulseOnNotificationEnabled(MY_USER);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mDozeForNotifPref != null)
            mDozeForNotifPref.setEnabled(isChecked);
        Settings.Secure.putInt(mContext.getContentResolver(), DOZE_ENABLED, isChecked ? ON : OFF);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return getAmbientConfig().pulseOnNotificationAvailable()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "ambient_display_notification");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    private AmbientDisplayConfiguration getAmbientConfig() {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(mContext);
        }

        return mConfig;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }
}
