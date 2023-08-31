/*
 * Copyright (C) 2023 The LineageOS Project
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

package com.android.settings.evolution.health;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.android.internal.lineage.health.HealthInterface;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.evolution.settings.preference.CustomDialogPreference;
import com.evolution.settings.preference.SystemSettingDropDownPreference;
import com.evolution.settings.preference.SystemSettingMainSwitchPreference;

import static com.android.internal.lineage.health.HealthInterface.MODE_AUTO;
import static com.android.internal.lineage.health.HealthInterface.MODE_MANUAL;
import static com.android.internal.lineage.health.HealthInterface.MODE_LIMIT;

@SearchIndexable
public class ChargingControlSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = ChargingControlSettings.class.getSimpleName();

    private static final String CHARGING_CONTROL_PREF = "charging_control";
    private static final String CHARGING_CONTROL_ENABLED_PREF = "charging_control_enabled";
    private static final String CHARGING_CONTROL_MODE_PREF = "charging_control_mode";
    private static final String CHARGING_CONTROL_START_TIME_PREF = "charging_control_start_time";
    private static final String CHARGING_CONTROL_TARGET_TIME_PREF = "charging_control_target_time";
    private static final String CHARGING_CONTROL_LIMIT_PREF = "charging_control_charging_limit";

    private SystemSettingMainSwitchPreference mChargingControlEnabledPref;
    private SystemSettingDropDownPreference mChargingControlModePref;
    private StartTimePreference mChargingControlStartTimePref;
    private TargetTimePreference mChargingControlTargetTimePref;
    private ChargingLimitPreference mChargingControlLimitPref;

    private HealthInterface mHealthInterface;

    private static final int MENU_RESET = Menu.FIRST;

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Resources res = getResources();

        addPreferencesFromResource(R.xml.charging_control_settings);
        getActivity().getActionBar().setTitle(R.string.charging_control_title);

        mHealthInterface = HealthInterface.getInstance(getActivity());

        final PreferenceScreen prefSet = getPreferenceScreen();

        mChargingControlEnabledPref = prefSet.findPreference(CHARGING_CONTROL_ENABLED_PREF);
        mChargingControlEnabledPref.setOnPreferenceChangeListener(this);
        mChargingControlModePref = prefSet.findPreference(CHARGING_CONTROL_MODE_PREF);
        mChargingControlModePref.setOnPreferenceChangeListener(this);
        mChargingControlStartTimePref = prefSet.findPreference(CHARGING_CONTROL_START_TIME_PREF);
        mChargingControlTargetTimePref = prefSet.findPreference(CHARGING_CONTROL_TARGET_TIME_PREF);
        mChargingControlLimitPref = prefSet.findPreference(CHARGING_CONTROL_LIMIT_PREF);

        if (mChargingControlLimitPref != null) {
            if (mHealthInterface.allowFineGrainedSettings()) {
                mChargingControlModePref.setEntries(concatStringArrays(
                        mChargingControlModePref.getEntries(),
                        res.getStringArray(
                                R.array.charging_control_mode_entries_fine_grained_control)));
                mChargingControlModePref.setEntryValues(concatStringArrays(
                        mChargingControlModePref.getEntryValues(),
                        res.getStringArray(
                                R.array.charging_control_mode_values_fine_grained_control)));
            }
        }

        setHasOptionsMenu(true);

        refreshValues();

    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVO_SETTINGS;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    private void refreshValues() {
        if (mChargingControlEnabledPref != null) {
            mChargingControlEnabledPref.setChecked(mHealthInterface.getEnabled());
        }

        if (mChargingControlModePref != null) {
            final int chargingControlMode = mHealthInterface.getMode();
            mChargingControlModePref.setValue(Integer.toString(chargingControlMode));
            refreshUi();
        }

        if (mChargingControlStartTimePref != null) {
            mChargingControlStartTimePref.setValue(
                    mChargingControlStartTimePref.getTimeSetting());
        }

        if (mChargingControlTargetTimePref != null) {
            mChargingControlTargetTimePref.setValue(
                    mChargingControlTargetTimePref.getTimeSetting());
        }

        if (mChargingControlLimitPref != null) {
            mChargingControlLimitPref.setValue(
                    mChargingControlLimitPref.getSetting());
        }
    }

    private void refreshUi() {
        final int chargingControlMode = mHealthInterface.getMode();

        refreshUi(chargingControlMode);
    }

    private void refreshUi(final int chargingControlMode) {
        String summary = null;
        boolean isChargingControlStartTimePrefVisible = false;
        boolean isChargingControlTargetTimePrefVisible = false;
        boolean isChargingControlLimitPrefVisible = false;

        final Resources res = getResources();

        switch (chargingControlMode) {
            case MODE_AUTO:
                summary = res.getString(R.string.charging_control_mode_auto_summary);
                break;
            case MODE_MANUAL:
                summary = res.getString(R.string.charging_control_mode_custom_summary);
                isChargingControlStartTimePrefVisible = true;
                isChargingControlTargetTimePrefVisible = true;
                break;
            case MODE_LIMIT:
                summary = res.getString(R.string.charging_control_mode_limit_summary);
                isChargingControlLimitPrefVisible = true;
                break;
            default:
                return;
        }

        mChargingControlModePref.setSummary(summary);

        if (mChargingControlStartTimePref != null) {
            mChargingControlStartTimePref.setVisible(isChargingControlStartTimePrefVisible);
        }

        if (mChargingControlTargetTimePref != null) {
            mChargingControlTargetTimePref.setVisible(isChargingControlTargetTimePrefVisible);
        }

        if (mChargingControlLimitPref != null) {
            mChargingControlLimitPref.setVisible(isChargingControlLimitPrefVisible);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == MENU_RESET) {
            resetToDefaults();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object objValue) {
        if (preference == mChargingControlEnabledPref) {
            mHealthInterface.setEnabled((Boolean) objValue);
        } else if (preference == mChargingControlModePref) {
            final int chargingControlMode = Integer.parseInt((String) objValue);
            mHealthInterface.setMode(chargingControlMode);
            refreshUi(chargingControlMode);
        }
        return true;
    }

    private void resetToDefaults() {
        mHealthInterface.reset();

        refreshValues();
    }

    private CharSequence[] concatStringArrays(CharSequence[] array1, CharSequence[] array2) {
        return Stream.concat(Arrays.stream(array1), Arrays.stream(array2)).toArray(size ->
                (CharSequence[]) Array.newInstance(CharSequence.class, size));
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> result = new ArrayList<String>();
            if (!HealthInterface.isChargingControlSupported(context)) {
                result.add(CHARGING_CONTROL_PREF);
                result.add(CHARGING_CONTROL_ENABLED_PREF);
                result.add(CHARGING_CONTROL_MODE_PREF);
                result.add(CHARGING_CONTROL_START_TIME_PREF);
                result.add(CHARGING_CONTROL_TARGET_TIME_PREF);
                result.add(CHARGING_CONTROL_LIMIT_PREF);
            }
            return result;
        }
    };

}
