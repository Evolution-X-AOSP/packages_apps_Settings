/*
 * Copyright (C) 2017 The Android Open Source Project
 * Copyright (C) 2023 The LeafOS Project
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

package com.android.settings.deviceinfo.hardwareinfo;

import android.app.ActivityManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;

import java.text.DecimalFormat;

public class TotalRAMPreferenceController extends BasePreferenceController {

    public TotalRAMPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_device_model)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
    ActivityManager actManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    actManager.getMemoryInfo(memInfo);

    long totRam = memInfo.totalMem;
    double gb = (double) totRam / 1073741824.0;

    String aproxRam;
    if (gb > 0 && gb <= 2) {
        aproxRam = "2";
    } else if (gb <= 3) {
        aproxRam = "3";
    } else if (gb <= 4) {
        aproxRam = "4";
    } else if (gb <= 6) {
        aproxRam = "6";
    } else if (gb <= 8) {
        aproxRam = "8";
    } else if (gb <= 12) {
        aproxRam = "12";
    } else {
        aproxRam = "12+";
    }

    String actualRam = aproxRam.concat(" GB");
    return actualRam;
   }
}
