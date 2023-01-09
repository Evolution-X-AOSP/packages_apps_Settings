/*
 * Copyright (C) 2023 Yet Another AOSP Project
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

package com.android.settings.notification.app;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.evolution.EvolutionUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedSwitchPreference;

import com.evolution.settings.preference.CustomSeekBarPreference;

public class TorchBlinkPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY = "default_notification_torch";
    private static final String KEY1 = "default_notification_torch1";
    private static final String KEY2 = "default_notification_torch2";
    private static final String CATEGORY_KEY = "default_notification_torch_seek_bars";

    private final boolean mHasTorch;

    private RestrictedSwitchPreference mPreference;
    private CustomSeekBarPreference mSeekBar1;
    private CustomSeekBarPreference mSeekBar2;
    private PreferenceCategory mBarsCategory;

    public TorchBlinkPreferenceController(Context context, NotificationBackend backend) {
        super(context, backend);
        mHasTorch = EvolutionUtils.deviceHasFlashlight(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
        mPreference.setOnPreferenceChangeListener(this);
        mSeekBar1 = screen.findPreference(KEY1);
        mSeekBar1.setOnPreferenceChangeListener(this);
        mSeekBar2 = screen.findPreference(KEY2);
        mSeekBar2.setOnPreferenceChangeListener(this);
        mBarsCategory = screen.findPreference(CATEGORY_KEY);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable() || mChannel == null) {
            if (mBarsCategory != null)
                mBarsCategory.setVisible(false);
            return false;
        }
        if (mBarsCategory != null && !mHasTorch)
            mBarsCategory.setVisible(false);
        return mHasTorch;
    }

    @Override
    boolean isIncludedInFilter() {
        return mHasTorch;
    }

    public void updateState(Preference preference) {
        if (mChannel == null) return;
        if (mPreference == null) mPreference = (RestrictedSwitchPreference) preference;
        mPreference.setDisabledByAdmin(mAdmin);
        mPreference.setEnabled(!mPreference.isDisabledByAdmin());
        final boolean isChecked = mChannel.shouldBlinkTorch() &&
                mChannel.getTorchBlinkPattern() != null;
        mPreference.setChecked(isChecked);
        mBarsCategory.setVisible(isChecked);
        if (isChecked) getValues();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int pref = 0;
        if (preference == mPreference) {
            Boolean value = (Boolean) newValue;
            mBarsCategory.setVisible(value);
            if (value) getValues();
            else setValues(false, 0, 0);
            return true;
        } else if (preference == mSeekBar1) {
            pref = 1;
        } else if (preference == mSeekBar2) {
            pref = 2;
        }
        return setValues(mPreference.isChecked(), pref,
                ((Integer) newValue));
    }

    private boolean setValues(boolean enabled, int pref, int val) {
        mBarsCategory.setVisible(enabled);
        mChannel.enableTorchBlink(enabled);
        if (enabled) {
            final int val1 = pref == 1 ? val : mSeekBar1.getValue();
            final int val2 = pref == 2 ? val : mSeekBar2.getValue();
            if (val1 < 1 || val2 < 1)
                return false;
            final int[] pattern = {val1, val2};
            mChannel.setCustomTorchBlinkPattern(pattern);
        } else {
            mChannel.setCustomTorchBlinkPattern(null);
        }
        saveChannel();
        return true;
    }

    private void getValues() {
        int[] pattern = mChannel.getTorchBlinkPattern();
        if (pattern == null) pattern = new int[] {2, 2};
        mSeekBar1.setValue(pattern[0]);
        mSeekBar2.setValue(pattern[1]);
    }
}
