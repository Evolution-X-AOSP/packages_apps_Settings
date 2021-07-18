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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class RefreshRateSettingsFragment extends DashboardFragment {

    private static final String TAG = "RefreshRateSettingsFragment";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.refresh_rate_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final MinRefreshRatePreferenceController minRefreshRatePreferenceController =
                new MinRefreshRatePreferenceController(context);
        final PeakRefreshRatePreferenceController peakRefreshRatePreferenceController =
                new PeakRefreshRatePreferenceController(context);
        final PreferredRefreshRatePreferenceController preferredRefreshRatePreferenceController =
                new PreferredRefreshRatePreferenceController(context);

        controllers.add(minRefreshRatePreferenceController);
        controllers.add(peakRefreshRatePreferenceController);
        controllers.add(preferredRefreshRatePreferenceController);

        return controllers;
    }

}
