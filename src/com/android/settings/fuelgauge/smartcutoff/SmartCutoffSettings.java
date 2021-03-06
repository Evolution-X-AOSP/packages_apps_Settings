/*
 * Copyright (C) 2021 Palladium-OS
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

package com.android.settings.fuelgauge.smartcutoff;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.evolution.settings.preference.CustomSeekBarPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Smart Cutoff
 */
public class SmartCutoffSettings extends DashboardFragment implements OnPreferenceChangeListener {

    private static final String TAG = "SmartCutoffSettings";
    private static final String KEY_SMART_CUTOFF_LEVEL = "smart_cutoff_level";
    private static final String KEY_SMART_CUTOFF_RESUME_LEVEL = "smart_cutoff_resume_level";

    private CustomSeekBarPreference mSmartCutoffTemperature;
    private CustomSeekBarPreference mSmartCutoffResumeTemperature;

    private int mSmartCutoffTemperatureDefaultConfig;
    private int mSmartCutoffResumeTemperatureConfig;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSmartCutoffTemperatureDefaultConfig = getResources().getInteger(
                com.android.internal.R.integer.config_smartCutoffTemperature);
        mSmartCutoffResumeTemperatureConfig = getResources().getInteger(
                com.android.internal.R.integer.config_smartCutoffResumeTemperature);

        mSmartCutoffTemperature = (CustomSeekBarPreference) findPreference(KEY_SMART_CUTOFF_LEVEL);
        int currentTemperature = Settings.System.getInt(getContentResolver(),
            Settings.System.SMART_CUTOFF_TEMPERATURE, mSmartCutoffTemperatureDefaultConfig);
        mSmartCutoffTemperature.setValue(currentTemperature);
        mSmartCutoffTemperature.setOnPreferenceChangeListener(this);
        mSmartCutoffResumeTemperature = (CustomSeekBarPreference) findPreference(KEY_SMART_CUTOFF_RESUME_LEVEL);
        int currentResumeLevel = Settings.System.getInt(getContentResolver(),
            Settings.System.SMART_CUTOFF_RESUME_TEMPERATURE, mSmartCutoffResumeTemperatureConfig);
        mSmartCutoffResumeTemperature.setMax(currentTemperature - 1);
        if (currentResumeLevel >= currentTemperature) currentResumeLevel = currentTemperature -1;
        mSmartCutoffResumeTemperature.setValue(currentResumeLevel);
        mSmartCutoffResumeTemperature.setOnPreferenceChangeListener(this);

    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smart_cutoff;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVO_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mSmartCutoffTemperature) {
            int mCutoffResumeLevel = Settings.System.getInt(getContentResolver(),
                     Settings.System.SMART_CUTOFF_RESUME_TEMPERATURE, mSmartCutoffResumeTemperatureConfig);
            int smartCutoffTemperature = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CUTOFF_TEMPERATURE, smartCutoffTemperature);

            mSmartCutoffResumeTemperature.setMax(smartCutoffTemperature - 1);
            if (smartCutoffTemperature <= mCutoffResumeLevel) {
                mSmartCutoffResumeTemperature.setValue(smartCutoffTemperature - 1);
                Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CUTOFF_RESUME_TEMPERATURE, smartCutoffTemperature - 1);
            }
            return true;
        } else if (preference == mSmartCutoffResumeTemperature) {
            int smartCutoffResumeLevel = (Integer) objValue;
            int mCutoffLevel = Settings.System.getInt(getContentResolver(),
                     Settings.System.SMART_CUTOFF_TEMPERATURE, mSmartCutoffTemperatureDefaultConfig);
                mSmartCutoffResumeTemperature.setMax(mCutoffLevel - 1);
                Settings.System.putInt(getContentResolver(),
                    Settings.System.SMART_CUTOFF_RESUME_TEMPERATURE, smartCutoffResumeLevel);
            return true;
        } else {
            return false;
        }
    }
}
