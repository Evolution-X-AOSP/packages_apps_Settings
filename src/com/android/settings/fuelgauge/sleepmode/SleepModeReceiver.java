/*
 * Copyright (c) 2021 Havoc-OS
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

package com.android.settings.fuelgauge.sleepmode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.android.internal.util.evolution.EvolutionUtils.SleepModeController;

public class SleepModeReceiver extends BroadcastReceiver {

    private static final String TAG = "SleepModeReceiver";

    public SleepModeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals(SleepModeController.SLEEP_MODE_TURN_OFF)) {
            Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.SLEEP_MODE_ENABLED, 0);
        }
    }
}
