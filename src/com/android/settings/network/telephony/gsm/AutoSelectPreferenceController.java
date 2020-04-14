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

package com.android.settings.network.telephony.gsm;

import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.NetworkSelectSettings;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Preference controller for "Auto Select Network"
 */
public class AutoSelectPreferenceController extends TelephonyTogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private static final long MINIMUM_DIALOG_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final Handler mUiHandler;
    private TelephonyManager mTelephonyManager;
    private boolean mOnlyAutoSelectInHome;
    private boolean mIsCallStateIdle = true;
    private List<OnNetworkSelectModeListener> mListeners;
    @VisibleForTesting
    ProgressDialog mProgressDialog;
    @VisibleForTesting
    SwitchPreference mSwitchPreference;
    @VisibleForTesting
    PhoneStateListener mPhoneStateListener;

    public AutoSelectPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListeners = new ArrayList<>();
        mUiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, subId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                mIsCallStateIdle = state == TelephonyManager.CALL_STATE_IDLE;
                updateState(mSwitchPreference);
            }
        };
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onStop() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean isChecked() {
        return mTelephonyManager.getNetworkSelectionMode()
                == TelephonyManager.NETWORK_SELECTION_MODE_AUTO;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(null);
        if (mTelephonyManager.getServiceState().getRoaming()) {
            preference.setEnabled(mIsCallStateIdle);
        } else {
            preference.setEnabled(!mOnlyAutoSelectInHome && mIsCallStateIdle);
            if (mOnlyAutoSelectInHome) {
                preference.setSummary(mContext.getString(
                        R.string.manual_mode_disallowed_summary,
                        mTelephonyManager.getSimOperatorName()));
            }
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            final long startMillis = SystemClock.elapsedRealtime();
            showAutoSelectProgressBar();
            mSwitchPreference.setEnabled(false);
            ThreadUtils.postOnBackgroundThread(() -> {
                // set network selection mode in background
                mTelephonyManager.setNetworkSelectionModeAutomatic();
                final int mode = mTelephonyManager.getNetworkSelectionMode();

                //Update UI in UI thread
                final long durationMillis = SystemClock.elapsedRealtime() - startMillis;
                mUiHandler.postDelayed(() -> {
                            mSwitchPreference.setEnabled(true);
                            mSwitchPreference.setChecked(
                                    mode == TelephonyManager.NETWORK_SELECTION_MODE_AUTO);
                            for (OnNetworkSelectModeListener lsn : mListeners) {
                                lsn.onNetworkSelectModeChanged();
                            }
                            dismissProgressBar();
                        },
                        Math.max(MINIMUM_DIALOG_TIME_MILLIS - durationMillis, 0));
            });
            return false;
        } else {
            final Bundle bundle = new Bundle();
            bundle.putInt(Settings.EXTRA_SUB_ID, mSubId);
            new SubSettingLauncher(mContext)
                    .setDestination(NetworkSelectSettings.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.MOBILE_NETWORK_SELECT)
                    .setTitleRes(R.string.choose_network_title)
                    .setArguments(bundle)
                    .launch();
            return false;
        }
    }

    public AutoSelectPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
        final PersistableBundle carrierConfig = mContext.getSystemService(
                CarrierConfigManager.class).getConfigForSubId(mSubId);
        mOnlyAutoSelectInHome = carrierConfig != null
                ? carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL)
                : false;

        return this;
    }

    public AutoSelectPreferenceController addListener(OnNetworkSelectModeListener lsn) {
        mListeners.add(lsn);

        return this;
    }

    private void showAutoSelectProgressBar() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setMessage(
                    mContext.getResources().getString(R.string.register_automatically));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    private void dismissProgressBar() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Callback when network select mode is changed
     *
     * @see TelephonyManager#getNetworkSelectionMode()
     */
    public interface OnNetworkSelectModeListener {
        void onNetworkSelectModeChanged();
    }
}
