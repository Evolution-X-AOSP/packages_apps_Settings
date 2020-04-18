/*
 * Copyright (C) 2019-2021 The Evolution X Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class EvolutionXBuildMaintainerPreferenceController extends BasePreferenceController {

    private static final String TAG = "EvolutionXBuildMaintainerCtrl";

    private String mDeviceMaintainer;

    public EvolutionXBuildMaintainerPreferenceController(Context context, String key) {
        super(context, key);
        mDeviceMaintainer = mContext.getResources().getString(R.string.build_maintainer_summary);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mDeviceMaintainer.equalsIgnoreCase("UNKNOWN")) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mDeviceMaintainer;
    }
}
