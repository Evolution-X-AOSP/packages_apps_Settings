/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.hardware.face.FaceManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class FaceSettingsLockscreenBypassPreferenceController
        extends FaceSettingsPreferenceController 
        implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    protected FaceManager mFaceManager;
    private UserManager mUserManager;

    private final SecureSettingObserver mSecureSettingsContentObserver;
    private Preference mPreference;

    private static final String PREF_KEY_FACEUNLOCK_METHOD = "face_unlock_method";

    public FaceSettingsLockscreenBypassPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE)) {
            mFaceManager = context.getSystemService(FaceManager.class);
        }

        mUserManager = context.getSystemService(UserManager.class);
        mSecureSettingsContentObserver = new SecureSettingObserver(PREF_KEY_FACEUNLOCK_METHOD) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mPreference);
            }
        };
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                mSecureSettingsContentObserver.uri,
                false /* notifyForDescendants */,
                mSecureSettingsContentObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSecureSettingsContentObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            return false;
        } else if (getRestrictingAdmin() != null) {
            return false;
        }
        int defaultValue = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthDismissesKeyguard) ? 1 : 0;
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, defaultValue, getUserId()) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, isChecked ? 1 : 0);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        EnforcedAdmin admin;
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if ((admin = getRestrictingAdmin()) != null) {
            ((RestrictedSwitchPreference) preference).setDisabledByAdmin(admin);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(),
                    PREF_KEY_FACEUNLOCK_METHOD, 0) == 0;
            preference.setEnabled(enabled);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mUserManager.isManagedProfile(UserHandle.myUserId())) {
            return UNSUPPORTED_ON_DEVICE;
        }

        boolean faceAuthOnlyOnSecurityView  = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthOnlyOnSecurityView);

        if (mFaceManager != null && mFaceManager.isHardwareDetected() &&
                !faceAuthOnlyOnSecurityView) {
            return mFaceManager.hasEnrolledTemplates(getUserId())
                    ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    private static class SecureSettingObserver extends ContentObserver {

        public final Uri uri;

        public SecureSettingObserver(String settingKey) {
            super(new Handler(Looper.getMainLooper()));
            uri = Settings.Secure.getUriFor(settingKey);
        }
    }
}