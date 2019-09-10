package com.google.android.settings.applications;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.icu.text.MeasureFormat;
import android.icu.text.MeasureFormat.FormatWidth;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.os.Bundle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.applications.ApplicationFeatureProviderImpl;
import java.util.Locale;
import java.util.Set;

public class ApplicationFeatureProviderGoogleImpl extends ApplicationFeatureProviderImpl {
    private static final boolean DEBUG = Log.isLoggable("ApplicationFeatureProviderGoogleImpl", 3);
    private final Context mContext;

    public ApplicationFeatureProviderGoogleImpl(Context context, PackageManager packageManager, IPackageManager iPackageManager, DevicePolicyManager devicePolicyManager) {
        super(context, packageManager, iPackageManager, devicePolicyManager);
        mContext = context;
    }

    @Override
    public Set<String> getKeepEnabledPackages() {
        Set<String> keepEnabledPackages = super.getKeepEnabledPackages();
        keepEnabledPackages.add("com.google.android.inputmethod.latin");
        keepEnabledPackages.add("com.google.android.dialer");
        keepEnabledPackages.add("com.google.android.apps.wellbeing");
        keepEnabledPackages.add("com.google.android.settings.intelligence");
        keepEnabledPackages.add("com.google.android.ims");
        keepEnabledPackages.add("com.google.android.packageinstaller");
        keepEnabledPackages.add("com.android.packageinstaller");
        return keepEnabledPackages;
    }

    @Override
    public CharSequence getTimeSpentInApp(String str) {
        String str2 = "com.google.android.apps.wellbeing.api";
        String str3 = "";
        String str4 = "ApplicationFeatureProviderGoogleImpl";
        try {
            if (!isPrivilegedApp(str2)) {
                if (DEBUG) {
                    Log.d(str4, "Not a privileged app.");
                }
                return str3;
            }
            Bundle bundle = new Bundle();
            bundle.putString("packageName", str);
            Bundle call = mContext.getContentResolver().call(str2, "get_app_usage_millis", null, bundle);
            if (call != null) {
                if (call.getBoolean("success")) {
                    Bundle bundle2 = call.getBundle("data");
                    if (bundle2 == null) {
                        if (DEBUG) {
                            Log.d(str4, "data bundle is null.");
                        }
                        return str3;
                    }
                    String readableDuration = getReadableDuration(Long.valueOf(bundle2.getLong("total_time_millis")), FormatWidth.NARROW, R.string.duration_less_than_one_minute, false);
                    return mContext.getString(R.string.screen_time_summary_usage_today, new Object[]{readableDuration});
                }
            }
            if (DEBUG) {
                Log.d(str4, "Provider call unsuccessful");
            }
            return str3;
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error getting time spent for app ");
            sb.append(str);
            Log.w(str4, sb.toString(), e);
            return str3;
        }
    }

    private String getReadableDuration(Long l, FormatWidth formatWidth, int i, boolean z) {
        long j;
        long j2;
        long longValue = l.longValue();
        if (longValue >= 3600000) {
            j = longValue / 3600000;
            longValue -= 3600000 * j;
        } else {
            j = 0;
        }
        if (longValue >= 60000) {
            j2 = longValue / 60000;
            longValue -= 60000 * j2;
        } else {
            j2 = 0;
        }
        int i2 = (j > 0 ? 1 : (j == 0 ? 0 : -1));
        if (i2 > 0 && j2 > 0) {
            return MeasureFormat.getInstance(Locale.getDefault(), formatWidth).formatMeasures(new Measure[]{new Measure(Long.valueOf(j), MeasureUnit.HOUR), new Measure(Long.valueOf(j2), MeasureUnit.MINUTE)});
        } else if (i2 > 0) {
            Locale locale = Locale.getDefault();
            if (!z) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale, formatWidth).formatMeasures(new Measure[]{new Measure(Long.valueOf(j), MeasureUnit.HOUR)});
        } else if (j2 > 0) {
            Locale locale2 = Locale.getDefault();
            if (!z) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale2, formatWidth).formatMeasures(new Measure[]{new Measure(Long.valueOf(j2), MeasureUnit.MINUTE)});
        } else if (longValue > 0) {
            return mContext.getResources().getString(i);
        } else {
            Locale locale3 = Locale.getDefault();
            if (!z) {
                formatWidth = FormatWidth.WIDE;
            }
            return MeasureFormat.getInstance(locale3, formatWidth).formatMeasures(new Measure[]{new Measure(Integer.valueOf(0), MeasureUnit.MINUTE)});
        }
    }

    private boolean isPrivilegedApp(String str) {
        ProviderInfo resolveContentProvider = mContext.getPackageManager().resolveContentProvider(str, 0);
        if (resolveContentProvider == null) {
            return false;
        }
        return resolveContentProvider.applicationInfo.isPrivilegedApp();
    }
}
