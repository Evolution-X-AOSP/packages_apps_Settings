/*
 * Copyright (C) 2019-2022 Evolution X
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
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class EvolutionXBuildStatusPreferenceController extends BasePreferenceController {

    private static final String TAG = "EvolutionXBuildStatusCtrl";

    private String mBuildExclusive;
    private String mBuildExclusiveTest;
    private String mBuildTest;

    public EvolutionXBuildStatusPreferenceController(Context context, String key) {
        super(context, key);
        mBuildExclusive = mContext.getResources().getString(R.string.build_status_exclusive);
        mBuildExclusiveTest = mContext.getResources().getString(R.string.build_status_exclusive_test);
        mBuildTest = mContext.getResources().getString(R.string.build_status_test);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_build_status)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (mContext.getResources().getBoolean(R.bool.config_show_build_status)
                && mContext.getResources().getBoolean(R.bool.config_show_build_status_exclusive)) {
            return mBuildExclusive;
        } else if (mContext.getResources().getBoolean(R.bool.config_show_build_status)
                && mContext.getResources().getBoolean(R.bool.config_show_build_status_exclusive_test)) {
            return mBuildExclusiveTest;
        } else if (mContext.getResources().getBoolean(R.bool.config_show_build_status)
                && mContext.getResources().getBoolean(R.bool.config_show_build_status_test)) {
            return mBuildTest;
        } else {
            return "";
        }
    }
}
