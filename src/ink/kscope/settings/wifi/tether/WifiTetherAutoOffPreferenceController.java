/*
 * Copyright (C) 2022 Project Kaleidoscope
 * Copyright (C) 2022 FlamingoOS Project
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

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class WifiTetherAutoOffPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final WifiManager mWifiManager;

    private ListPreference mPreference;

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
        updateDisplay();
    }

    private long getAutoOffTimeout() {
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final boolean settingsOn = softApConfiguration.isAutoShutdownEnabled();
        return settingsOn ? softApConfiguration.getShutdownTimeoutMillis() / 1000 : 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final long timeout;
        try {
            timeout = Long.parseLong((String) newValue);
        } catch (NumberFormatException e) {
            return false;
        }
        final SoftApConfiguration softApConfiguration = mWifiManager.getSoftApConfiguration();
        final SoftApConfiguration.Builder builder =
                new SoftApConfiguration.Builder(softApConfiguration);
        setShutdownTimeout(builder, timeout);
        return mWifiManager.setSoftApConfiguration(builder.build());
    }

    public void updateConfig(SoftApConfiguration.Builder builder) {
        if (builder == null) return;
        final long timeout = getAutoOffTimeout();
        setShutdownTimeout(builder, timeout);
    }

    private void setShutdownTimeout(SoftApConfiguration.Builder builder, long timeout) {
        builder.setAutoShutdownEnabled(timeout > 0);
        if (timeout > 0) {
            builder.setShutdownTimeoutMillis(timeout * 1000);
        }
    }

    public void updateDisplay() {
        if (mPreference != null) {
            mPreference.setValue(String.valueOf(getAutoOffTimeout()));
        }
    }
}
