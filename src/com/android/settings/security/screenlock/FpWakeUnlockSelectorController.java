/*
 * Copyright (C) 2021 ArrowOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.security.screenlock;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for the fp wake unlock option dropdown
 */
public class FpWakeUnlockSelectorController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = FpWakeUnlockSelectorController.class.getSimpleName();

    private FingerprintManager mFingerprintManager;
    private ListPreference mPreference;
    private int mCurrentMode;
    private boolean mAvailable;

    public FpWakeUnlockSelectorController(Context context, String key) {
        super(context, key);
        mFingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        mAvailable = (mFingerprintManager.isHardwareDetected());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return mAvailable ? BasePreferenceController.AVAILABLE : BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final void updateState(Preference preference) {
        mCurrentMode = getCurrentMode();
        mPreference.setValueIndex(mCurrentMode);
    }

    private int getCurrentMode() {
        int resId;
        int fpWakeUnlock = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.FP_WAKE_UNLOCK, 0, UserHandle.USER_CURRENT);
        switch (fpWakeUnlock) {
            case 0:
                resId = R.string.fp_wake_unlock_touch;
                break;
            case 1:
                resId = R.string.fp_wake_unlock_press;
                break;
            default:
                resId = R.string.fp_wake_unlock_touch;
        }
        return mPreference.findIndexOfValue(mContext.getString(resId));
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        final int newMode = mPreference.findIndexOfValue((String) newValue);
        if (newMode == mCurrentMode) {
            return false;
        }
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.FP_WAKE_UNLOCK, newMode);
        mCurrentMode = newMode;
        return true;
    }
}
