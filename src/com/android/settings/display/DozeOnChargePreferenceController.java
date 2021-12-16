/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class DozeOnChargePreferenceController extends TogglePreferenceController {

    private AmbientDisplayConfiguration mConfig;

    public DozeOnChargePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(getConfig()) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.DOZE_ON_CHARGE, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.DOZE_ON_CHARGE, isChecked ? 1 : 0);
        return true;
    }

    public DozeOnChargePreferenceController setConfig(
            AmbientDisplayConfiguration config) {
        mConfig = config;
        return this;
    }

    private static boolean isAvailable(AmbientDisplayConfiguration config) {
        return config.alwaysOnAvailableForUser(UserHandle.myUserId());
    }

    private AmbientDisplayConfiguration getConfig() {
        if (mConfig == null) {
            mConfig = new AmbientDisplayConfiguration(mContext);
        }
        return mConfig;
    }
}
