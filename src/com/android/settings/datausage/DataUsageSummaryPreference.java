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

package com.android.settings.datausage;

import android.annotation.AttrRes;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.icu.text.MessageFormat;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.Utils;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.utils.StringUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Provides a summary of data usage.
 */
public class DataUsageSummaryPreference extends Preference {
    private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long WARNING_AGE = TimeUnit.HOURS.toMillis(6L);
    @VisibleForTesting
    static final Typeface SANS_SERIF_MEDIUM =
            Typeface.create("@*android:string/config_bodyFontFamilyMedium", Typeface.NORMAL);

    private boolean mChartEnabled = true;
    private CharSequence mStartLabel;
    private CharSequence mEndLabel;

    /** large vs small size is 36/16 ~ 2.25 */
    private static final float LARGER_FONT_RATIO = 2.25f;
    private static final float SMALLER_FONT_RATIO = 1.0f;

    private boolean mDefaultTextColorSet;
    private int mDefaultTextColor;
    private int mNumPlans;
    /** The specified un-initialized value for cycle time */
    private final long CYCLE_TIME_UNINITIAL_VALUE = 0;
    /** The ending time of the billing cycle in milliseconds since epoch. */
    private long mCycleEndTimeMs;
    /** The time of the last update in standard milliseconds since the epoch */
    private long mSnapshotTimeMs;
    /** Name of carrier, or null if not available */
    private CharSequence mCarrierName;
    private CharSequence mLimitInfoText;
    private Intent mLaunchIntent;

    /** Progress to display on ProgressBar */
    private float mProgress;
    private boolean mHasMobileData;

    /**
     * The size of the first registered plan if one exists or the size of the warning if it is set.
     * -1 if no information is available.
     */
    private long mDataplanSize;

    /** The number of bytes used since the start of the cycle. */
    private long mDataplanUse;

    /** WiFi only mode */
    private boolean mWifiMode;
    private String mUsagePeriod;
    private boolean mSingleWifi;    // Shows only one specified WiFi network usage

