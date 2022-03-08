/*
 * Copyright (C) 2022 Project Kaleidoscope
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

package ink.kscope.settings.wifi.tether;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import ink.kscope.settings.wifi.tether.preference.WifiTetherAutoOffPreference;

import com.android.settings.core.BasePreferenceController;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private static final int MAX_TIMEOUT_MINUTES = 90;

    private final WifiManager mWifiManager;

    private WifiTetherAutoOffPreference mPreference;

    public WifiTetherAutoOffPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mWifiManager = context.getSystemService(WifiManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setMax(MAX_TIMEOUT_MINUTES);
        updateDisplay();
    }

    private int getAutoOffTimeout() {
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final boolean settingsOn = softApConfiguration.isAutoShutdownEnabled();
        return settingsOn ? (int) (softApConfiguration.getShutdownTimeoutMillis() / 60 / 1000) : 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int timeout = (int) newValue;
        SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        SoftApConfiguration newSoftApConfiguration =
                new SoftApConfiguration.Builder(softApConfiguration)
                        .setAutoShutdownEnabled(timeout > 0)
                        .setShutdownTimeoutMillis((long) timeout * 60 * 1000)
                        .build();
        return mWifiManager.setSoftApConfiguration(newSoftApConfiguration);
    }

    public void updateConfig(SoftApConfiguration.Builder builder) {
        if (builder == null) return;
        final int timeout = getAutoOffTimeout();
        builder.setAutoShutdownEnabled(timeout > 0)
                .setShutdownTimeoutMillis((long) timeout * 60 * 1000);
    }

    public void updateDisplay() {
        mPreference.setValue(getAutoOffTimeout(), false);
    }
}
