/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.Nullable;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.graph.CircleBatteryDrawable;
import com.android.settingslib.graph.FullCircleBatteryDrawable;
import com.android.settingslib.graph.ThemedBatteryDrawable;

public class BatteryMeterView extends ImageView {
    @VisibleForTesting
    BatteryMeterDrawable mThemedDrawable;
    @VisibleForTesting
    ColorFilter mErrorColorFilter;
    @VisibleForTesting
    ColorFilter mAccentColorFilter;
    @VisibleForTesting
    ColorFilter mForegroundColorFilter;

    CircleBatteryDrawable mCircleDrawable;
    FullCircleBatteryDrawable mFullCircleDrawable;
    private int mIconStyle = 0;

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final int frameColor = context.getColor(R.color.meter_background_color);
        mAccentColorFilter = new PorterDuffColorFilter(
                Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent),
                PorterDuff.Mode.SRC_IN);
        mErrorColorFilter = new PorterDuffColorFilter(
                context.getColor(R.color.battery_icon_color_error), PorterDuff.Mode.SRC_IN);
        mForegroundColorFilter =new PorterDuffColorFilter(
                Utils.getColorAttrDefaultColor(context, android.R.attr.colorForeground),
                PorterDuff.Mode.SRC_IN);
        mThemedDrawable = new BatteryMeterDrawable(context, frameColor);
        mCircleDrawable = new CircleBatteryDrawable(context, frameColor);
        mFullCircleDrawable = new FullCircleBatteryDrawable(context, frameColor);
    }

    public void setDrawableStyle() {
        int style = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, 0);
        switch (style) {
            case 1:
            case 2:
                mCircleDrawable.setMeterStyle(style);
                mCircleDrawable.setColorFilter(mAccentColorFilter);
                setImageDrawable(mCircleDrawable);
                break;
            case 3:
                mFullCircleDrawable.setColorFilter(mAccentColorFilter);
                setImageDrawable(mFullCircleDrawable);
                break;
            default:
                mThemedDrawable.setColorFilter(mAccentColorFilter);
                setImageDrawable(mThemedDrawable);
        }
        if (mIconStyle != style) {
            mIconStyle = style;
            postInvalidate();
        }
    }

    public void setBatteryLevel(int level) {
        mCircleDrawable.setBatteryLevel(level);
        mFullCircleDrawable.setBatteryLevel(level);
        mThemedDrawable.setBatteryLevel(level);
        updateColorFilter();
    }

    public int getBatteryLevel() {
        return mThemedDrawable.getBatteryLevel();
    }

    public void setPowerSave(boolean powerSave) {
        mCircleDrawable.setPowerSaveEnabled(powerSave);
        mFullCircleDrawable.setPowerSaveEnabled(powerSave);
        mThemedDrawable.setPowerSaveEnabled(powerSave);
        updateColorFilter();
    }

    public boolean getPowerSave() {
        return mThemedDrawable.getPowerSaveEnabled();
    }

    public void setCharging(boolean charging) {
        mCircleDrawable.setCharging(charging);
        mFullCircleDrawable.setCharging(charging);
        mThemedDrawable.setCharging(charging);
        postInvalidate();
    }

    public boolean getCharging() {
        return mThemedDrawable.getCharging();
    }

    private int getCriticalLevel() {
        return mThemedDrawable.getCriticalLevel();
    }

    private void updateColorFilter() {
        final boolean powerSaveEnabled = getPowerSave();
        final int level = getBatteryLevel();
        if (powerSaveEnabled) {
            mCircleDrawable.setColorFilter(mForegroundColorFilter);
            mFullCircleDrawable.setColorFilter(mForegroundColorFilter);
            mThemedDrawable.setColorFilter(mForegroundColorFilter);
        } else if (level < getCriticalLevel()) {
            mCircleDrawable.setColorFilter(mErrorColorFilter);
            mFullCircleDrawable.setColorFilter(mErrorColorFilter);
            mThemedDrawable.setColorFilter(mErrorColorFilter);
        } else {
            mCircleDrawable.setColorFilter(mAccentColorFilter);
            mFullCircleDrawable.setColorFilter(mAccentColorFilter);
            mThemedDrawable.setColorFilter(mAccentColorFilter);
        }
    }

    public static class BatteryMeterDrawable extends ThemedBatteryDrawable {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;

        public BatteryMeterDrawable(Context context, int frameColor) {
            super(context, frameColor);

            mIntrinsicWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_height);
        }

        public BatteryMeterDrawable(Context context, int frameColor, int width, int height) {
            super(context, frameColor);

            mIntrinsicWidth = width;
            mIntrinsicHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }
    }

    public static class CircleBatteryMeterDrawable extends CircleBatteryDrawable {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;

        public CircleBatteryMeterDrawable(Context context, int frameColor) {
            super(context, frameColor);

            mIntrinsicWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_height);
        }

        public CircleBatteryMeterDrawable(Context context, int frameColor, int width, int height) {
            super(context, frameColor);

            mIntrinsicWidth = width;
            mIntrinsicHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }
    }

    public static class FullCircleBatteryMeterDrawable extends FullCircleBatteryDrawable {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;

        public FullCircleBatteryMeterDrawable(Context context, int frameColor) {
            super(context, frameColor);

            mIntrinsicWidth = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_width);
            mIntrinsicHeight = context.getResources()
                    .getDimensionPixelSize(R.dimen.battery_meter_height);
        }

        public FullCircleBatteryMeterDrawable(Context context, int frameColor, int width, int height) {
            super(context, frameColor);

            mIntrinsicWidth = width;
            mIntrinsicHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }
    }
}
