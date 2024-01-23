/*
 * Copyright (C) 2023 Paranoid Android
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class RefreshRatePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "RefreshRatePreferenceController";

    private RefreshRateUtils mUtils;
    private Preference mPreference;
    private final PowerManager mPowerManager;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference != null)
                updateState(mPreference);
        }
    };

    public RefreshRatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUtils = new RefreshRateUtils(context);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        if (mPowerManager.isPowerSaveMode()) {
            return mContext.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_on);
        }
        return mContext.getString(mUtils.isVrrEnabled() ? R.string.refresh_rate_summary_vrr_on
                : R.string.refresh_rate_summary_vrr_off, mUtils.getCurrentRefreshRate());
    }

    @Override
    public int getAvailabilityStatus() {
        return mUtils.isHighRefreshRateAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mPowerManager.isPowerSaveMode());
    }
}
