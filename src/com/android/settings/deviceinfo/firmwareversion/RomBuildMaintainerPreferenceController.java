/*
 * Copyright (C) 2019-2020 The Evolution X Project
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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class RomBuildMaintainerPreferenceController extends BasePreferenceController {

    private static final String TAG = "RomMaintainerCtrl";
    private static final String KEY_BUILD_MAINTAINER_URL =
        "org.evolution.build_donate_url";
    private static final String OTA_BUILD_TYPE_PROP = "org.evolution.build_type";

    private final PackageManager mPackageManager;

    private String mDeviceMaintainer;

    public RomBuildMaintainerPreferenceController(Context context, String key) {
        super(context, key);
        mDeviceMaintainer = SystemProperties.get("org.evolution.build_maintainer");
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        String buildtype = SystemProperties.get(OTA_BUILD_TYPE_PROP,"unofficial");
        if (TextUtils.isEmpty(mDeviceMaintainer) || !buildtype.equalsIgnoreCase("official")) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mDeviceMaintainer;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        String maintainerUrl = SystemProperties.get(KEY_BUILD_MAINTAINER_URL,
                mContext.getString(R.string.unknown));
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(maintainerUrl));
        if (mPackageManager.queryIntentActivities(intent, 0).isEmpty()) {
            // Don't send out the intent to stop crash
            Log.w(TAG, "queryIntentActivities() returns empty");
            return true;
        }

        mContext.startActivity(intent);
        return true;
    }
}
