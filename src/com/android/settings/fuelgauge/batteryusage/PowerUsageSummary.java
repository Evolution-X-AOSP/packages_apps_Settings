/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryHeaderPreferenceController;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryInfoLoader;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.batterytip.BatteryTipLoader;
import com.android.settings.fuelgauge.batterytip.BatteryTipPreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was consumed
 * since the last time it was unplugged.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class PowerUsageSummary extends PowerUsageBase implements
        BatteryTipPreferenceController.BatteryTipListener {

    static final String TAG = "PowerUsageSummary";

    @VisibleForTesting
    static final String KEY_BATTERY_ERROR = "battery_help_message";
    @VisibleForTesting
    static final String KEY_BATTERY_USAGE = "battery_usage_summary";

    private static final String KEY_CURRENT_BATTERY_CAPACITY = "current_battery_capacity";
    private static final String KEY_DESIGNED_BATTERY_CAPACITY = "designed_battery_capacity";
    private static final String KEY_BATTERY_CHARGE_CYCLES = "battery_charge_cycles";
    private static final String KEY_BATTERY_TEMP = "battery_temp";
    private static final String KEY_FAST_CHARGING = "fast_charging";

    private String mBatDesCap;
    private String mBatCurCap;
    private String mBatChgCyc;

    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    @VisibleForTesting
    LayoutPreference mBatteryLayoutPref;
    @VisibleForTesting
    BatteryInfo mBatteryInfo;
    @VisibleForTesting
    PowerGaugePreference mCurrentBatteryCapacity;
    @VisibleForTesting
    PowerGaugePreference mDesignedBatteryCapacity;
    @VisibleForTesting
    PowerGaugePreference mBatteryChargeCycles;
    @VisibleForTesting
    PowerGaugePreference mBatteryTempPref;

    @VisibleForTesting
    BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    @VisibleForTesting
    BatteryTipPreferenceController mBatteryTipPreferenceController;
    @VisibleForTesting
    boolean mNeedUpdateBatteryTip;
    @VisibleForTesting
    Preference mHelpPreference;
    @VisibleForTesting
    Preference mBatteryUsagePreference;

    Preference mSleepMode;

    @VisibleForTesting
    final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            restartBatteryInfoLoader();
            initPreference();
        }
    };

    @VisibleForTesting
    LoaderManager.LoaderCallbacks<BatteryInfo> mBatteryInfoLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<BatteryInfo>() {

                @Override
                public Loader<BatteryInfo> onCreateLoader(int i, Bundle bundle) {
                    return new BatteryInfoLoader(getContext());
                }

                @Override
                public void onLoadFinished(Loader<BatteryInfo> loader, BatteryInfo batteryInfo) {
                    mBatteryHeaderPreferenceController.updateHeaderPreference(batteryInfo);
                    mBatteryHeaderPreferenceController.updateHeaderByBatteryTips(
                            mBatteryTipPreferenceController.getCurrentBatteryTip(), batteryInfo);
                    mBatteryInfo = batteryInfo;
                }

                @Override
                public void onLoaderReset(Loader<BatteryInfo> loader) {
                    // do nothing
                }
            };

    private LoaderManager.LoaderCallbacks<List<BatteryTip>> mBatteryTipsCallbacks =
            new LoaderManager.LoaderCallbacks<List<BatteryTip>>() {

                @Override
                public Loader<List<BatteryTip>> onCreateLoader(int id, Bundle args) {
                    return new BatteryTipLoader(getContext(), mBatteryUsageStats);
                }

                @Override
                public void onLoadFinished(Loader<List<BatteryTip>> loader,
                        List<BatteryTip> data) {
                    mBatteryTipPreferenceController.updateBatteryTips(data);
                    mBatteryHeaderPreferenceController.updateHeaderByBatteryTips(
                            mBatteryTipPreferenceController.getCurrentBatteryTip(), mBatteryInfo);
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
        initPreference();

        mCurrentBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_CURRENT_BATTERY_CAPACITY);
        mDesignedBatteryCapacity = (PowerGaugePreference) findPreference(
                KEY_DESIGNED_BATTERY_CAPACITY);
        mBatteryChargeCycles = (PowerGaugePreference) findPreference(
                KEY_BATTERY_CHARGE_CYCLES);
        mBatteryUtils = BatteryUtils.getInstance(getContext());

        // Check availability of Battery Health
        Preference mDesignedHealthPref = (Preference) findPreference(KEY_DESIGNED_BATTERY_CAPACITY);
        if (!getResources().getBoolean(R.bool.config_supportBatteryHealth)) {
            getPreferenceScreen().removePreference(mDesignedHealthPref);
        }

        Preference mCurrentHealthPref = (Preference) findPreference(KEY_CURRENT_BATTERY_CAPACITY);
        if (!getResources().getBoolean(R.bool.config_supportBatteryHealth)) {
            getPreferenceScreen().removePreference(mCurrentHealthPref);
        }

        Preference mCyclesHealthPref = (Preference) findPreference(KEY_BATTERY_CHARGE_CYCLES);
        if (!getResources().getBoolean(R.bool.config_supportBatteryHealth)) {
            getPreferenceScreen().removePreference(mCyclesHealthPref);
        }

        if (Utils.isBatteryPresent(getContext())) {
            restartBatteryInfoLoader();
        } else {
            // Present help preference when battery is unavailable.
            mHelpPreference.setVisible(true);
        }
        mBatteryTipPreferenceController.restoreInstanceState(icicle);
        updateBatteryTipFlag(icicle);

        mSleepMode = findPreference("sleep_mode");
        updateSleepModeSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Global.getUriFor(Global.BATTERY_ESTIMATES_LAST_UPDATE_TIME),
                false,
                mSettingsObserver);
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor("battery_24_hrs_stats"),
                false,
                mSettingsObserver);
        updateSleepModeSummary();
    }

    @Override
    public void onPause() {
        getContentResolver().unregisterContentObserver(mSettingsObserver);
        updateSleepModeSummary();
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

    private void updateSleepModeSummary() {
        if (mSleepMode == null) return;
        final boolean enabled = Secure.getIntForUser(getActivity().getContentResolver(),
                Secure.SLEEP_MODE_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        final int mode = Secure.getIntForUser(getActivity().getContentResolver(),
                Secure.SLEEP_MODE_AUTO_MODE, 0, UserHandle.USER_CURRENT);
        String timeValue = Secure.getStringForUser(getActivity().getContentResolver(),
                Secure.SLEEP_MODE_AUTO_TIME, UserHandle.USER_CURRENT);
        if (timeValue == null || timeValue.equals("")) timeValue = "22:00,07:00";
        final String[] time = timeValue.split(",", 0);
        final String outputFormat = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm a";
        final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        final LocalTime sinceValue = LocalTime.parse(time[0], formatter);
        final LocalTime tillValue = LocalTime.parse(time[1], formatter);
        String detail;
        switch (mode) {
            default:
            case 0:
                detail = getActivity().getString(enabled
                        ? R.string.night_display_summary_on_auto_mode_never
                        : R.string.night_display_summary_off_auto_mode_never);
                break;
            case 1:
                detail = getActivity().getString(enabled
                        ? R.string.night_display_summary_on_auto_mode_twilight
                        : R.string.night_display_summary_off_auto_mode_twilight);
                break;
            case 2:
                if (enabled) {
                    detail = getActivity().getString(R.string.night_display_summary_on_auto_mode_custom, tillValue.format(outputFormatter));
                } else {
                    detail = getActivity().getString(R.string.night_display_summary_off_auto_mode_custom, sinceValue.format(outputFormatter));
                }
                break;
            case 3:
                if (enabled) {
                    detail = getActivity().getString(R.string.night_display_summary_on_auto_mode_custom, tillValue.format(outputFormatter));
                } else {
                    detail = getActivity().getString(R.string.night_display_summary_off_auto_mode_twilight);
                }
                break;
            case 4:
                if (enabled) {
                    detail = getActivity().getString(R.string.night_display_summary_on_auto_mode_twilight);
                } else {
                    detail = getActivity().getString(R.string.night_display_summary_off_auto_mode_custom, sinceValue.format(outputFormatter));
                }
                break;
        }
        final String summary = getActivity().getString(enabled
                ? R.string.sleep_mode_summary_on
                : R.string.sleep_mode_summary_off, detail);
        mSleepMode.setSummary(summary);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    protected boolean isBatteryHistoryNeeded() {
        return false;
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
        // reload BatteryInfo and updateUI
        restartBatteryInfoLoader();

        mBatDesCap = getResources().getString(R.string.config_batDesCap);
        mBatCurCap = getResources().getString(R.string.config_batCurCap);
        mBatChgCyc = getResources().getString(R.string.config_batChargeCycle);

        mCurrentBatteryCapacity.setSubtitle(parseBatterymAhText(mBatCurCap));
        mDesignedBatteryCapacity.setSubtitle(parseBatterymAhText(mBatDesCap));
        mBatteryChargeCycles.setSubtitle(parseBatteryCycle(mBatChgCyc));

        if (BatteryInfo.batteryTemp != 0f) {
            mBatteryTempPref.setSubtitle(BatteryInfo.batteryTemp + " \u2103");
        } else {
            mBatteryTempPref.setSubtitle(getResources().getString(R.string.status_unavailable));
        }
    }

    @VisibleForTesting
    void restartBatteryTipLoader() {
        restartLoader(LoaderIndex.BATTERY_TIP_LOADER, Bundle.EMPTY, mBatteryTipsCallbacks);
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    @VisibleForTesting
    void initPreference() {
        mBatteryUsagePreference = findPreference(KEY_BATTERY_USAGE);
        mBatteryUsagePreference.setSummary(getString(R.string.advanced_battery_preference_summary));

        mBatteryTempPref = (PowerGaugePreference) findPreference(KEY_BATTERY_TEMP);
        mHelpPreference = findPreference(KEY_BATTERY_ERROR);
        mHelpPreference.setVisible(false);
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
        restartLoader(LoaderIndex.BATTERY_INFO_LOADER, Bundle.EMPTY, mBatteryInfoLoaderCallbacks);
    }

    @VisibleForTesting
    void updateBatteryTipFlag(Bundle icicle) {
        mNeedUpdateBatteryTip = icicle == null || mBatteryTipPreferenceController.needUpdate();
    }

    @Override
    protected void restartBatteryStatsLoader(@BatteryUpdateType int refreshType) {
        super.restartBatteryStatsLoader(refreshType);
        // Update battery header if battery is present.
        if (mIsBatteryPresent) {
            mBatteryHeaderPreferenceController.quickUpdateHeaderPreference();
        }
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
            new BaseSearchIndexProvider(R.xml.power_usage_summary) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    final Resources res = context.getResources();

                    boolean mFastChargingSupported = res.getBoolean(
                            R.bool.config_lineageFastChargeSupported);
                    if (!mFastChargingSupported)
                        keys.add(KEY_FAST_CHARGING);

                    return keys;
                }
            };
}
