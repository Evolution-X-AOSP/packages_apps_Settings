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

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BluetoothAddressPreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.IpAddressPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.SleeptimePreferenceController;
import com.android.settings.deviceinfo.UptimePreferenceController;
import com.android.settings.deviceinfo.WifiMacAddressPreferenceController;
import com.android.settings.deviceinfo.imei.ImeiInfoPreferenceController;
import com.android.settings.deviceinfo.simstatus.EidStatus;
import com.android.settings.deviceinfo.simstatus.SimEidPreferenceController;
import com.android.settings.deviceinfo.simstatus.SimStatusPreferenceController;
import com.android.settings.deviceinfo.simstatus.SlotSimStatus;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    private static final String KEY_EID_INFO = "eid_info";
    private static final String KEY_MY_DEVICE_INFO_HEADER = "my_device_info_header";

    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d(LOG_TAG, "Received ACTION_SIM_STATE_CHANGED: " + state);
                updatePreferenceStates();
            }
        }
    };

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DeviceNamePreferenceController.class).setHost(this /* parent */);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
    }

    @Override
    public void onStart() {
        super.onStart();
        initHeader();
    }

    @Override
    public void onPause() {
        super.onPause();
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(mSimStateReceiver);
        } else {
            Log.i(LOG_TAG, "context already null, not unregistering SimStateReceiver");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context != null) {
            context.registerReceiver(mSimStateReceiver,
                    new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED));
        } else {
            Log.i(LOG_TAG, "context is null, not registering SimStateReceiver");
        }
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this /* fragment */, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, MyDeviceInfoFragment fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        final ExecutorService executor = (fragment == null) ? null :
                Executors.newSingleThreadExecutor();
        androidx.lifecycle.Lifecycle lifecycleObject = (fragment == null) ? null :
                fragment.getLifecycle();
        final SlotSimStatus slotSimStatus = new SlotSimStatus(context, executor, lifecycleObject);

        controllers.add(new IpAddressPreferenceController(context, lifecycle));
        controllers.add(new WifiMacAddressPreferenceController(context, lifecycle));
        controllers.add(new BluetoothAddressPreferenceController(context, lifecycle));
        controllers.add(new RegulatoryInfoPreferenceController(context));
        controllers.add(new SafetyInfoPreferenceController(context));
        controllers.add(new ManualPreferenceController(context));
        controllers.add(new FeedbackPreferenceController(fragment, context));
        controllers.add(new FccEquipmentIdPreferenceController(context));
        controllers.add(new SleeptimePreferenceController(context, lifecycle));
        controllers.add(new UptimePreferenceController(context, lifecycle));

        Consumer<String> imeiInfoList = imeiKey -> {
            ImeiInfoPreferenceController imeiRecord =
                    new ImeiInfoPreferenceController(context, imeiKey);
            imeiRecord.init(fragment, slotSimStatus);
            controllers.add(imeiRecord);
        };

        if (fragment != null) {
            imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY);
        }

        for (int slotIndex = 0; slotIndex < slotSimStatus.size(); slotIndex ++) {
            SimStatusPreferenceController slotRecord =
                    new SimStatusPreferenceController(context,
                    slotSimStatus.getPreferenceKey(slotIndex));
            slotRecord.init(fragment, slotSimStatus);
            controllers.add(slotRecord);

            if (fragment != null) {
                imeiInfoList.accept(ImeiInfoPreferenceController.DEFAULT_KEY + (1 + slotIndex));
            }
        }

        EidStatus eidStatus = new EidStatus(slotSimStatus, context, executor);
        SimEidPreferenceController simEid = new SimEidPreferenceController(context, KEY_EID_INFO);
        simEid.init(slotSimStatus, eidStatus);
        controllers.add(simEid);

        if (executor != null) {
            executor.shutdown();
        }
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initHeader() {
        // TODO: Migrate into its own controller.
        final LayoutPreference headerPreference =
                getPreferenceScreen().findPreference(KEY_MY_DEVICE_INFO_HEADER);
        final boolean shouldDisplayHeader = getContext().getResources().getBoolean(
                R.bool.config_show_device_header_in_device_info);
        headerPreference.setVisible(shouldDisplayHeader);
        if (!shouldDisplayHeader) {
            return;
        }
        final View headerView = headerPreference.findViewById(R.id.entity_header);
        final Activity context = getActivity();
        final Bundle bundle = getArguments();
        final EntityHeaderController controller = EntityHeaderController
                .newInstance(context, this, headerView)
                .setRecyclerView(getListView(), getSettingsLifecycle())
                .setButtonActions(EntityHeaderController.ActionType.ACTION_NONE,
                        EntityHeaderController.ActionType.ACTION_NONE);

        // TODO: There may be an avatar setting action we can use here.
        final int iconId = bundle.getInt("icon_id", 0);
        if (iconId == 0) {
            final UserManager userManager = (UserManager) getActivity().getSystemService(
                    Context.USER_SERVICE);
            final UserInfo info = Utils.getExistingUser(userManager,
                    android.os.Process.myUserHandle());
            controller.setLabel(info.name);
            controller.setIcon(
                    com.android.settingslib.Utils.getUserIcon(getActivity(), userManager, info));
        }

        controller.done(context, true /* rebindActions */);
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        DeviceNameWarningDialog.show(this);
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        final DeviceNamePreferenceController controller = use(DeviceNamePreferenceController.class);
        controller.updateDeviceName(confirm);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* fragment */,
                            null /* lifecycle */);
                }
            };
}
