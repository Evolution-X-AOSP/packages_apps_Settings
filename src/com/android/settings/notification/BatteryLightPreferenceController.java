/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import static android.provider.Settings.Global.BATTERY_LIGHT_ENABLED;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class BatteryLightPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private static final int ON = 1;
    private static final int OFF = 0;
    private SettingObserver mSettingObserver;

    public BatteryLightPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            mSettingObserver = new SettingObserver(preference);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_intrusiveBatteryLed) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        boolean enabledByDefault = mContext.getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveBatteryLed);
        return Settings.Global.getInt(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED,
                enabledByDefault ? ON : OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(), BATTERY_LIGHT_ENABLED,
                isChecked ? ON : OFF);
    }

    class SettingObserver extends ContentObserver {

        private final Uri BATTERY_LIGHT_ENABLED_URI =
                Settings.Global.getUriFor(Settings.Global.BATTERY_LIGHT_ENABLED);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(BATTERY_LIGHT_ENABLED_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (BATTERY_LIGHT_ENABLED_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }
}
