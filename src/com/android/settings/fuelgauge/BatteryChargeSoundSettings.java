/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.evolution.settings.preference.CustomSeekBarPreference;

public class BatteryChargeSoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final int REQUEST_CODE_RINGTONE = 1;
    private static final String KEY_BATTERY_LEVEL_CHARGE_ALARM_ENABLED = "battery_level_charge_alarm_enabled";
    private static final String KEY_BATTERY_LEVEL_CHARGE_RINGTONE = "battery_level_charge_ringtone";
    private static final String KEY_BATTERY_LEVEL_CHARGE_SEEK_BAR = "battery_level_charge_seek_bar";
    private static final String KEY_SOUND_SILENT = "silent";

    private CustomSeekBarPreference mBatteryLevelChargeSeekbar;
    private Preference mBatteryLevelChargeRingtone;
    private SwitchPreference mBatteryLevelChargeAlarmEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_charge_level_settings);

        mBatteryLevelChargeAlarmEnabled = (SwitchPreference) findPreference(KEY_BATTERY_LEVEL_CHARGE_ALARM_ENABLED);
        mBatteryLevelChargeAlarmEnabled.setChecked(Settings.System.getIntForUser(getContentResolver(),
                Settings.System.BATTERY_LEVEL_CHARGE_ALARM_ENABLED, 0, UserHandle.USER_CURRENT) == 1);
        mBatteryLevelChargeAlarmEnabled.setOnPreferenceChangeListener(this);

        mBatteryLevelChargeSeekbar = (CustomSeekBarPreference) findPreference(KEY_BATTERY_LEVEL_CHARGE_SEEK_BAR);
        int batterylevelchargeseekbar = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.SEEK_BAR_BATTERY_CHARGE_LEVEL_SOUND, 100, UserHandle.USER_CURRENT);
        mBatteryLevelChargeSeekbar.setValue(batterylevelchargeseekbar);
        mBatteryLevelChargeSeekbar.setOnPreferenceChangeListener(this);

        mBatteryLevelChargeRingtone = (Preference) findPreference(KEY_BATTERY_LEVEL_CHARGE_RINGTONE);
        String curTone = Settings.Global.getString(getContentResolver(),
                Settings.Global.BATTERY_LEVEL_CHARGE_SOUND_ALARM);
        if (curTone == null) {
            updateChargeLevelRingtone(Settings.System.DEFAULT_NOTIFICATION_URI.toString(), true);
        } else {
            updateChargeLevelRingtone(curTone, false);
        }
    }

    private void updateChargeLevelRingtone(String toneUriString, boolean persist) {
        final String toneName;

        if (toneUriString != null && !toneUriString.equals(KEY_SOUND_SILENT)) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(),
                    Uri.parse(toneUriString));
            if (ringtone != null) {
                toneName = ringtone.getTitle(getActivity());
            } else {
                toneName = "";
                toneUriString = Settings.System.DEFAULT_NOTIFICATION_URI.toString();
                persist = true;
            }
        } else {
            toneName = getString(R.string.notification_battery_charge_level_silent);
            toneUriString = KEY_SOUND_SILENT;
        }

        mBatteryLevelChargeRingtone.setSummary(toneName);
        if (persist) {
            Settings.Global.putString(getContentResolver(),
                    Settings.Global.BATTERY_LEVEL_CHARGE_SOUND_ALARM, toneUriString);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mBatteryLevelChargeAlarmEnabled)) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.BATTERY_LEVEL_CHARGE_ALARM_ENABLED, enabled ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference.equals(mBatteryLevelChargeSeekbar)) {
            int val = (Integer) newValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.SEEK_BAR_BATTERY_CHARGE_LEVEL_SOUND, val, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.equals(mBatteryLevelChargeRingtone)) {
            batteryChargeSoundSoundPicker(REQUEST_CODE_RINGTONE,
                    Settings.Global.getString(getContentResolver(),
                    Settings.Global.BATTERY_LEVEL_CHARGE_SOUND_ALARM));
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void batteryChargeSoundSoundPicker(int requestCode, String toneUriString) {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.notification_battery_charge_level_ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                Settings.System.DEFAULT_NOTIFICATION_URI);
        if (toneUriString != null && !toneUriString.equals(KEY_SOUND_SILENT)) {
            Uri uri = Uri.parse(toneUriString);
            if (uri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
            }
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_RINGTONE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            updateChargeLevelRingtone(uri != null ? uri.toString() : null, true);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.EVO_SETTINGS;
    }

}
