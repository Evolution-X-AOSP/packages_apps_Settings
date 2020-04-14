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

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.network.telephony.MobileNetworkUtils;
import java.util.Arrays;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OpenNetworkSelectPagePreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final String OPERATOR_NAME = "T-mobile";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private ServiceState mServiceState;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private PersistableBundle mCarrierConfig;
    private OpenNetworkSelectPagePreferenceController mController;
    private Preference mPreference;
    private Context mContext;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(
                mCarrierConfigManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getServiceState()).thenReturn(mServiceState);

        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);

        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(SUB_ID);
        when(mSubscriptionInfo.getCarrierName()).thenReturn(OPERATOR_NAME);

        when(mSubscriptionManager.getActiveSubscriptionInfoList(eq(true))).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionManager.getAccessibleSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));

        when(mTelephonyManager.getNetworkOperatorName()).thenReturn(OPERATOR_NAME);

        mPreference = new Preference(mContext);
        mController = spy(new OpenNetworkSelectPagePreferenceController(mContext,
                "open_network_select"));
        mController.mPreference = mPreference;
        mController.init(SUB_ID);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void updateState_modeAuto_disabled() {
        when(mTelephonyManager.getNetworkSelectionMode()).thenReturn(
                TelephonyManager.NETWORK_SELECTION_MODE_AUTO);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void getSummary_inService_returnOperatorName() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        assertThat(mController.getSummary()).isEqualTo(OPERATOR_NAME);
    }

    @Test
    public void getSummary_notInService_returnDisconnect() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.network_disconnected));
    }

    @Test
    public void onCallStateChanged_callIdle_enabled() {
        // Go through lifecycle to set up listener.
        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

        // Trigger listener update.
        when(mTelephonyManager.getNetworkSelectionMode()).thenReturn(
                TelephonyManager.NETWORK_SELECTION_MODE_MANUAL);
        mController.mPhoneStateListener.onCallStateChanged(TelephonyManager.CALL_STATE_IDLE, "");

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void onCallStateChanged_notCallIdle_disabled() {
        // Go through lifecycle to set up listener.
        mLifecycle.handleLifecycleEvent(ON_START);
        verify(mController).onStart();
        verify(mTelephonyManager).listen(mController.mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

        // Trigger listener update.
        when(mTelephonyManager.getNetworkSelectionMode()).thenReturn(
                TelephonyManager.NETWORK_SELECTION_MODE_MANUAL);
        mController.mPhoneStateListener.onCallStateChanged(TelephonyManager.CALL_STATE_OFFHOOK, "");

        assertThat(mPreference.isEnabled()).isFalse();
    }
}
