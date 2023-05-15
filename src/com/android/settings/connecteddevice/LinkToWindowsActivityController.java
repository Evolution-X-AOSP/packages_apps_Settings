/*
 * Copyright (C) 2023 Microsoft Corporation
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

package com.android.settings.connecteddevice;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.UserManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.WorkerThread;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import android.util.Log;

/**
 * Controller that used to show LinkToWindows features
 */
public class LinkToWindowsActivityController extends BasePreferenceController {
    private static final String TAG = "LinkToWindowsActivityController";

    private static final String DEVICE_INTEGRATION_SERVICE = "com.microsoft.deviceintegrationservice";
    private static final String APP_MANAGER = "com.microsoft.appmanager";
    private static final String ACTIVITY_NAME = "com.microsoft.appmanager.StartUpActivity";
    private static final String ACTION_NAME = "android.service.quicksettings.action.QS_TILE_PREFERENCES";
    private static final String URI = "content://com.microsoft.appmanager.quickSettingsProvider";
    private static final String METHOD_IS_TILE_ACTIVE = "method_is_tile_active";
    private static final String PARAM_ACTIVE = "param_active";

    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private final Context mContext;

    public LinkToWindowsActivityController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mPackageManager = context.getPackageManager();
        mUserManager = context.getSystemService(UserManager.class);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        int state = UNSUPPORTED_ON_DEVICE;

        try {
            mPackageManager.getApplicationInfo(DEVICE_INTEGRATION_SERVICE, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException");
            return UNSUPPORTED_ON_DEVICE;
        }

        try {
            state = mPackageManager.getApplicationEnabledSetting(APP_MANAGER);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException");
            return UNSUPPORTED_ON_DEVICE;
        }

        switch(state) {
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
                return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (isTileActive(mContext)) {
            return mContext.getText(R.string.link_to_windows_setting_on);
        } else {
            return mContext.getText(R.string.link_to_windows_setting_off);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        try {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage(preference.getFragment());
            intent.setComponent(new ComponentName(preference.getFragment(), ACTIVITY_NAME));
            intent.setAction(ACTION_NAME);
            preference.getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found to handle the intent");
        }

        return true;
    }

    @WorkerThread
    private boolean isTileActive(Context context) {
        if(context == null || this.getAvailabilityStatus() == UNSUPPORTED_ON_DEVICE){
            return false;
        }
        try {
            Bundle statusBundle = context.getContentResolver().call(
                    Uri.parse(URI),
                    METHOD_IS_TILE_ACTIVE,
                    context.getPackageName(),
                    null);
            return statusBundle.getBoolean(PARAM_ACTIVE);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException");
            return false;
        }
    }
}
 