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

package com.android.settings.development;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.StrictMode;
import android.provider.Settings;

import com.android.settings.core.TogglePreferenceController;

public class CleartextNetworkPolicyPreferenceController extends TogglePreferenceController {

    public CleartextNetworkPolicyPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        ConnectivityManager connectivityManager = ConnectivityManager.from(mContext);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            if (linkProperties != null) {
                return isChecked() || linkProperties.isPrivateDnsActive() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
            }
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CLEARTEXT_NETWORK_POLICY, StrictMode.NETWORK_POLICY_ACCEPT) != StrictMode.NETWORK_POLICY_ACCEPT;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CLEARTEXT_NETWORK_POLICY, isChecked ? StrictMode.NETWORK_POLICY_REJECT : StrictMode.NETWORK_POLICY_ACCEPT);
    }
}