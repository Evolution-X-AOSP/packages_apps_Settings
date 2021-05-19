/*
 * Copyright (C) 2021 Havoc-OS
 * Copyright (C) 2021 The Evolution X Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import com.evolution.settings.preference.SystemSettingMasterSwitchPreference;

public class AdaptiveChargingPreferenceController extends AbstractPreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause, OnStart, OnStop {

    private static final String KEY_ADAPTIVE_CHARGING = "adaptive_charging";

    private SystemSettingMasterSwitchPreference mAdaptiveCharging;

    public AdaptiveChargingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ADAPTIVE_CHARGING;
    }

    @Override
    public void onResume() {
        if (mAdaptiveCharging != null) updateState(mAdaptiveCharging);
    }

    @Override
    public void onPause() {
        if (mAdaptiveCharging != null) updateState(mAdaptiveCharging);
    }

    @Override
    public void onStart() {
        if (mAdaptiveCharging != null) updateState(mAdaptiveCharging);
    }

    @Override
    public void onStop() {
        if (mAdaptiveCharging != null) updateState(mAdaptiveCharging);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mAdaptiveCharging = (SystemSettingMasterSwitchPreference) screen.findPreference(KEY_ADAPTIVE_CHARGING);
        mAdaptiveCharging.setChecked((Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING, 0) == 1));
        mAdaptiveCharging.setOnPreferenceChangeListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mAdaptiveCharging != null) {
            mAdaptiveCharging.setChecked((Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING, 0) == 1));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING, value ? 1 : 0);
        updateState(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING, 0) == 1;
        int resId = enabled ? R.string.adaptive_charging_summary : R.string.switch_off_text;
        return mContext.getResources().getText(resId);
    }
}
