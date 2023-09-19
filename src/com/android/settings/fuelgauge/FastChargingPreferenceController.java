/*
 * Copyright (C) 2021 The LineageOS Project
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
import android.os.RemoteException;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.android.settingslib.core.lifecycle.events.OnStart;

import java.util.NoSuchElementException;

import vendor.lineage.fastcharge.V1_0.IFastCharge;
import vendor.lineage.fastcharge.V1_0.IRestrictedCurrent;

public class FastChargingPreferenceController extends BasePreferenceController
        implements OnStart {

    private static final String KEY_FAST_CHARGING = "fast_charging";
    private static final String TAG = "FastChargingPreferenceController";

    private final boolean DEBUG = false;

    private IFastCharge mFastChargeService = null;
    private IRestrictedCurrent mRestrictedCurrentService = null;

    private Preference mFastChargePref;
    private boolean mEnabled = true;

    public FastChargingPreferenceController(Context context) {
        super(context, KEY_FAST_CHARGING);
        String iStr = "IFastCharge";
        if (!mContext.getResources().getBoolean(R.bool.config_lineageFastChargeSupported)) return;
        try {
            mFastChargeService = IFastCharge.getService();
            iStr = "IRestrictedCurrent";
            mRestrictedCurrentService = IRestrictedCurrent.getService();
        } catch (NoSuchElementException | RemoteException e) {
            if (DEBUG) Log.e(TAG, "Failed to get " + iStr + " interface", e);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mFastChargePref = screen.findPreference(KEY_FAST_CHARGING);
    }

    @Override
    public void onStart() {
        updateSummary();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_lineageFastChargeSupported)) return UNSUPPORTED_ON_DEVICE;
        return mFastChargeService != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled;
        int current;
        try {
            enabled = mFastChargeService.isEnabled();
        } catch (RemoteException e) {
            if (DEBUG) Log.e(TAG, "isEnabled failed", e);
            return "";
        }
        if (enabled) {
            return mContext.getString(R.string.string_enabled);
        }

        try {
            current = mRestrictedCurrentService.getRestrictedCurrent();
        } catch (RemoteException e) {
            if (DEBUG) Log.e(TAG, "getRestrictedCurrent failed", e);
            return mContext.getString(R.string.string_disabled);
        }

        return mContext.getString(R.string.fastcharge_disabled_extended_summary,
                String.valueOf(current) + " mA");
    }

    private void updateSummary() {
        if (mFastChargePref != null) {
            mFastChargePref.setSummary(getSummary());
        }
    }
}
