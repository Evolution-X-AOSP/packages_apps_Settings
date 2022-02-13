/*
 * Copyright (C) 2020-2021 Paranoid Android
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.RadioButtonPreference;

public class AdaptivePlaybackSoundPreferenceController extends BasePreferenceController
        implements RadioButtonPreference.OnClickListener, LifecycleObserver, OnStart, OnStop {

    private static final String KEY_NO_TIMEOUT = "adaptive_playback_timeout_none";
    private static final String KEY_30_SECS = "adaptive_playback_timeout_30_secs";
    private static final String KEY_1_MIN = "adaptive_playback_timeout_1_min";
    private static final String KEY_2_MIN = "adaptive_playback_timeout_2_min";
    private static final String KEY_5_MIN = "adaptive_playback_timeout_5_min";
    private static final String KEY_10_MIN = "adaptive_playback_timeout_10_min";

    static final int ADAPTIVE_PLAYBACK_TIMEOUT_NONE = 0;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS = 30000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN = 60000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN = 120000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN = 300000;
    static final int ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN = 600000;

    private boolean mAdaptivePlaybackEnabled;
    private int mAdaptivePlaybackTimeout;

    private PreferenceCategory mPreferenceCategory;
    private RadioButtonPreference mTimeoutNonePref;
    private RadioButtonPreference mTimeout30SecPref;
    private RadioButtonPreference mTimeout1MinPref;
    private RadioButtonPreference mTimeout2MinPref;
    private RadioButtonPreference mTimeout5MinPref;
    private RadioButtonPreference mTimeout10MinPref;

    private final SettingObserver mSettingObserver;

    public AdaptivePlaybackSoundPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mSettingObserver = new SettingObserver(new Handler(Looper.getMainLooper()));
        mAdaptivePlaybackEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) != 0;
        mAdaptivePlaybackTimeout = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT, ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                UserHandle.USER_CURRENT);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mTimeoutNonePref = makeRadioPreference(KEY_NO_TIMEOUT,
                R.string.adaptive_playback_timeout_none);
        mTimeout30SecPref = makeRadioPreference(KEY_30_SECS,
                R.string.adaptive_playback_timeout_30_secs);
        mTimeout1MinPref = makeRadioPreference(KEY_1_MIN, R.string.adaptive_playback_timeout_1_min);
        mTimeout2MinPref = makeRadioPreference(KEY_2_MIN, R.string.adaptive_playback_timeout_2_min);
        mTimeout5MinPref = makeRadioPreference(KEY_5_MIN, R.string.adaptive_playback_timeout_5_min);
        mTimeout10MinPref = makeRadioPreference(KEY_10_MIN,
                R.string.adaptive_playback_timeout_10_min);
        updateState(null);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
        int adaptivePlaybackTimeout = keyToSetting(preference.getKey());
        if (adaptivePlaybackTimeout != Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT, ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                UserHandle.USER_CURRENT)) {
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT, adaptivePlaybackTimeout,
                    UserHandle.USER_CURRENT);
        }
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isTimeoutNone = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_NONE;
        final boolean isTimeout30Sec = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS;
        final boolean isTimeout1Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN;
        final boolean isTimeout2Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN;
        final boolean isTimeout5Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN;
        final boolean isTimeout10Min = mAdaptivePlaybackEnabled
                && mAdaptivePlaybackTimeout == ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN;
        if (mTimeoutNonePref != null && mTimeoutNonePref.isChecked() != isTimeoutNone) {
            mTimeoutNonePref.setChecked(isTimeoutNone);
        }
        if (mTimeout30SecPref != null && mTimeout30SecPref.isChecked() != isTimeout30Sec) {
            mTimeout30SecPref.setChecked(isTimeout30Sec);
        }
        if (mTimeout1MinPref != null && mTimeout1MinPref.isChecked() != isTimeout1Min) {
            mTimeout1MinPref.setChecked(isTimeout1Min);
        }
        if (mTimeout2MinPref != null && mTimeout2MinPref.isChecked() != isTimeout2Min) {
            mTimeout2MinPref.setChecked(isTimeout2Min);
        }
        if (mTimeout5MinPref != null && mTimeout5MinPref.isChecked() != isTimeout5Min) {
            mTimeout5MinPref.setChecked(isTimeout5Min);
        }
        if (mTimeout10MinPref != null && mTimeout10MinPref.isChecked() != isTimeout10Min) {
            mTimeout10MinPref.setChecked(isTimeout10Min);
        }

        if (mAdaptivePlaybackEnabled) {
            mPreferenceCategory.setEnabled(true);
            mTimeoutNonePref.setEnabled(true);
            mTimeout30SecPref.setEnabled(true);
            mTimeout1MinPref.setEnabled(true);
            mTimeout2MinPref.setEnabled(true);
            mTimeout5MinPref.setEnabled(true);
            mTimeout10MinPref.setEnabled(true);
        } else {
            mPreferenceCategory.setEnabled(false);
            mTimeoutNonePref.setEnabled(false);
            mTimeout30SecPref.setEnabled(false);
            mTimeout1MinPref.setEnabled(false);
            mTimeout2MinPref.setEnabled(false);
            mTimeout5MinPref.setEnabled(false);
            mTimeout10MinPref.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        mSettingObserver.observe();
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
    }

    private static int keyToSetting(String key) {
        switch (key) {
            case KEY_NO_TIMEOUT:
                return ADAPTIVE_PLAYBACK_TIMEOUT_NONE;
            case KEY_1_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN;
            case KEY_2_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN;
            case KEY_5_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN;
            case KEY_10_MIN:
                return ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN;
            default:
                return ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS;
        }
    }

    private RadioButtonPreference makeRadioPreference(String key, int titleId) {
        RadioButtonPreference pref = new RadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(titleId);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri ADAPTIVE_PLAYBACK = Settings.System.getUriFor(
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED);
        private final Uri ADAPTIVE_PLAYBACK_TIMEOUT = Settings.System.getUriFor(
                Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT);

        public SettingObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(ADAPTIVE_PLAYBACK, false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(ADAPTIVE_PLAYBACK_TIMEOUT, false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (ADAPTIVE_PLAYBACK.equals(uri) || ADAPTIVE_PLAYBACK_TIMEOUT.equals(uri)) {
                mAdaptivePlaybackEnabled = Settings.System.getIntForUser(
                        mContext.getContentResolver(), Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0,
                        UserHandle.USER_CURRENT) != 0;
                mAdaptivePlaybackTimeout = Settings.System.getIntForUser(
                        mContext.getContentResolver(), Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT,
                        ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS, UserHandle.USER_CURRENT);
                updateState(null);
            }
        }
    }
}
