/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.os.SELinux;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class SelinuxStatusPreferenceController extends BasePreferenceController {

    private static final String TAG = "SelinuxStatusCtrl";

    private String mStatus;

    public SelinuxStatusPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_selinux_status)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        int stringId = R.string.selinux_status_disabled;
        if (SELinux.isSELinuxEnabled()) {
            stringId = SELinux.isSELinuxEnforced()
                    ? R.string.selinux_status_enforcing
                    : R.string.selinux_status_permissive;
        }
        return mContext.getString(stringId);
    }
}
