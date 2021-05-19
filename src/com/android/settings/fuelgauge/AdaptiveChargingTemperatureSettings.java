/*
 * Copyright (C) 2021 Palladium-OS
 * Copyright (C) 2021 Havoc-OS
 * Copyright (C) 2021 The Evolution X Project
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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import com.evolution.settings.preference.CustomSeekBarPreference;
import com.evolution.settings.preference.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Adapative Charging Temperature
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AdaptiveChargingTemperatureSettings extends DashboardFragment implements
        OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "AdaptiveChargingTemperatureSettings";
    private static final String KEY_ADAPTIVE_CHARGING_CUTOFF_TEMPERATURE = "adaptive_charging_cutoff_temperature";
    private static final String KEY_ADAPTIVE_CHARGING_RESUME_TEMPERATURE = "adaptive_charging_resume_temperature";

    private CustomSeekBarPreference mAdaptiveChargingCutoffTemperature;
    private CustomSeekBarPreference mAdaptiveChargingResumeTemperature;

    private int mAdaptiveChargingCutoffTemperatureConfig;
    private int mAdaptiveChargingResumeTemperatureConfig;

    private TextView mTextView;
    private View mSwitchBar;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mAdaptiveChargingCutoffTemperatureConfig = getResources().getInteger(
                com.android.internal.R.integer.config_adaptiveChargingCutoffTemperature);
        mAdaptiveChargingResumeTemperatureConfig = getResources().getInteger(
                com.android.internal.R.integer.config_adaptiveChargingResumeTemperature);

        mAdaptiveChargingCutoffTemperature = (CustomSeekBarPreference) findPreference(KEY_ADAPTIVE_CHARGING_CUTOFF_TEMPERATURE);
        int currentCutoffTemperature = Settings.System.getInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_CUTOFF_TEMPERATURE, mAdaptiveChargingCutoffTemperatureConfig);
        mAdaptiveChargingCutoffTemperature.setValue(currentCutoffTemperature);
        mAdaptiveChargingCutoffTemperature.setOnPreferenceChangeListener(this);

        mAdaptiveChargingResumeTemperature = (CustomSeekBarPreference) findPreference(KEY_ADAPTIVE_CHARGING_RESUME_TEMPERATURE);
        int currentResumeTemperature = Settings.System.getInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_RESUME_TEMPERATURE, mAdaptiveChargingResumeTemperatureConfig);
        mAdaptiveChargingResumeTemperature.setMax(currentCutoffTemperature - 1);
        if (currentResumeTemperature >= currentCutoffTemperature) currentResumeTemperature = currentCutoffTemperature -1;
        mAdaptiveChargingResumeTemperature.setValue(currentResumeTemperature);
        mAdaptiveChargingResumeTemperature.setOnPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.master_setting_switch, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_TEMPERATURE, 0) == 1;

        mTextView = view.findViewById(R.id.switch_text);
        mTextView.setText(getString(enabled ?
                R.string.switch_on_text : R.string.switch_off_text));

        mSwitchBar = view.findViewById(R.id.switch_bar);
        Switch switchWidget = mSwitchBar.findViewById(android.R.id.switch_widget);
        switchWidget.setChecked(enabled);
        switchWidget.setOnCheckedChangeListener(this);
        mSwitchBar.setActivated(enabled);
        mSwitchBar.setOnClickListener(v -> {
            switchWidget.setChecked(!switchWidget.isChecked());
            mSwitchBar.setActivated(switchWidget.isChecked());
        });

        mAdaptiveChargingCutoffTemperature.setEnabled(enabled);
        mAdaptiveChargingResumeTemperature.setEnabled(enabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_TEMPERATURE, isChecked ? 1 : 0);
        mTextView.setText(getString(isChecked ? R.string.switch_on_text : R.string.switch_off_text));
        mSwitchBar.setActivated(isChecked);

        mAdaptiveChargingCutoffTemperature.setEnabled(isChecked);
        mAdaptiveChargingResumeTemperature.setEnabled(isChecked);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_charging_temperature;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVO_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mAdaptiveChargingCutoffTemperature) {
            int adaptiveChargingCutoffTemperature = (Integer) objValue;
            int adaptiveChargingResumeTemperature = Settings.System.getInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_RESUME_TEMPERATURE, mAdaptiveChargingResumeTemperatureConfig);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_CUTOFF_TEMPERATURE, adaptiveChargingCutoffTemperature);
            mAdaptiveChargingResumeTemperature.setMax(adaptiveChargingCutoffTemperature - 1);
            if (adaptiveChargingCutoffTemperature <= adaptiveChargingResumeTemperature) {
                mAdaptiveChargingResumeTemperature.setValue(adaptiveChargingCutoffTemperature - 1);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ADAPTIVE_CHARGING_RESUME_TEMPERATURE, adaptiveChargingCutoffTemperature - 1);
            }
            return true;
        } else if (preference == mAdaptiveChargingResumeTemperature) {
            int adaptiveChargingResumeTemperature = (Integer) objValue;
            int adaptiveChargingCutoffTemperature = Settings.System.getInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_CUTOFF_TEMPERATURE, mAdaptiveChargingCutoffTemperatureConfig);
            mAdaptiveChargingResumeTemperature.setMax(adaptiveChargingCutoffTemperature - 1);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_RESUME_TEMPERATURE, adaptiveChargingResumeTemperature);
            return true;
        } else {
            return false;
        }
    }

    /**
     * For Search.
     */

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.adaptive_charging_temperature);
}
