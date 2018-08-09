/*
 * Copyright (C) 2024 Evolution X
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
import android.os.Build;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class EvolutionXVersionPreferenceController extends BasePreferenceController {

    private static final String KEY_EVOLUTION_BUILD_VERSION_PROP = "org.evolution.build_version";
    private static final String KEY_EVOLUTION_DEVICE_PROP = "org.evolution.device";
    private static final String KEY_EVOLUTION_RELEASE_TYPE_PROP = "org.evolution.build_type";

    public EvolutionXVersionPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return shortRomVersion();
    }

    private String shortRomVersion() {
        String romVersion = SystemProperties.get(KEY_EVOLUTION_BUILD_VERSION_PROP,
                this.mContext.getString(R.string.device_info_default));
        String deviceCodename = SystemProperties.get(KEY_EVOLUTION_DEVICE_PROP,
                this.mContext.getString(R.string.device_info_default));
        String romReleasetype = SystemProperties.get(KEY_EVOLUTION_RELEASE_TYPE_PROP,
                this.mContext.getString(R.string.device_info_default));
        String shortVersion = romVersion + " | " + deviceCodename + " | " + romReleasetype;
        return shortVersion;
    }
}
