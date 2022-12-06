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

import static android.provider.Settings.System.ADAPTIVE_PLAYBACK_ENABLED;
import static android.provider.Settings.System.ADAPTIVE_PLAYBACK_TIMEOUT;

import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN;
import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN;
import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN;
import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS;
import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN;
import static com.android.settings.sound.AdaptivePlaybackSoundPreferenceController.ADAPTIVE_PLAYBACK_TIMEOUT_NONE;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class AdaptivePlaybackParentPreferenceController extends BasePreferenceController {

    public AdaptivePlaybackParentPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        boolean enabled = Settings.System.getIntForUser(
                mContext.getContentResolver(), ADAPTIVE_PLAYBACK_ENABLED, 0,
                UserHandle.USER_CURRENT) != 0;
        int timeout = Settings.System.getIntForUser(
                mContext.getContentResolver(), ADAPTIVE_PLAYBACK_TIMEOUT,
                ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS,
                UserHandle.USER_CURRENT);
        int summary = R.string.adaptive_playback_disabled_summary;
        if (!enabled) {
            return mContext.getText(summary);
        }
        switch (timeout) {
            case ADAPTIVE_PLAYBACK_TIMEOUT_NONE:
                summary = R.string.adaptive_playback_timeout_none_summary;
                break;
            case ADAPTIVE_PLAYBACK_TIMEOUT_30_SECS:
                summary = R.string.adaptive_playback_timeout_30_secs_summary;
                break;
            case ADAPTIVE_PLAYBACK_TIMEOUT_1_MIN:
                summary = R.string.adaptive_playback_timeout_1_min_summary;
                break;
            case ADAPTIVE_PLAYBACK_TIMEOUT_2_MIN:
                summary = R.string.adaptive_playback_timeout_2_min_summary;
                break;
            case ADAPTIVE_PLAYBACK_TIMEOUT_5_MIN:
                summary = R.string.adaptive_playback_timeout_5_min_summary;
                break;
            case ADAPTIVE_PLAYBACK_TIMEOUT_10_MIN:
                summary = R.string.adaptive_playback_timeout_10_min_summary;
                break;
        }
        return mContext.getText(summary);
    }
}
