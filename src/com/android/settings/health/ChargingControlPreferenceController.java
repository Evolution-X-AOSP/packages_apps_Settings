/*
 * Copyright (C) 2022 The PixelExperience Project
 * Copyright (C) 2023 The LibreMobileOS Foundation
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

package com.android.settings.health;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.R;

public class ChargingControlPreferenceController extends BasePreferenceController {

    public static final String KEY = "charging_control";

    private Context mContext;

    public ChargingControlPreferenceController(Context context, String key) {
        super(context, key);

        mContext = context;
    }

    public ChargingControlPreferenceController(Context context) {
        this(context, KEY);

        mContext = context;
    }

    private boolean isNegated(String key) {
        return key != null && key.startsWith("!");
    }

    @Override
    public int getAvailabilityStatus() {
        String rService =  "lineagehealth";
        boolean negated = isNegated(rService);
        if (negated) {
           rService = rService.substring(1);
        }
        IBinder value = ServiceManager.getService(rService);
        boolean available = value != null;
        if (available == negated) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

}
