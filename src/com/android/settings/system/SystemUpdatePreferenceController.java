/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class SystemUpdatePreferenceController extends BasePreferenceController {

    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";

    private static final String OTA_BUILD_TYPE_PROP = "org.evolution.build_type";
    private static final String OTA_APP_PACKAGE = "org.evolution.ota";

    private final UserManager mUm;

    public SystemUpdatePreferenceController(Context context) {
        super(context, KEY_SYSTEM_UPDATE_SETTINGS);
        mUm = UserManager.get(context);
    }

    @Override
    public int getAvailabilityStatus() {
        String buildtype = SystemProperties.get(OTA_BUILD_TYPE_PROP,"unofficial");
        if (!mUm.isAdminUser() || !buildtype.equalsIgnoreCase("official")){
            return UNSUPPORTED_ON_DEVICE;
        }
        try {
            PackageManager pm = mContext.getPackageManager();
            pm.getPackageInfo(OTA_APP_PACKAGE, PackageManager.GET_ACTIVITIES);
        } catch (Exception e) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            Utils.updatePreferenceToSpecificActivityOrRemove(mContext, screen,
                    getPreferenceKey(),
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = mContext.getString(R.string.android_version_summary,
                Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY);
        return summary;
    }

}
