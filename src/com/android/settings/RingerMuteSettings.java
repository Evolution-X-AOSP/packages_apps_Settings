/*
 * Copyright (C) 2020 Yet Another AOSP Project
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
package com.android.settings;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;
import android.widget.Switch;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

public class RingerMuteSettings extends SettingsPreferenceFragment implements OnMainSwitchChangeListener {

    private static final String KEY_RINGER_MUTE = "ringer_mute_speaker_media";

    private MainSwitchPreference mSwitchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mute_on_silent);
        final ContentResolver resolver = getContentResolver();

        mSwitchBar = (MainSwitchPreference) findPreference(KEY_RINGER_MUTE);
        int enabled = Settings.Global.getInt(resolver, KEY_RINGER_MUTE, 0);
        mSwitchBar.setChecked(enabled != 0);
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.EVOLVER;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        ContentResolver resolver = getActivity().getContentResolver();
        Settings.Global.putInt(resolver, KEY_RINGER_MUTE, isChecked ? 1 : 0);
        mSwitchBar.setChecked(isChecked);
    }
}
