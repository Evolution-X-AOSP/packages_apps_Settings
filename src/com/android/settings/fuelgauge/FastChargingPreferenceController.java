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

import com.android.settings.core.BasePreferenceController;

import com.android.settings.R;

import vendor.lineage.fastcharge.V1_0.IFastCharge;

import java.util.NoSuchElementException;

public class FastChargingPreferenceController extends BasePreferenceController {

    private static final String KEY_FAST_CHARGING = "fast_charging";
    private static final String TAG = "FastChargingPreferenceController";

    private final boolean DEBUG = false;
    private Context mContext = null;

    public FastChargingPreferenceController(Context context) {
        super(context, KEY_FAST_CHARGING);
        mContext = context;
        if (!mContext.getResources().getBoolean(R.bool.config_lineageFastChargeSupported)) return;
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            if (!mContext.getResources().getBoolean(R.bool.config_lineageFastChargeSupported)) return UNSUPPORTED_ON_DEVICE;
            return IFastCharge.getService() != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (NoSuchElementException | RemoteException e) {
            if (DEBUG) Log.e(TAG, "Failed to get IFastCharge interface", e);
        }
        return UNSUPPORTED_ON_DEVICE;
    }
}
