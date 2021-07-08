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
import android.hardware.display.ColorDisplayManager;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settings.widget.SeekBarPreference;

public class ColorBalancePreferenceController extends SliderPreferenceController {

    private final ColorDisplayManager mColorDisplayManager;
    private final int mChannel;

    public ColorBalancePreferenceController(Context context, String key) {
        super(context, key);
        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mChannel = keyToChannel(key);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isColorTransformAccelerated(mContext) ?
                AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), channelToKey(mChannel));
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final SeekBarPreference preference = screen.findPreference(getPreferenceKey());
        preference.setContinuousUpdates(true);
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setProgress(getSliderPosition());
    }

    @Override
    public int getSliderPosition() {
        return mColorDisplayManager.getColorBalanceChannel(mChannel);
    }

    @Override
    public boolean setSliderPosition(int position) {
        return mColorDisplayManager.setColorBalanceChannel(mChannel, position);
    }

    @Override
    public int getMax() {
        // 8-bpc linear sRGB
        return 255;
    }

    @Override
    public int getMin() {
        // Users shouldn't be able to (accidentally) make their display black. Limit it to 10%.
        return 25;
    }

    private static int keyToChannel(String key) {
        switch (key) {
            case "color_balance_red":
                return 0;
            case "color_balance_green":
                return 1;
            case "color_balance_blue":
                return 2;
            default:
                throw new IllegalArgumentException("Unknown key: " + key);
        }
    }

    /* package-private */ static String channelToKey(int channel) {
        switch (channel) {
            case 0:
                return "color_balance_red";
            case 1:
                return "color_balance_green";
            case 2:
                return "color_balance_blue";
            default:
                throw new IllegalArgumentException("Unknown channel: " + channel);
        }
    }
}
