/*
 * Copyright (C) 2021 Paranoid Android
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
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class RefreshRatePreferenceController extends BasePreferenceController {

    public RefreshRatePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_refresh_rate_controls)
                   ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        final float defaultRefreshRate = (float) mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_defaultRefreshRate);
        final float preferredRefreshRate = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PREFERRED_REFRESH_RATE, defaultRefreshRate);

        return String.format("%.0fHz", preferredRefreshRate);
    }

}
