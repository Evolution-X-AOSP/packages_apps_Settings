/*
 * Copyright (C) 2022 Yet Another AOSP Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import com.evolution.settings.preference.CustomSeekBarPreference;

public class PlaybackControlSwitchPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, OnMainSwitchChangeListener {

    private static final String KEY = "gesture_playback_control_switch";
    private static final String DELAY_KEY = "volume_button_music_control_delay";

    private final Context mContext;
    MainSwitchPreference mSwitch;
    CustomSeekBarPreference mDelayPref;

    public PlaybackControlSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mDelayPref = screen.findPreference(DELAY_KEY);
        mSwitch = screen.findPreference(getPreferenceKey());
        mSwitch.setOnPreferenceClickListener(preference -> {
            final boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.VOLUME_BUTTON_MUSIC_CONTROL, 0) == 1;
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.VOLUME_BUTTON_MUSIC_CONTROL,
                    enabled ? 0 : 1);
            updateDelayEnablement(!enabled);
            return true;
        });
        mSwitch.setTitle(mContext.getString(R.string.gesture_playback_control_primary_switch_title));
        mSwitch.addOnSwitchChangeListener(this);
        updateState(mSwitch);
    }

    public void setChecked(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.updateStatus(isChecked);
        }
        updateDelayEnablement(isChecked);
    }

    @Override
    public void updateState(Preference preference) {
        final boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.VOLUME_BUTTON_MUSIC_CONTROL, 0) == 1;
        setChecked(enabled);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.VOLUME_BUTTON_MUSIC_CONTROL, isChecked ? 1 : 0);
        updateDelayEnablement(isChecked);
    }

    private void updateDelayEnablement(boolean enabled) {
        if (mDelayPref == null) return;
        mDelayPref.setEnabled(enabled);
    }
}
