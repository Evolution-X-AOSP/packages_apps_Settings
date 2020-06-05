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
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class AboutDeviceNamePreferenceController extends BasePreferenceController {

    private static final String TAG = "AboutDeviceNameCtrl";

    private static final String KEY_BRAND_NAME_PROP = "ro.product.manufacturer";
    private static final String KEY_DEVICE_NAME_PROP = "org.evolution.device";
    private static final String KEY_SUPPORT_URL = "org.evolution.build_support_url";

    private final PackageManager mPackageManager;

    public AboutDeviceNamePreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        String deviceBrand = SystemProperties.get(KEY_BRAND_NAME_PROP,
                mContext.getString(R.string.device_info_default));
        String deviceCodename = SystemProperties.get(KEY_DEVICE_NAME_PROP,
                mContext.getString(R.string.device_info_default));
        String deviceModel = Build.MODEL;
        return deviceBrand + " " + deviceModel + " | " + deviceCodename;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        String Url = SystemProperties.get(KEY_SUPPORT_URL,
                mContext.getString(R.string.device_info_default));
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(Url));
        if (mPackageManager.queryIntentActivities(intent, 0).isEmpty()) {
            // Don't send out the intent to stop crash
            Log.w(TAG, "queryIntentActivities() returns empty");
            return true;
        }

        mContext.startActivity(intent);
        return true;
    }
}