    public DataUsageSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_summary_preference);
    }

    public void setLimitInfo(CharSequence text) {
        if (!Objects.equals(text, mLimitInfoText)) {
            mLimitInfoText = text;
            notifyChanged();
        }
    }

    public void setProgress(float progress) {
        mProgress = progress;
        notifyChanged();
    }

    public void setUsageInfo(long cycleEnd, long snapshotTime, CharSequence carrierName,
            int numPlans, Intent launchIntent) {
        mCycleEndTimeMs = cycleEnd;
        mSnapshotTimeMs = snapshotTime;
        mCarrierName = carrierName;
        mNumPlans = numPlans;
        mLaunchIntent = launchIntent;
        notifyChanged();
    }

    public void setChartEnabled(boolean enabled) {
        if (mChartEnabled != enabled) {
            mChartEnabled = enabled;
            notifyChanged();
        }
    }

    public void setLabels(CharSequence start, CharSequence end) {
        mStartLabel = start;
        mEndLabel = end;
        notifyChanged();
    }

    void setUsageNumbers(long used, long dataPlanSize, boolean hasMobileData) {
        mDataplanUse = used;
        mDataplanSize = dataPlanSize;
        mHasMobileData = hasMobileData;
        notifyChanged();
    }

    void setWifiMode(boolean isWifiMode, String usagePeriod, boolean isSingleWifi) {
        mWifiMode = isWifiMode;
        mUsagePeriod = usagePeriod;
        mSingleWifi = isSingleWifi;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ProgressBar bar = getProgressBar(holder);
        if (mChartEnabled && (!TextUtils.isEmpty(mStartLabel) || !TextUtils.isEmpty(mEndLabel))) {
            bar.setVisibility(View.VISIBLE);
            getLabelBar(holder).setVisibility(View.VISIBLE);
            bar.setProgress((int) (mProgress * 100));
            (getLabel1(holder)).setText(mStartLabel);
            (getLabel2(holder)).setText(mEndLabel);
        } else {
            bar.setVisibility(View.GONE);
            getLabelBar(holder).setVisibility(View.GONE);
        }

        updateDataUsageLabels(holder);

        TextView usageTitle = getUsageTitle(holder);
        TextView carrierInfo = getCarrierInfo(holder);
        Button launchButton = getLaunchButton(holder);
        TextView limitInfo = getDataLimits(holder);

        if (mWifiMode && mSingleWifi) {
            updateCycleTimeText(holder);

            usageTitle.setVisibility(View.GONE);
            launchButton.setVisibility(View.GONE);
            carrierInfo.setVisibility(View.GONE);

            limitInfo.setVisibility(TextUtils.isEmpty(mLimitInfoText) ? View.GONE : View.VISIBLE);
            limitInfo.setText(mLimitInfoText);
        } else if (mWifiMode) {
            usageTitle.setText(R.string.data_usage_wifi_title);
            usageTitle.setVisibility(View.VISIBLE);
            TextView cycleTime = getCycleTime(holder);
            cycleTime.setText(mUsagePeriod);
            carrierInfo.setVisibility(View.GONE);
            limitInfo.setVisibility(View.GONE);

            final long usageLevel = getHistoricalUsageLevel();
            if (usageLevel > 0L) {
                launchButton.setOnClickListener((view) -> {
                    launchWifiDataUsage(getContext());
                });
            } else {
                launchButton.setEnabled(false);
            }
            launchButton.setText(R.string.launch_wifi_text);
            launchButton.setVisibility(View.VISIBLE);
        } else {
            usageTitle.setVisibility(mNumPlans > 1 ? View.VISIBLE : View.GONE);
            updateCycleTimeText(holder);
            updateCarrierInfo(carrierInfo);
            if (mLaunchIntent != null) {
                launchButton.setOnClickListener((view) -> {
                    getContext().startActivity(mLaunchIntent);
                });
                launchButton.setVisibility(View.VISIBLE);
            } else {
                launchButton.setVisibility(View.GONE);
            }
            limitInfo.setVisibility(
                    TextUtils.isEmpty(mLimitInfoText) ? View.GONE : View.VISIBLE);
            limitInfo.setText(mLimitInfoText);
        }
    }

    @VisibleForTesting
    static void launchWifiDataUsage(Context context) {
        final Bundle args = new Bundle(1);
        args.putParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE,
                new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build());
        args.putInt(DataUsageList.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_WIFI);
        final SubSettingLauncher launcher = new SubSettingLauncher(context)
                .setArguments(args)
                .setDestination(DataUsageList.class.getName())
                .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN);
        launcher.setTitleRes(R.string.wifi_data_usage);
        launcher.launch();
    }

    private void updateDataUsageLabels(PreferenceViewHolder holder) {
        TextView usageNumberField = getDataUsed(holder);

        final Formatter.BytesResult usedResult = Formatter.formatBytes(getContext().getResources(),
                mDataplanUse, Formatter.FLAG_CALCULATE_ROUNDED | Formatter.FLAG_IEC_UNITS);
        final SpannableString usageNumberText = new SpannableString(usedResult.value);
        final int textSize =
                getContext().getResources().getDimensionPixelSize(R.dimen.usage_number_text_size);
        usageNumberText.setSpan(new AbsoluteSizeSpan(textSize), 0, usageNumberText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        CharSequence template = getContext().getText(R.string.data_used_formatted);

        CharSequence usageText =
                TextUtils.expandTemplate(template, usageNumberText, usedResult.units);
        usageNumberField.setText(usageText);

        final MeasurableLinearLayout layout = getLayout(holder);

        if (mHasMobileData && mNumPlans >= 0 && mDataplanSize > 0L) {
            TextView usageRemainingField = getDataRemaining(holder);
            long dataRemaining = mDataplanSize - mDataplanUse;
            if (dataRemaining >= 0) {
                usageRemainingField.setText(
                        TextUtils.expandTemplate(getContext().getText(R.string.data_remaining),
                                DataUsageUtils.formatDataUsage(getContext(), dataRemaining)));
                usageRemainingField.setTextColor(
                        Utils.getColorAttr(getContext(), android.R.attr.colorAccent));
            } else {
                usageRemainingField.setText(
                        TextUtils.expandTemplate(getContext().getText(R.string.data_overusage),
                                DataUsageUtils.formatDataUsage(getContext(), -dataRemaining)));
                usageRemainingField.setTextColor(
                        Utils.getColorAttr(getContext(), android.R.attr.colorError));
            }
            layout.setChildren(usageNumberField, usageRemainingField);
        } else {
            layout.setChildren(usageNumberField, null);
        }
    }

    private void updateCycleTimeText(PreferenceViewHolder holder) {
        TextView cycleTime = getCycleTime(holder);

        // Takes zero as a special case which value is never set.
        if (mCycleEndTimeMs == CYCLE_TIME_UNINITIAL_VALUE) {
            cycleTime.setVisibility(View.GONE);
            return;
        }

        cycleTime.setVisibility(View.VISIBLE);
        long millisLeft = mCycleEndTimeMs - System.currentTimeMillis();
        if (millisLeft <= 0) {
            cycleTime.setText(getContext().getString(R.string.billing_cycle_none_left));
        } else {
            int daysLeft = (int) (millisLeft / MILLIS_IN_A_DAY);
            MessageFormat msgFormat = new MessageFormat(
                    getContext().getResources().getString(R.string.billing_cycle_days_left),
                    Locale.getDefault());
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("count", daysLeft);
            cycleTime.setText(daysLeft < 1
                    ? getContext().getString(R.string.billing_cycle_less_than_one_day_left)
                    : msgFormat.format(arguments));
        }
    }


    private void updateCarrierInfo(TextView carrierInfo) {
        if (mNumPlans > 0 && mSnapshotTimeMs >= 0L) {
            carrierInfo.setVisibility(View.VISIBLE);
            long updateAgeMillis = calculateTruncatedUpdateAge();

            int textResourceId;
            CharSequence updateTime = null;
            if (updateAgeMillis == 0) {
                if (mCarrierName != null) {
                    textResourceId = R.string.carrier_and_update_now_text;
                } else {
                    textResourceId = R.string.no_carrier_update_now_text;
                }
            } else {
                if (mCarrierName != null) {
                    textResourceId = R.string.carrier_and_update_text;
                } else {
                    textResourceId = R.string.no_carrier_update_text;
                }
                updateTime = StringUtil.formatElapsedTime(
                        getContext(),
                        updateAgeMillis,
                        false /* withSeconds */,
                        false /* collapseTimeUnit */);
            }
            carrierInfo.setText(TextUtils.expandTemplate(
                    getContext().getText(textResourceId),
                    mCarrierName,
                    updateTime));

            if (updateAgeMillis <= WARNING_AGE) {
                setCarrierInfoTextStyle(
                        carrierInfo, android.R.attr.textColorSecondary, Typeface.DEFAULT);
            } else {
                setCarrierInfoTextStyle(carrierInfo, android.R.attr.colorError, SANS_SERIF_MEDIUM);
            }
        } else {
            carrierInfo.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the time since the last carrier update, as defined by {@link #mSnapshotTimeMs},
     * truncated to the nearest day / hour / minute in milliseconds, or 0 if less than 1 min.
     */
    private long calculateTruncatedUpdateAge() {
        long updateAgeMillis = System.currentTimeMillis() - mSnapshotTimeMs;

        // Round to nearest whole unit
        if (updateAgeMillis >= TimeUnit.DAYS.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.DAYS.toMillis(1)) * TimeUnit.DAYS.toMillis(1);
        } else if (updateAgeMillis >= TimeUnit.HOURS.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.HOURS.toMillis(1)) * TimeUnit.HOURS.toMillis(1);
        } else if (updateAgeMillis >= TimeUnit.MINUTES.toMillis(1)) {
            return (updateAgeMillis / TimeUnit.MINUTES.toMillis(1)) * TimeUnit.MINUTES.toMillis(1);
        } else {
            return 0;
        }
    }

    private void setCarrierInfoTextStyle(
            TextView carrierInfo, @AttrRes int colorId, Typeface typeface) {
        carrierInfo.setTextColor(Utils.getColorAttr(getContext(), colorId));
        carrierInfo.setTypeface(typeface);
    }

    @VisibleForTesting
    protected long getHistoricalUsageLevel() {
        final DataUsageController controller = new DataUsageController(getContext());
        return controller.getHistoricalUsageLevel(
                new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build());
    }

    @VisibleForTesting
    protected TextView getUsageTitle(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.usage_title);
    }

    @VisibleForTesting
    protected TextView getCycleTime(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.cycle_left_time);
    }

    @VisibleForTesting
    protected TextView getCarrierInfo(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.carrier_and_update);
    }

    @VisibleForTesting
    protected TextView getDataLimits(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_limits);
    }

    @VisibleForTesting
    protected TextView getDataUsed(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_usage_view);
    }

    @VisibleForTesting
    protected TextView getDataRemaining(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(R.id.data_remaining_view);
    }

    @VisibleForTesting
    protected Button getLaunchButton(PreferenceViewHolder holder) {
        return (Button) holder.findViewById(R.id.launch_mdp_app_button);
    }

    @VisibleForTesting
    protected LinearLayout getLabelBar(PreferenceViewHolder holder) {
        return (LinearLayout) holder.findViewById(R.id.label_bar);
    }

    @VisibleForTesting
    protected TextView getLabel1(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(android.R.id.text1);
    }

    @VisibleForTesting
    protected TextView getLabel2(PreferenceViewHolder holder) {
        return (TextView) holder.findViewById(android.R.id.text2);
    }

    @VisibleForTesting
    protected ProgressBar getProgressBar(PreferenceViewHolder holder) {
        return (ProgressBar) holder.findViewById(R.id.determinateBar);
    }

    @VisibleForTesting
    protected MeasurableLinearLayout getLayout(PreferenceViewHolder holder) {
        return (MeasurableLinearLayout) holder.findViewById(R.id.usage_layout);
    }
}
