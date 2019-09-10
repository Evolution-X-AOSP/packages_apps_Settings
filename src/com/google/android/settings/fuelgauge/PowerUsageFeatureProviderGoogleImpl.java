package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;
import android.util.SparseIntArray;
import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.utils.PowerUtil;
import java.time.Duration;

public class PowerUsageFeatureProviderGoogleImpl extends PowerUsageFeatureProviderImpl {
    static final String AVERAGE_BATTERY_LIFE_COL = "average_battery_life";
    static final String BATTERY_ESTIMATE_BASED_ON_USAGE_COL = "is_based_on_usage";
    static final String BATTERY_ESTIMATE_COL = "battery_estimate";
    static final String BATTERY_LEVEL_COL = "battery_level";
    static final int CUSTOMIZED_TO_USER = 1;
    static final String GFLAG_ADDITIONAL_BATTERY_INFO_ENABLED = "settingsgoogle:additional_battery_info_enabled";
    static final String GFLAG_BATTERY_ADVANCED_UI_ENABLED = "settingsgoogle:battery_advanced_ui_enabled";
    static final String GFLAG_POWER_ACCOUNTING_TOGGLE_ENABLED = "settingsgoogle:power_accounting_toggle_enabled";
    static final String IS_EARLY_WARNING_COL = "is_early_warning";
    static final int NEED_EARLY_WARNING = 1;
    private static final String[] PACKAGES_SERVICE = {"com.google.android.gms", "com.google.android.apps.gcs"};
    static final String TIMESTAMP_COL = "timestamp_millis";

    public PowerUsageFeatureProviderGoogleImpl(Context context) {
        super(context);
    }

    @Override
    public boolean isTypeService(BatterySipper batterySipper) {
        String[] packagesForUid = mPackageManager.getPackagesForUid(batterySipper.getUid());
        if (packagesForUid == null) {
            return false;
        }
        for (String contains : packagesForUid) {
            if (ArrayUtils.contains(PACKAGES_SERVICE, contains)) {
                return true;
            }
        }
        return false;
    }

    public void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    @Override
    public Estimate getEnhancedBatteryPrediction(Context context) {
        Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionUri(), null, null, null, null);
        if (query != null) {
            if (query.moveToFirst()) {
                int columnIndex = query.getColumnIndex(BATTERY_ESTIMATE_BASED_ON_USAGE_COL);
                boolean z = true;
                if (columnIndex != -1) {
                    if (1 != query.getInt(columnIndex)) {
                        z = false;
                    }
                }
                boolean z2 = z;
                int columnIndex2 = query.getColumnIndex(AVERAGE_BATTERY_LIFE_COL);
                long j = -1;
                if (columnIndex2 != -1) {
                    long j2 = query.getLong(columnIndex2);
                    if (j2 != -1) {
                        long millis = Duration.ofMinutes(15).toMillis();
                        if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1)) >= 0) {
                            millis = Duration.ofHours(1).toMillis();
                        }
                        j = PowerUtil.roundTimeToNearestThreshold(j2, millis);
                    }
                }
                Estimate estimate = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z2, j);
                cleanupCursor(query);
                return estimate;
            }
        }
        cleanupCursor(query);
        return null;
    }

    @Override
    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long j) {
        Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionCurveUri(), null, null, null, null);
        try {
            if (query == null || !query.moveToFirst()) {
                cleanupCursor(query);
                return null;
            }
            int columnIndex = query.getColumnIndex(TIMESTAMP_COL);
            int columnIndex2 = query.getColumnIndex(BATTERY_LEVEL_COL);
            SparseIntArray sparseIntArray = new SparseIntArray(query.getCount());
            while (query.moveToNext()) {
                sparseIntArray.append((int) (query.getLong(columnIndex) - j), query.getInt(columnIndex2));
            }
            cleanupCursor(query);
            return sparseIntArray;
        } catch (NullPointerException unused) {
        }
        cleanupCursor(query);
        return null;
    }

    @Override
    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        try {
            return mPackageManager.getPackageInfo("com.google.android.apps.turbo",
                                    PackageManager.MATCH_DISABLED_COMPONENTS).applicationInfo.enabled;
        }catch (Exception unused) {
        }
        return false;
    }

    private Uri getEnhancedBatteryPredictionUri() {
        return new Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("time_remaining").build();
    }

    private Uri getEnhancedBatteryPredictionCurveUri() {
        return new Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("discharge_curve").build();
    }

    @Override
    public String getEnhancedEstimateDebugString(String str) {
        return mContext.getString(R.string.power_usage_enhanced_debug, str);
    }

    @Override
    public String getOldEstimateDebugString(String str) {
        return mContext.getString(R.string.power_usage_old_debug, str);
    }

    @Override
    public String getAdvancedUsageScreenInfoString() {
        return mContext.getString(R.string.advanced_battery_graph_subtext);
    }

    @Override
    public boolean getEarlyWarningSignal(Context context, String str) {
        Builder appendPath = new Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("early_warning").appendPath("id");
        if (TextUtils.isEmpty(str)) {
            appendPath.appendPath(context.getPackageName());
        } else {
            appendPath.appendPath(str);
        }
        Cursor query = context.getContentResolver().query(appendPath.build(), null, null, null, null);
        boolean z = false;
        if (query != null) {
            if (query.moveToFirst()) {
                if (1 == query.getInt(query.getColumnIndex(IS_EARLY_WARNING_COL))) {
                    z = true;
                }
                cleanupCursor(query);
                return z;
            }
        }
        cleanupCursor(query);
        return false;
    }

    private void cleanupCursor(Cursor query){
        try {
            query.close();
            query = null;
        }catch (NullPointerException unused) {
        }
    }

}
