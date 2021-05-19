/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Global;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.android.internal.util.evolution.EvolutionUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.widget.LayoutPreference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was consumed
 * since the last time it was unplugged.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PowerUsageSummary extends PowerUsageBase implements OnLongClickListener,
        BatteryTipPreferenceController.BatteryTipListener {

    static final String TAG = "PowerUsageSummary";

    private static final String KEY_BATTERY_HEADER = "battery_header";

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";
    private static final String KEY_BATTERY_TEMP = "battery_temp";
    private static final String KEY_SMART_CHARGING_CATEGORY = "smart_charging_category";
    private static final String KEY_BATTERY_INFO_CATEGORY = "battery_info_category";
    private static final String KEY_CURRENT_BATTERY_CAPACITY = "current_battery_capacity";
    private static final String KEY_DESIGNED_BATTERY_CAPACITY = "designed_battery_capacity";
    private static final String KEY_BATTERY_CHARGE_CYCLES = "battery_charge_cycles";

    private String mBatDesCap;
    private String mBatCurCap;
    private String mBatChgCyc;

    private PreferenceCategory mSmartChargingCat;
    private PreferenceCategory mBatteryInfoCat;
    @VisibleForTesting
    static final int BATTERY_INFO_LOADER = 1;
    @VisibleForTesting
    static final int BATTERY_TIP_LOADER = 2;
    @VisibleForTesting
    static final int MENU_ADVANCED_BATTERY = Menu.FIRST + 1;
    static final int MENU_STATS_RESET = Menu.FIRST + 2;
    public static final int DEBUG_INFO_LOADER = 3;

    @VisibleForTesting
    PowerGaugePreference mScreenUsagePref;
    @VisibleForTesting
    PowerGaugePreference mCurrentBatteryCapacity;
    @VisibleForTesting
    PowerGaugePreference mDesignedBatteryCapacity;
    @VisibleForTesting
    PowerGaugePreference mBatteryChargeCycles;
    @VisibleForTesting
    PowerGaugePreference mLastFullChargePref;
    @VisibleForTesting
    PowerGaugePreference mBatteryTemp;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    LayoutPreference mBatteryLayoutPref;
    @VisibleForTesting
    BatteryInfo mBatteryInfo;

    @VisibleForTesting
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    @VisibleForTesting
    boolean mNeedUpdateBatteryTip;
    @VisibleForTesting
    BatteryTipPreferenceController mBatteryTipPreferenceController;
    BatteryMeterView mBatteryView;

    @VisibleForTesting
    final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            restartBatteryInfoLoader();
        }
    };

    @VisibleForTesting
    LoaderManager.LoaderCallbacks<BatteryInfo> mBatteryInfoLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<BatteryInfo>() {

                @Override
                public Loader<BatteryInfo> onCreateLoader(int i, Bundle bundle) {
                    return new BatteryInfoLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<BatteryInfo> loader, BatteryInfo batteryInfo) {
                    mBatteryHeaderPreferenceController.updateHeaderPreference(batteryInfo);
                    mBatteryInfo = batteryInfo;
                    updateLastFullChargePreference();
                }

                @Override
                public void onLoaderReset(Loader<BatteryInfo> loader) {
                    // do nothing
                }
            };

    LoaderManager.LoaderCallbacks<List<BatteryInfo>> mBatteryInfoDebugLoaderCallbacks =
            new LoaderCallbacks<List<BatteryInfo>>() {
                @Override
                public Loader<List<BatteryInfo>> onCreateLoader(int i, Bundle bundle) {
                    return new DebugEstimatesLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryInfo>> loader,
                        List<BatteryInfo> batteryInfos) {
                    updateViews(batteryInfos);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryInfo>> loader) {
                }
            };

    protected void updateViews(List<BatteryInfo> batteryInfos) {
        final TextView percentRemaining =
                mBatteryLayoutPref.findViewById(R.id.battery_percent);
        final TextView summary1 = mBatteryLayoutPref.findViewById(R.id.summary1);
        BatteryInfo oldInfo = batteryInfos.get(0);
        BatteryInfo newInfo = batteryInfos.get(1);
        percentRemaining.setText(Utils.formatPercentage(oldInfo.batteryLevel));

        // set the text to the old estimate (copied from battery info). Note that this
        // can sometimes say 0 time remaining because battery stats requires the phone
        // be unplugged for a period of time before being willing ot make an estimate.
        final String OldEstimateString = mPowerFeatureProvider.getOldEstimateDebugString(
                Formatter.formatShortElapsedTime(getContext(),
                        PowerUtil.convertUsToMs(oldInfo.remainingTimeUs)));
        final String NewEstimateString = mPowerFeatureProvider.getEnhancedEstimateDebugString(
                Formatter.formatShortElapsedTime(getContext(),
                        PowerUtil.convertUsToMs(newInfo.remainingTimeUs)));
        summary1.setText(OldEstimateString + "\n" + NewEstimateString);

        mBatteryView.setBatteryLevel(oldInfo.batteryLevel);
        mBatteryView.setCharging(!oldInfo.discharging);
    }

    private LoaderManager.LoaderCallbacks<List<BatteryTip>> mBatteryTipsCallbacks =
            new LoaderManager.LoaderCallbacks<List<BatteryTip>>() {

                @Override
                public Loader<List<BatteryTip>> onCreateLoader(int id, Bundle args) {
                    return new BatteryTipLoader(getContext(), mStatsHelper);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryTip>> loader,
                        List<BatteryTip> data) {
                    mBatteryTipPreferenceController.updateBatteryTips(data);
                }

                @Override
                public void onLoaderReset(Loader<List<BatteryTip>> loader) {

                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final SettingsActivity activity = (SettingsActivity) getActivity();

        mBatteryHeaderPreferenceController = use(BatteryHeaderPreferenceController.class);
        mBatteryHeaderPreferenceController.setActivity(activity);
        mBatteryHeaderPreferenceController.setFragment(this);
        mBatteryHeaderPreferenceController.setLifecycle(getSettingsLifecycle());

        mBatteryTipPreferenceController = use(BatteryTipPreferenceController.class);
        mBatteryTipPreferenceController.setActivity(activity);
        mBatteryTipPreferenceController.setFragment(this);
        mBatteryTipPreferenceController.setBatteryTipListener(this::onBatteryTipHandled);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        initFeatureProvider();
        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);
        mBatteryView = mBatteryLayoutPref.findViewById(R.id.battery_header_icon);
        mBatteryView.setDrawableStyle();

        mScreenUsagePref = (PowerGaugePreference) findPreference(KEY_SCREEN_USAGE);
        mCurrentBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_CURRENT_BATTERY_CAPACITY);
        mDesignedBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_DESIGNED_BATTERY_CAPACITY);
        mBatteryChargeCycles = (PowerGaugePreference) findPreference(
                KEY_BATTERY_CHARGE_CYCLES);
        mLastFullChargePref = (PowerGaugePreference) findPreference(
                KEY_TIME_SINCE_LAST_FULL_CHARGE);
        mBatteryTemp = (PowerGaugePreference) findPreference(KEY_BATTERY_TEMP);
        mBatteryUtils = BatteryUtils.getInstance(getContext());

        mBatteryInfoCat = (PreferenceCategory) findPreference(KEY_BATTERY_INFO_CATEGORY);

        if (Utils.isBatteryPresent(getContext())) {
            restartBatteryInfoLoader();
        }
        mBatteryTipPreferenceController.restoreInstanceState(icicle);
        updateBatteryTipFlag(icicle);

        // Check availability of Battery Health
        if (!getResources().getBoolean(R.bool.config_supportBatteryHealth)) {
            getPreferenceScreen().removePreference(mBatteryInfoCat);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AdaptiveChargingPreferenceController(context));
        controllers.add(new AdaptiveChargingTemperaturePreferenceController(context));
        return controllers;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BATTERY_HEADER.equals(preference.getKey())) {
            new SubSettingLauncher(getContext())
                        .setDestination(PowerUsageAdvanced.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.advanced_battery_title)
                        .launch();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Global.getUriFor(Global.BATTERY_ESTIMATES_LAST_UPDATE_TIME),
                false,
                mSettingsObserver);
        mBatteryView.setDrawableStyle();
    }

    @Override
    public void onPause() {
        getContentResolver().unregisterContentObserver(mSettingsObserver);
        super.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_POWER_USAGE_SUMMARY_V2;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_summary;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        MenuItem reset = menu.add(0, MENU_STATS_RESET, 0, R.string.battery_stats_reset)
                .setIcon(R.drawable.ic_reset)
                .setAlphabeticShortcut('r');
        reset.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void resetStats() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.battery_stats_reset)
            .setMessage(R.string.battery_stats_message)
            .setPositiveButton(R.string.battery_stats_clear, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mStatsHelper.resetStatistics();
                    refreshUi(BatteryUpdateType.MANUAL);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create();
        dialog.show();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_RESET:
                resetStats();
                return true;
            case MENU_ADVANCED_BATTERY:
                new SubSettingLauncher(getContext())
                        .setDestination(PowerUsageAdvanced.class.getName())
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setTitleRes(R.string.advanced_battery_title)
                        .launch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void refreshUi(@BatteryUpdateType int refreshType) {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        // Skip refreshing UI if battery is not present.
        if (!mIsBatteryPresent) {
            return;
        }

        // Skip BatteryTipLoader if device is rotated or only battery level change
        if (mNeedUpdateBatteryTip
                && refreshType != BatteryUpdateType.BATTERY_LEVEL) {
            restartBatteryTipLoader();
        } else {
            mNeedUpdateBatteryTip = true;
        }
        mBatDesCap = getResources().getString(R.string.config_batDesCap);
        mBatCurCap = getResources().getString(R.string.config_batCurCap);
        mBatChgCyc = getResources().getString(R.string.config_batChargeCycle);

        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();
        updateLastFullChargePreference();
        mScreenUsagePref.setSubtitle(StringUtil.formatElapsedTime(getContext(),
                mBatteryUtils.calculateScreenUsageTime(mStatsHelper), false));
        mCurrentBatteryCapacity.setSubtitle(parseBatterymAhText(mBatCurCap));
        mDesignedBatteryCapacity.setSubtitle(parseBatterymAhText(mBatDesCap));
        mBatteryChargeCycles.setSubtitle(parseBatteryCycle(mBatChgCyc));
        mBatteryTemp.setSubtitle(
                EvolutionUtils.mccCheck(getContext()) ?
                EvolutionUtils.batteryTemperature(getContext(), true) + "°F" :
                EvolutionUtils.batteryTemperature(getContext(), false) + "°C");
    }

    @VisibleForTesting
    void restartBatteryTipLoader() {
        getLoaderManager().restartLoader(BATTERY_TIP_LOADER, Bundle.EMPTY, mBatteryTipsCallbacks);
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    void updateLastFullChargePreference() {
//       if (mBatteryInfo != null && mBatteryInfo.averageTimeToDischarge
//                != EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN) {
//            mLastFullChargePref.setTitle(R.string.battery_full_charge_last);
//            mLastFullChargePref.setSummary(
//                    StringUtil.formatElapsedTime(getContext(), mBatteryInfo.averageTimeToDischarge,
//                            false /* withSeconds */));
//        } else {
            final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                    System.currentTimeMillis());
            mLastFullChargePref.setTitle(R.string.battery_last_full_charge);
            mLastFullChargePref.setSubtitle(
                    StringUtil.formatRelativeTime(getContext(), lastFullChargeTime,
                            false /* withSeconds */));
//        }
    }

    @VisibleForTesting
    void showBothEstimates() {
        final Context context = getContext();
        if (context == null
                || !mPowerFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
            return;
        }
        getLoaderManager().restartLoader(DEBUG_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoDebugLoaderCallbacks);
    }

    @VisibleForTesting
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @VisibleForTesting
    void restartBatteryInfoLoader() {
        if (getContext() == null) {
            return;
        }
        // Skip restartBatteryInfoLoader if battery is not present.
        if (!mIsBatteryPresent) {
            return;
        }
        getLoaderManager().restartLoader(BATTERY_INFO_LOADER, Bundle.EMPTY,
                mBatteryInfoLoaderCallbacks);
        if (mPowerFeatureProvider.isEstimateDebugEnabled()) {
            // Set long click action for summary to show debug info
            View header = mBatteryLayoutPref.findViewById(R.id.summary1);
            header.setOnLongClickListener(this);
        }
    }

    @VisibleForTesting
    void updateBatteryTipFlag(Bundle icicle) {
        mNeedUpdateBatteryTip = icicle == null || mBatteryTipPreferenceController.needUpdate();
    }

    @Override
    public boolean onLongClick(View view) {
        showBothEstimates();
        view.setOnLongClickListener(null);
        return true;
    }

    @Override
    protected void restartBatteryStatsLoader(@BatteryUpdateType int refreshType) {
        super.restartBatteryStatsLoader(refreshType);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mBatteryTipPreferenceController.saveInstanceState(outState);
    }

    @Override
    public void onBatteryTipHandled(BatteryTip batteryTip) {
        restartBatteryTipLoader();
    }

    private String parseBatterymAhText(String file) {
        try {
            return Integer.parseInt(readLine(file)) / 1000 + " mAh";
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot read battery capacity from "
                    + file, ioe);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Read a badly formatted battery capacity from "
                    + file, nfe);
        }
        return getResources().getString(R.string.status_unavailable);
    }

    private String parseBatteryCycle(String file) {
        try {
            return Integer.parseInt(readLine(file)) + " Cycles";
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot read battery cycle from "
                    + file, ioe);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Read a badly formatted battery cycle from "
                    + file, nfe);
        }
        return getResources().getString(R.string.status_unavailable);
    }

    /**
    * Reads a line from the specified file.
    *
    * @param filename The file to read from.
    * @return The first line up to 256 characters, or <code>null</code> if file is empty.
    * @throws IOException If the file couldn't be read.
    */
    @Nullable
    private String readLine(String filename) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.power_usage_summary);
}
