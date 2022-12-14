/*
 * Copyright (C) 2021 Havoc-OS
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

package com.android.settings.fuelgauge.sleepmode;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Switch;
import android.widget.TimePicker;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.evolution.settings.preference.CustomSecureSettingMainSwitchPreference;

import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public class SleepMode extends SettingsPreferenceFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, OnMainSwitchChangeListener {

    private static final String ENABLE_KEY = "sleep_mode_enabled";
    private static final String MODE_KEY = "sleep_mode_auto_mode";
    private static final String SINCE_PREF_KEY = "sleep_mode_auto_since";
    private static final String TILL_PREF_KEY = "sleep_mode_auto_till";
    private static final String TOGGLES_CATEGORY_KEY = "sleep_mode_toggles";

    private CustomSecureSettingMainSwitchPreference mEnable;
    private PreferenceCategory mToggles;
    private DropDownPreference mModePref;
    private Preference mSincePref;
    private Preference mTillPref;
    private Context mContext;
    private Handler mHandler;
    private ContentResolver mContentResolver;
    private boolean mIsNavSwitchingMode = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.sleep_mode_settings);
        PreferenceScreen screen = getPreferenceScreen();

        mContext = getContext();
        mHandler = new Handler();
        mContentResolver = getActivity().getContentResolver();

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();

        mEnable = findPreference(ENABLE_KEY);
        mEnable.addOnSwitchChangeListener(this);

        mSincePref = findPreference(SINCE_PREF_KEY);
        mSincePref.setOnPreferenceClickListener(this);
        mTillPref = findPreference(TILL_PREF_KEY);
        mTillPref.setOnPreferenceClickListener(this);

        final int mode = Settings.Secure.getIntForUser(mContentResolver,
                MODE_KEY, 0, UserHandle.USER_CURRENT);
        mModePref = (DropDownPreference) findPreference(MODE_KEY);
        mModePref.setValue(String.valueOf(mode));
        mModePref.setSummary(mModePref.getEntry());
        mModePref.setOnPreferenceChangeListener(this);

        mToggles = findPreference(TOGGLES_CATEGORY_KEY);

        updateTimeEnablement(mode);
        updateTimeSummary(mode);
        updateStateInternal();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mModePref) {
            final int value = Integer.parseInt((String) objValue);
            final int index = mModePref.findIndexOfValue((String) objValue);
            mModePref.setSummary(mModePref.getEntries()[index]);
            Settings.Secure.putIntForUser(mContentResolver,
                    MODE_KEY, value, UserHandle.USER_CURRENT);
            updateTimeEnablement(value);
            updateTimeSummary(value);
            updateStateInternal();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mSincePref || preference == mTillPref) {
            String[] times = getCustomTimeSetting();
            boolean isSince = preference == mSincePref;
            int hour, minute;
            TimePickerDialog.OnTimeSetListener listener = (view, hourOfDay, minute1) -> {
                updateTimeSetting(isSince, hourOfDay, minute1);
            };
            if (isSince) {
                String[] sinceValues = times[0].split(":", 0);
                hour = Integer.parseInt(sinceValues[0]);
                minute = Integer.parseInt(sinceValues[1]);
            } else {
                String[] tillValues = times[1].split(":", 0);
                hour = Integer.parseInt(tillValues[0]);
                minute = Integer.parseInt(tillValues[1]);
            }
            TimePickerDialog dialog = new TimePickerDialog(mContext, listener,
                    hour, minute, DateFormat.is24HourFormat(mContext));
            dialog.show();
            return true;
        }
        return false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        enableSleepMode(isChecked);
        updateStateInternal();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIsNavSwitchingMode = false;
            }
        }, 1500);
    }

    private String[] getCustomTimeSetting() {
        String value = Settings.Secure.getStringForUser(mContentResolver,
                Settings.Secure.SLEEP_MODE_AUTO_TIME, UserHandle.USER_CURRENT);
        if (value == null || value.equals("")) value = "22:00,07:00";
        return value.split(",", 0);
    }

    private void updateTimeEnablement(int mode) {
        mSincePref.setVisible(mode == 2 || mode == 4);
        mTillPref.setVisible(mode == 2 || mode == 3);
    }

    private void updateTimeSummary(int mode) {
        updateTimeSummary(getCustomTimeSetting(), mode);
    }

    private void updateTimeSummary(String[] times, int mode) {
        if (mode == 0) {
            mSincePref.setSummary("-");
            mTillPref.setSummary("-");
            return;
        }

        if (mode == 1) {
            mSincePref.setSummary(R.string.sleep_mode_schedule_sunset);
            mTillPref.setSummary(R.string.sleep_mode_schedule_sunrise);
            return;
        }

        final String outputFormat = DateFormat.is24HourFormat(mContext) ? "HH:mm" : "h:mm a";
        final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        final LocalTime sinceDT = LocalTime.parse(times[0], formatter);
        final LocalTime tillDT = LocalTime.parse(times[1], formatter);

        if (mode == 3) {
            mSincePref.setSummary(R.string.sleep_mode_schedule_sunset);
            mTillPref.setSummary(tillDT.format(outputFormatter));
        } else if (mode == 4) {
            mTillPref.setSummary(R.string.sleep_mode_schedule_sunrise);
            mSincePref.setSummary(sinceDT.format(outputFormatter));
        } else {
            mSincePref.setSummary(sinceDT.format(outputFormatter));
            mTillPref.setSummary(tillDT.format(outputFormatter));
        }
    }

    private void updateTimeSetting(boolean since, int hour, int minute) {
        String[] times = getCustomTimeSetting();
        String nHour = "";
        String nMinute = "";
        if (hour < 10) nHour += "0";
        if (minute < 10) nMinute += "0";
        nHour += String.valueOf(hour);
        nMinute += String.valueOf(minute);
        times[since ? 0 : 1] = nHour + ":" + nMinute;
        Settings.Secure.putStringForUser(mContentResolver,
                Settings.Secure.SLEEP_MODE_AUTO_TIME,
                times[0] + "," + times[1], UserHandle.USER_CURRENT);
        updateTimeSummary(times, Integer.parseInt(mModePref.getValue()));
    }

    private void updateStateInternal() {
        final int mode = Settings.Secure.getIntForUser(mContentResolver,
                MODE_KEY, 0, UserHandle.USER_CURRENT);
        final boolean isActivated = Settings.Secure.getIntForUser(mContentResolver,
                Settings.Secure.SLEEP_MODE_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        String timeValue = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_AUTO_TIME, UserHandle.USER_CURRENT);
        if (timeValue == null || timeValue.equals("")) timeValue = "20:00,07:00";
        final String[] time = timeValue.split(",", 0);
        final String outputFormat = DateFormat.is24HourFormat(mContext) ? "HH:mm" : "h:mm a";
        final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        final LocalTime sinceValue = LocalTime.parse(time[0], formatter);
        final LocalTime tillValue = LocalTime.parse(time[1], formatter);

        String switchText;
        switch (mode) {
            default:
            case 0:
                switchText = mContext.getString(isActivated ? R.string.night_display_activation_off_manual
                        : R.string.night_display_activation_on_manual);
                break;
            case 1:
                switchText = mContext.getString(isActivated ? R.string.night_display_activation_off_twilight
                        : R.string.night_display_activation_on_twilight);
                break;
            case 2:
                if (isActivated) {
                    switchText = mContext.getString(R.string.night_display_activation_off_custom, sinceValue.format(outputFormatter));
                } else {
                    switchText = mContext.getString(R.string.night_display_activation_on_custom, tillValue.format(outputFormatter));
                }
                break;
            case 3:
                if (isActivated) {
                    switchText = mContext.getString(R.string.night_display_activation_off_twilight);
                } else {
                    switchText = mContext.getString(R.string.night_display_activation_on_custom, tillValue.format(outputFormatter));
                }
                break;
            case 4:
                if (isActivated) {
                    switchText = mContext.getString(R.string.night_display_activation_off_custom, sinceValue.format(outputFormatter));
                } else {
                    switchText = mContext.getString(R.string.night_display_activation_on_twilight);
                }
                break;
        }

        mEnable.setTitle(switchText);
        mToggles.setEnabled(!isActivated);
    }

    private void enableSleepMode(boolean enable) {
        if (mIsNavSwitchingMode) return;
        mIsNavSwitchingMode = true;
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_ENABLED, enable ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContentResolver;
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.SLEEP_MODE_ENABLED), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.SLEEP_MODE_AUTO_MODE), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateStateInternal();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.EVO_SETTINGS;
    }
}
