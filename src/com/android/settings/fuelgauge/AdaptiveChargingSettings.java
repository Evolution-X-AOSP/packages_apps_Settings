/*
 * Copyright (C) 2019 RevengeOS
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
 * Settings screen for Adapative Charging
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AdaptiveChargingSettings extends DashboardFragment implements
        OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "AdaptiveChargingSettings";
    private static final String KEY_ADAPTIVE_CHARGING_CUTOFF_LEVEL = "adaptive_charging_cutoff_level";
    private static final String KEY_ADAPTIVE_CHARGING_RESUME_LEVEL = "adaptive_charging_resume_level";
    private static final String KEY_ADAPTIVE_CHARGING_RESET_STATS = "adaptive_charging_reset_stats";

    private CustomSeekBarPreference mAdaptiveChargingCutoffLevel;
    private CustomSeekBarPreference mAdaptiveChargingResumeLevel;
    private SystemSettingSwitchPreference mResetStats;

    private int mAdaptiveChargingCutoffLevelConfig;
    private int mAdaptiveChargingResumeLevelConfig;

    private TextView mTextView;
    private View mSwitchBar;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mAdaptiveChargingCutoffLevelConfig = getResources().getInteger(
                com.android.internal.R.integer.config_adaptiveChargingCutoffLevel);
        mAdaptiveChargingResumeLevelConfig = getResources().getInteger(
                com.android.internal.R.integer.config_adaptiveChargingResumeLevel);

        mAdaptiveChargingCutoffLevel = (CustomSeekBarPreference) findPreference(KEY_ADAPTIVE_CHARGING_CUTOFF_LEVEL);
        int currentCutoffLevel = Settings.System.getInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_CUTOFF_LEVEL, mAdaptiveChargingCutoffLevelConfig);
        mAdaptiveChargingCutoffLevel.setValue(currentCutoffLevel);
        mAdaptiveChargingCutoffLevel.setOnPreferenceChangeListener(this);

        mAdaptiveChargingResumeLevel = (CustomSeekBarPreference) findPreference(KEY_ADAPTIVE_CHARGING_RESUME_LEVEL);
        int currentResumeLevel = Settings.System.getInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING_RESUME_LEVEL, mAdaptiveChargingResumeLevelConfig);
        mAdaptiveChargingResumeLevel.setMax(currentCutoffLevel - 1);
        if (currentResumeLevel >= currentCutoffLevel) currentResumeLevel = currentCutoffLevel -1;
        mAdaptiveChargingResumeLevel.setValue(currentResumeLevel);
        mAdaptiveChargingResumeLevel.setOnPreferenceChangeListener(this);

        mResetStats = (SystemSettingSwitchPreference) findPreference(KEY_ADAPTIVE_CHARGING_RESET_STATS);
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
                Settings.System.ADAPTIVE_CHARGING, 0) == 1;

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

        mAdaptiveChargingCutoffLevel.setEnabled(enabled);
        mAdaptiveChargingResumeLevel.setEnabled(enabled);
        mResetStats.setEnabled(enabled);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.ADAPTIVE_CHARGING, isChecked ? 1 : 0);
        mTextView.setText(getString(isChecked ? R.string.switch_on_text : R.string.switch_off_text));
        mSwitchBar.setActivated(isChecked);

        mAdaptiveChargingCutoffLevel.setEnabled(isChecked);
        mAdaptiveChargingResumeLevel.setEnabled(isChecked);
        mResetStats.setEnabled(isChecked);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.adaptive_charging;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVO_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mAdaptiveChargingCutoffLevel) {
            int adaptiveChargingCutoffLevel = (Integer) objValue;
            int adaptiveChargingResumeLevel = Settings.System.getInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_RESUME_LEVEL, mAdaptiveChargingResumeLevelConfig);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_CUTOFF_LEVEL, adaptiveChargingCutoffLevel);
            mAdaptiveChargingResumeLevel.setMax(adaptiveChargingCutoffLevel - 1);
            if (adaptiveChargingCutoffLevel <= adaptiveChargingResumeLevel) {
                mAdaptiveChargingResumeLevel.setValue(adaptiveChargingCutoffLevel - 1);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ADAPTIVE_CHARGING_RESUME_LEVEL, adaptiveChargingCutoffLevel - 1);
            }
            return true;
        } else if (preference == mAdaptiveChargingResumeLevel) {
            int adaptiveChargingResumeLevel = (Integer) objValue;
            int adaptiveChargingCutoffLevel = Settings.System.getInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_CUTOFF_LEVEL, mAdaptiveChargingCutoffLevelConfig);
            mAdaptiveChargingResumeLevel.setMax(adaptiveChargingCutoffLevel - 1);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ADAPTIVE_CHARGING_RESUME_LEVEL, adaptiveChargingResumeLevel);
            return true;
        } else {
            return false;
        }
    }

    /**
     * For Search.
     */

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.adaptive_charging);
}
