/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.fastcharge;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.superior.support.preferences.CustomSeekBarPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.lang.Integer;
import java.util.NoSuchElementException;

import vendor.lineage.fastcharge.V1_0.IRestrictedCurrent;
import vendor.lineage.fastcharge.V1_0.IFastCharge;

@SearchIndexable
public class FastChargingSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "FastChargingSettings";

    private static final String KEY_FAST_CHARGING = "fast_charging_switch";
    private static final String KEY_RESTRICTED_CURRENT = "restricted_current";

    private SwitchPreference mFastChargingSwitch;
    private CustomSeekBarPreference mRestrictedCurrentSeekBar;

    private IFastCharge mFastChargeService = null;
    private IRestrictedCurrent mRestrictedCurrentService = null;
    private int mMinCurrent = 100;
    private int mMaxCurrent = 0;
    private int mRestrictedCurrent = 0;
    private int mDefaultCurrent = 0;
    private boolean mFastChargeEnabled = true; //default fastcharge status

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fast_charging_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        mFastChargingSwitch = findPreference(KEY_FAST_CHARGING);
        mRestrictedCurrentSeekBar = findPreference(KEY_RESTRICTED_CURRENT);

        try {
            mFastChargeService = IFastCharge.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.e(TAG, "Failed to get IFastCharge interface", e);
            if (mFastChargingSwitch != null) {
                prefScreen.removePreference(mFastChargingSwitch);
            }
            if (mRestrictedCurrentSeekBar != null) {
                prefScreen.removePreference(mRestrictedCurrentSeekBar);
            }
            return;
        }

        mFastChargeEnabled = isFastChargeEnabled();
        mFastChargingSwitch.setChecked(mFastChargeEnabled);
        mFastChargingSwitch.setOnPreferenceChangeListener(this);

        try {
            mRestrictedCurrentService = IRestrictedCurrent.getService();
        } catch (NoSuchElementException | RemoteException e) {
            Log.d(TAG, "Failed to get IRestrictedCurrent interface", e);
            if (mRestrictedCurrentSeekBar != null) {
                prefScreen.removePreference(mRestrictedCurrentSeekBar);
            }
            return;
        }

        mMaxCurrent = getMaxSupportedCurrent();
        mDefaultCurrent = (mMaxCurrent + mMinCurrent) / 2;

        mRestrictedCurrentSeekBar.setMin(mMinCurrent);
        mRestrictedCurrentSeekBar.setMax(mMaxCurrent);
        mRestrictedCurrentSeekBar.setDefaultValue(mDefaultCurrent);

        mRestrictedCurrent = getRestrictedCurrent();
        mRestrictedCurrentSeekBar.setValue(mRestrictedCurrent);

        mRestrictedCurrentSeekBar.setEnabled(!mFastChargeEnabled);
        mRestrictedCurrentSeekBar.setOnPreferenceChangeListener(this);
    }

    private boolean isFastChargeEnabled() {
        try {
            return mFastChargeService.isEnabled();
        } catch (RemoteException e) {
            Log.e(TAG, "isEnabled failed", e);
        }
        return mFastChargeEnabled;
    }

    private boolean setEnabled(boolean enable) {
        boolean success = false;
        try {
            success=mFastChargeService.setEnabled(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setEnabled failed", e);
        }
        return success;
    }

    private int getRestrictedCurrent() {
        int current;
        try {
            current = mRestrictedCurrentService.getRestrictedCurrent();
        } catch (RemoteException e) {
            Log.e(TAG, "getRestrictedCurrent failed", e);
            return mRestrictedCurrent;
        }
        if (current < mMinCurrent) {
            current = mMinCurrent;
        } else if (current > mMaxCurrent) {
            current = mMaxCurrent;
        }
        return current;
    }

    private boolean setRestrictedCurrent(int current) {
        boolean success = false;
        try {
            success = mRestrictedCurrentService.setRestrictedCurrent(current);
        } catch (RemoteException e) {
            Log.e(TAG, "setRestrictedCurrent failed", e);
        }
        return success;
    }

    private int getMaxSupportedCurrent() {
        try {
            return mRestrictedCurrentService.getMaxSupportedCurrent();
        } catch (RemoteException e) {
            Log.e(TAG, "getMaxSupportedCurrent failed", e);
        }
        return mMaxCurrent;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (preference == mFastChargingSwitch) {
            boolean enable = (Boolean) newValue;
            if (setEnabled(enable)) mFastChargeEnabled = enable; /* update only if set succeeds */
            mFastChargingSwitch.setChecked(mFastChargeEnabled);
            mRestrictedCurrentSeekBar.setEnabled(!mFastChargeEnabled);
            return true;
        } else if (preference == mRestrictedCurrentSeekBar) {
            int current = (Integer) newValue;
            if (setRestrictedCurrent(current)) mRestrictedCurrent = current; /* update only if set succeeds */
            mRestrictedCurrentSeekBar.setValue(mRestrictedCurrent);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SUPERIOR;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.fast_charging_settings);

}
