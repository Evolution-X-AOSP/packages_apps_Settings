/*
 * Copyright (C) 2021 Yet Another AOSP Project
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

package com.android.settings.sound;

import static android.os.Vibrator.VIBRATION_INTENSITY_OFF;
import static android.os.Vibrator.VIBRATION_INTENSITY_LOW;
import static android.os.Vibrator.VIBRATION_INTENSITY_MEDIUM;
import static android.os.Vibrator.VIBRATION_INTENSITY_HIGH;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.sound.VibrationIntensityController;
import com.android.settings.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

import com.evolution.settings.preference.CustomSeekBarPreference;

/**
 * This class allows choosing a custom vibration intensity
 */
public class VibrationIntensityController extends AbstractPreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "vibration_intensity";
    private static final String KEY_NOTIFICATION = "notification_vibration_intensity";
    private static final String KEY_RING = "ring_vibration_intensity";
    private static final String KEY_HAPTIC = "haptic_feedback_intensity";

    private Vibrator mVibrator;

    private CustomSeekBarPreference mNotificationVib;
    private CustomSeekBarPreference mRingVib;
    private CustomSeekBarPreference mHapticVib;

    public VibrationIntensityController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mVibrator = mContext.getSystemService(Vibrator.class);
        final ContentResolver resolver = mContext.getContentResolver();

        mNotificationVib = screen.findPreference(KEY_NOTIFICATION);
        int def = getDefaultIntensity(0);
        int value = Settings.System.getInt(resolver, KEY_NOTIFICATION, def);
        mNotificationVib.setDefaultValue(def);
        mNotificationVib.setValue(value);
        updateSummary(mNotificationVib, value);
        mNotificationVib.setOnPreferenceChangeListener(this);

        mRingVib = screen.findPreference(KEY_RING);
        def = getDefaultIntensity(1);
        value = Settings.System.getInt(resolver, KEY_RING, def);
        mRingVib.setDefaultValue(def);
        mRingVib.setValue(value);
        updateSummary(mRingVib, value);
        mRingVib.setOnPreferenceChangeListener(this);

        mHapticVib = screen.findPreference(KEY_HAPTIC);
        def = getDefaultIntensity(2);
        value = Settings.System.getInt(resolver, KEY_HAPTIC, def);
        mHapticVib.setDefaultValue(def);
        mHapticVib.setValue(value);
        updateSummary(mHapticVib, value);
        mHapticVib.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ContentResolver resolver = mContext.getContentResolver();
        if (preference == mNotificationVib) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver, KEY_NOTIFICATION, value);
            updateSummary(mNotificationVib, value);
            previewVib(0);
            return true;
        } else if (preference == mRingVib) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver, KEY_RING, value);
            updateSummary(mRingVib, value);
            previewVib(1);
            return true;
        } else if (preference == mHapticVib) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver, KEY_HAPTIC, value);
            updateSummary(mHapticVib, value);
            previewVib(2);
            return true;
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                R.bool.config_vibration_supports_multiple_intensities);
    }

    private int getDefaultIntensity(int stream) {
        switch (stream) {
            default:
            case 0: // notifications
                return mVibrator.getDefaultNotificationVibrationIntensity();
            case 1: // ring
                return mVibrator.getDefaultRingVibrationIntensity();
            case 2: // haptic
                return mVibrator.getDefaultHapticFeedbackIntensity();
        }
    }

    private void updateSummary(CustomSeekBarPreference pref, int setting) {
        switch (setting) {
            case VIBRATION_INTENSITY_OFF:
                pref.setSummary(mContext.getString(
                        R.string.vibration_intensity_off));
                break;
            case VIBRATION_INTENSITY_LOW:
                pref.setSummary(mContext.getString(
                        R.string.vibration_intensity_low));
                break;
            default:
            case VIBRATION_INTENSITY_MEDIUM:
                pref.setSummary(mContext.getString(
                        R.string.vibration_intensity_medium));
                break;
            case VIBRATION_INTENSITY_HIGH:
                pref.setSummary(mContext.getString(
                        R.string.vibration_intensity_high));
                break;
        }
    }

    private void previewVib(int stream) {
        AudioAttributes.Builder builder = new AudioAttributes.Builder();
        final VibrationEffect effect = VibrationEffect.get(VibrationEffect.EFFECT_CLICK);
        switch (stream) {
            default:
            case 0: // notifications
                builder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
                break;
            case 1: // ring
                builder.setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE);
                break;
            case 2: // haptic
                builder.setUsage(AudioAttributes.USAGE_UNKNOWN);
                break;
        }
        mVibrator.vibrate(effect, builder.build());
    }
}
