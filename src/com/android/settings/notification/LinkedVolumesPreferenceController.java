/*
 * Copyright (C) 2018 The LineageOS Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.notification.VolumeSeekBarPreference;
import com.android.settings.R;

import com.evolution.settings.preference.SecureSettingSwitchPreference;

public class LinkedVolumesPreferenceController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_VOLUME_LINK_NOTIFICATION = "volume_link_notification";
    private static final String KEY_VOLUME_RING = "ring_volume";
    private static final String KEY_VOLUME_NOTIFICATION = "notification_volume";

    private Context mContext;
    private SecureSettingSwitchPreference mLinkedVolume;
    private VolumeSeekBarPreference mRingVolume;
    private VolumeSeekBarPreference mNotificationVolume;

    public LinkedVolumesPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VOLUME_LINK_NOTIFICATION;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            return;
        }
        mRingVolume = (VolumeSeekBarPreference) screen.findPreference(KEY_VOLUME_RING);
        mNotificationVolume = (VolumeSeekBarPreference) screen.findPreference(KEY_VOLUME_NOTIFICATION);
        mLinkedVolume = (SecureSettingSwitchPreference) screen.findPreference(KEY_VOLUME_LINK_NOTIFICATION);
        mLinkedVolume.setOnPreferenceChangeListener(this);
        boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                KEY_VOLUME_LINK_NOTIFICATION, 1) == 1;
        mLinkedVolume.setChecked(enabled ? true : false);
        updateNotificationVis(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                KEY_VOLUME_LINK_NOTIFICATION, enabled ? 1 : 0);
        updateNotificationVis(enabled);
        return true;
    }

    private void updateNotificationVis(boolean linked) {
        if (mRingVolume == null || mNotificationVolume == null) {
            return;
        }
        if (linked) {
            mRingVolume.setTitle(mContext.getResources().getString(
                    R.string.ring_volume_option_title));
            mNotificationVolume.setVisible(false);
        } else {
            mRingVolume.setTitle(mContext.getResources().getString(
                    R.string.ring_volume_unlinked_option_title));
            mNotificationVolume.setVisible(true);
        }
    }
}
