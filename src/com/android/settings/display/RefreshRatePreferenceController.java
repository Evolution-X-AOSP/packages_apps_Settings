/*
 * Copyright (C) 2020 The PixelExperience Project
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

import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

public class RefreshRatePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final static String REFRESH_RATE_AUTOMATIC = "0";
    private final static String REFRESH_RATE_60 = "1";
    private final static String REFRESH_RATE_CUSTOM = "2";

    private ListPreference mPref;

    public RefreshRatePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPref = screen.findPreference(getPreferenceKey());
        String titleAutomatic = mContext.getResources().getString(R.string.refresh_rate_auto);
        String title60Hz = mContext.getResources().getString(R.string.refresh_rate_60);
        CharSequence[] entries = {titleAutomatic, title60Hz, getDefaultPeakRefreshRate() + "Hz"};
        CharSequence[] entryValues = {REFRESH_RATE_AUTOMATIC, REFRESH_RATE_60, REFRESH_RATE_CUSTOM};
        mPref.setEntries(entries);
        mPref.setEntryValues(entryValues);
        mPref.setValueIndex(valueToIndex(getCurrentValue()));
    }

    @Override
    public int getAvailabilityStatus() {
        boolean supported = mContext.getResources().getBoolean(R.bool.config_supportsRefreshRateSwitch);
        return supported && getDefaultPeakRefreshRate() != 60 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        if (value.equals(REFRESH_RATE_60)){
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE, 60);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, 60);
        }else if (value.equals(REFRESH_RATE_CUSTOM)){
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE, getDefaultPeakRefreshRate());
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, getDefaultPeakRefreshRate());
        }else{
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE, getDefaultPeakRefreshRate());
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, 60);
        }
        return true;
    }

    private int getDefaultPeakRefreshRate(){
        return mContext.getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
    }

    private String getCurrentValue(){
        int defaultPeakRate = getDefaultPeakRefreshRate();
        int minRefreshRate = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, 0);
        int peakRefreshRate = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, defaultPeakRate);
        if (minRefreshRate == 60 && peakRefreshRate == 60){
            return REFRESH_RATE_60;
        }else if (minRefreshRate == defaultPeakRate && peakRefreshRate == defaultPeakRate){
            return REFRESH_RATE_CUSTOM;
        }else{
            return REFRESH_RATE_AUTOMATIC;
        }
    }

    private int valueToIndex(String value){
        if (value.equals(REFRESH_RATE_60)){
            return 1;
        }else if (value.equals(REFRESH_RATE_CUSTOM)){
            return 2;
        }else{
            return 0;
        }
    }

}
