/*
 * Copyright (C) 2023 ArrowOS
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

package com.android.settings.fuelgauge.batterysaver;

import static android.provider.Settings.System.LOW_POWER_DISABLE_5G;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.Arrays;

public class BatterySaver5gPreferenceController extends TogglePreferenceController {

    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public BatterySaver5gPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                LOW_POWER_DISABLE_5G, 1, UserHandle.USER_CURRENT) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                LOW_POWER_DISABLE_5G, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getAvailabilityStatus() {
        return Arrays.stream(mSubscriptionManager.getActiveSubscriptionIdList())
                .anyMatch(this::is5gAvailable) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean is5gAvailable(int subId) {
        return (mTelephonyManager.createForSubscriptionId(subId).getSupportedRadioAccessFamily()
                & TelephonyManager.NETWORK_TYPE_BITMASK_NR) != 0;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
