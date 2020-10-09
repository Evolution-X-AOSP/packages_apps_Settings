package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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

    private void setPackageManager(PackageManager packageManager) {
        mPackageManager = packageManager;
    }

    public Estimate getEnhancedBatteryPrediction(Context context) {
        long j;
        Cursor query = context.getContentResolver().query(getEnhancedBatteryPredictionUri(), (String[]) null, (String) null, (String[]) null, (String) null);
        if (query != null) {
            try {
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
                    if (columnIndex2 != -1) {
                        long j2 = query.getLong(columnIndex2);
                        if (j2 != -1) {
                            long millis = Duration.ofMinutes(15).toMillis();
                            if (Duration.ofMillis(j2).compareTo(Duration.ofDays(1)) >= 0) {
                                millis = Duration.ofHours(1).toMillis();
                            }
                            j = PowerUtil.roundTimeToNearestThreshold(j2, millis);
                            Estimate estimate = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z2, j);
                            if (query != null) {
                                query.close();
                            }
                            return estimate;
                        }
                    }
                    j = -1;
                    Estimate estimate2 = new Estimate(query.getLong(query.getColumnIndex(BATTERY_ESTIMATE_COL)), z2, j);
                    if (query != null) {
                    }
                    return estimate2;
                }
            } catch (Throwable th) {
                th.addSuppressed(th);
            }
        }
        if (query == null) {
            return null;
        }
        query.close();
        return null;
    }

    public SparseIntArray getEnhancedBatteryPredictionCurve(Context context, long j) {
        Cursor query;
        try {
            query = context.getContentResolver().query(getEnhancedBatteryPredictionCurveUri(), (String[]) null, (String) null, (String[]) null, (String) null);
            if (query == null) {
                if (query != null) {
                    query.close();
                }
                return null;
            }
            int columnIndex = query.getColumnIndex(TIMESTAMP_COL);
            int columnIndex2 = query.getColumnIndex(BATTERY_LEVEL_COL);
            SparseIntArray sparseIntArray = new SparseIntArray(query.getCount());
            while (query.moveToNext()) {
                sparseIntArray.append((int) (query.getLong(columnIndex) - j), query.getInt(columnIndex2));
            }
            if (query != null) {
                query.close();
            }
            return sparseIntArray;
        } catch (NullPointerException unused) {
            return null;
        }
    }

    public boolean isEnhancedBatteryPredictionEnabled(Context context) {
        try {
            if (!mPackageManager.getPackageInfo("com.google.android.apps.turbo", 512).applicationInfo.enabled) {
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    private Uri getEnhancedBatteryPredictionUri() {
        return new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("time_remaining").build();
    }

    private Uri getEnhancedBatteryPredictionCurveUri() {
        return new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("discharge_curve").build();
    }

    public String getEnhancedEstimateDebugString(String str) {
        return mContext.getString(R.string.power_usage_enhanced_debug, new Object[]{str});
    }

    public String getOldEstimateDebugString(String str) {
        return mContext.getString(R.string.power_usage_old_debug, new Object[]{str});
    }

    public String getAdvancedUsageScreenInfoString() {
        return mContext.getString(R.string.advanced_battery_graph_subtext);
    }

    public boolean getEarlyWarningSignal(Context context, String str) {
        Uri.Builder appendPath = new Uri.Builder().scheme("content").authority("com.google.android.apps.turbo.estimated_time_remaining").appendPath("early_warning").appendPath("id");
        if (TextUtils.isEmpty(str)) {
            appendPath.appendPath(context.getPackageName());
        } else {
            appendPath.appendPath(str);
        }
        Cursor query = context.getContentResolver().query(appendPath.build(), (String[]) null, (String) null, (String[]) null, (String) null);
        boolean z = false;
        if (query != null) {
            try {
                if (query.moveToFirst()) {
                    if (1 == query.getInt(query.getColumnIndex(IS_EARLY_WARNING_COL))) {
                        z = true;
                    }
                    if (query != null) {
                        query.close();
                    }
                    return z;
                }
            } catch (Throwable th) {
                th.addSuppressed(th);
            }
        }
        if (query != null) {
            query.close();
        }
        return false;
    }
}
