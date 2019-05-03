/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2021 The LineageOS Project
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
package com.android.settings.livedisplay;

import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_ANTI_FLICKER;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_CABC;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_COLOR_ADJUSTMENT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_COLOR_ENHANCEMENT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_DISPLAY_MODES;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_PICTURE_ADJUSTMENT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.FEATURE_READING_ENHANCEMENT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_AUTO;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_DAY;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_NIGHT;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_OFF;
import static com.android.internal.lineage.hardware.LiveDisplayManager.MODE_OUTDOOR;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.lineage.hardware.DisplayMode;
import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.lineage.hardware.LiveDisplayConfig;
import com.android.internal.lineage.hardware.LiveDisplayManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.evolution.settings.preference.CustomDialogPreference;
import com.evolution.settings.preference.SystemSettingSwitchPreference;
import com.android.settings.custom.utils.ResourceUtils;
import com.android.settings.custom.utils.SettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class LiveDisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, SettingsHelper.OnSettingsChangeListener {

    private static final String TAG = "LiveDisplay";

    private static final String KEY_SCREEN_LIVE_DISPLAY = "livedisplay";
    private static final String KEY_CATEGORY_ADVANCED = "advanced";

    private static final String KEY_LIVE_DISPLAY = "live_display";
    private static final String KEY_LIVE_DISPLAY_ANTI_FLICKER = "display_anti_flicker";
    private static final String KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE =
            "display_auto_outdoor_mode";
    private static final String KEY_LIVE_DISPLAY_READING_ENHANCEMENT = "display_reading_mode";
    private static final String KEY_LIVE_DISPLAY_LOW_POWER = "display_low_power";
    private static final String KEY_LIVE_DISPLAY_COLOR_ENHANCE = "display_color_enhance";
    private static final String KEY_LIVE_DISPLAY_TEMPERATURE = "live_display_color_temperature";

    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_PICTURE_ADJUSTMENT = "picture_adjustment";

    private static final String KEY_LIVE_DISPLAY_COLOR_PROFILE = "live_display_color_profile";

    private static final String KEY_COLOR_MODE_PREVIEW = "color_mode_preview";

    private static final String COLOR_PROFILE_TITLE =
            KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_title";

    private static final String COLOR_PROFILE_SUMMARY =
            KEY_LIVE_DISPLAY_COLOR_PROFILE + "_%s_summary";

    private final Uri DISPLAY_TEMPERATURE_DAY_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_DAY);
    private final Uri DISPLAY_TEMPERATURE_NIGHT_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_NIGHT);
    private final Uri DISPLAY_TEMPERATURE_MODE_URI =
            Settings.System.getUriFor(Settings.System.DISPLAY_TEMPERATURE_MODE);

    private ListPreference mLiveDisplay;

    private SystemSettingSwitchPreference mAntiFlicker;
    private SystemSettingSwitchPreference mColorEnhancement;
    private SystemSettingSwitchPreference mLowPower;
    private SystemSettingSwitchPreference mOutdoorMode;
    private SystemSettingSwitchPreference mReadingMode;

    private PictureAdjustment mPictureAdjustment;
    private DisplayTemperature mDisplayTemperature;
    private DisplayColor mDisplayColor;

    private ListPreference mColorProfile;
    private String[] mColorProfileSummaries;

    private String[] mModeEntries;
    private String[] mModeValues;
    private String[] mModeSummaries;

    private boolean mHasDisplayModes = false;

    private LiveDisplayManager mLiveDisplayManager;
    private LiveDisplayConfig mConfig;

    private LineageHardwareManager mHardware;

    static final String PAGE_VIEWER_SELECTION_INDEX = "page_viewer_selection_index";

    private static final int DOT_INDICATOR_SIZE = 12;
    private static final int DOT_INDICATOR_LEFT_PADDING = 6;
    private static final int DOT_INDICATOR_RIGHT_PADDING = 6;

    private View mViewArrowPrevious;
    private View mViewArrowNext;
    private ViewPager mViewPager;

    private ArrayList<View> mPageList;

    private ImageView[] mDotIndicators;
    private View[] mViewPagerImages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getResources();

        final boolean isNightDisplayAvailable =
                ColorDisplayManager.isNightDisplayAvailable(getContext());
        mHardware = LineageHardwareManager.getInstance(getActivity());
        mLiveDisplayManager = LiveDisplayManager.getInstance(getActivity());
        mConfig = mLiveDisplayManager.getConfig();

        addPreferencesFromResource(R.xml.livedisplay);

        PreferenceScreen liveDisplayPrefs = findPreference(KEY_SCREEN_LIVE_DISPLAY);
        PreferenceCategory advancedPrefs = findPreference(KEY_CATEGORY_ADVANCED);

        int adaptiveMode = mLiveDisplayManager.getMode();

        mLiveDisplay = findPreference(KEY_LIVE_DISPLAY);
        mLiveDisplay.setValue(String.valueOf(adaptiveMode));

        mModeEntries = res.getStringArray(
                com.android.internal.R.array.live_display_entries);
        mModeValues = res.getStringArray(
                com.android.internal.R.array.live_display_values);
        mModeSummaries = res.getStringArray(
                com.android.internal.R.array.live_display_summaries);

        int[] removeIdx = null;
        // Remove outdoor mode from lists if there is no support
        if (!mConfig.hasFeature(MODE_OUTDOOR)) {
            removeIdx = ArrayUtils.appendInt(removeIdx,
                    ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OUTDOOR)));
        } else if (isNightDisplayAvailable) {
            final int autoIdx = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_AUTO));
            mModeSummaries[autoIdx] = res.getString(R.string.live_display_outdoor_mode_summary);
        }

        // Remove night display on HWC2
        if (isNightDisplayAvailable) {
            removeIdx = ArrayUtils.appendInt(removeIdx,
                    ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_DAY)));
            removeIdx = ArrayUtils.appendInt(removeIdx,
                    ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_NIGHT)));
        }

        if (removeIdx != null) {
            String[] entriesTemp = new String[mModeEntries.length - removeIdx.length];
            String[] valuesTemp = new String[mModeValues.length - removeIdx.length];
            String[] summariesTemp = new String[mModeSummaries.length - removeIdx.length];
            int j = 0;
            for (int i = 0; i < mModeEntries.length; i++) {
                if (ArrayUtils.contains(removeIdx, i)) {
                    continue;
                }
                entriesTemp[j] = mModeEntries[i];
                valuesTemp[j] = mModeValues[i];
                summariesTemp[j] = mModeSummaries[i];
                j++;
            }
            mModeEntries = entriesTemp;
            mModeValues = valuesTemp;
            mModeSummaries = summariesTemp;
        }

        mLiveDisplay.setEntries(mModeEntries);
        mLiveDisplay.setEntryValues(mModeValues);
        mLiveDisplay.setOnPreferenceChangeListener(this);

        mDisplayTemperature = findPreference(KEY_LIVE_DISPLAY_TEMPERATURE);
        if (isNightDisplayAvailable) {
            if (!mConfig.hasFeature(MODE_OUTDOOR)) {
                liveDisplayPrefs.removePreference(mLiveDisplay);
            }
            liveDisplayPrefs.removePreference(mDisplayTemperature);
        }

        mColorProfile = findPreference(KEY_LIVE_DISPLAY_COLOR_PROFILE);
        if (liveDisplayPrefs != null && mColorProfile != null
                && (!mConfig.hasFeature(FEATURE_DISPLAY_MODES) || !updateDisplayModes())) {
            liveDisplayPrefs.removePreference(mColorProfile);
        } else {
            mHasDisplayModes = true;
            mColorProfile.setOnPreferenceChangeListener(this);
        }

        mOutdoorMode = findPreference(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
        if (liveDisplayPrefs != null && mOutdoorMode != null
                // MODE_AUTO implies automatic outdoor mode on HWC2
                && (isNightDisplayAvailable || !mConfig.hasFeature(MODE_OUTDOOR))) {
            liveDisplayPrefs.removePreference(mOutdoorMode);
            mOutdoorMode = null;
        }

        mReadingMode = findPreference(KEY_LIVE_DISPLAY_READING_ENHANCEMENT);
        if (liveDisplayPrefs != null && mReadingMode != null &&
                !mHardware.isSupported(LineageHardwareManager.FEATURE_READING_ENHANCEMENT)) {
            liveDisplayPrefs.removePreference(mReadingMode);
            mReadingMode = null;
        } else {
            mReadingMode.setOnPreferenceChangeListener(this);
        }

        mLowPower = findPreference(KEY_LIVE_DISPLAY_LOW_POWER);
        if (advancedPrefs != null && mLowPower != null
                && !mConfig.hasFeature(FEATURE_CABC)) {
            advancedPrefs.removePreference(mLowPower);
            mLowPower = null;
        }

        mColorEnhancement = findPreference(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
        if (advancedPrefs != null && mColorEnhancement != null
                && !mConfig.hasFeature(FEATURE_COLOR_ENHANCEMENT)) {
            advancedPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        mPictureAdjustment = findPreference(KEY_PICTURE_ADJUSTMENT);
        if (advancedPrefs != null && mPictureAdjustment != null &&
                !mConfig.hasFeature(FEATURE_PICTURE_ADJUSTMENT)) {
            advancedPrefs.removePreference(mPictureAdjustment);
            mPictureAdjustment = null;
        }

        mDisplayColor = findPreference(KEY_DISPLAY_COLOR);
        if (advancedPrefs != null && mDisplayColor != null &&
                !mConfig.hasFeature(FEATURE_COLOR_ADJUSTMENT)) {
            advancedPrefs.removePreference(mDisplayColor);
            mDisplayColor = null;
        }

        mAntiFlicker = findPreference(KEY_LIVE_DISPLAY_ANTI_FLICKER);
        if (liveDisplayPrefs != null && mAntiFlicker != null &&
                !mHardware.isSupported(LineageHardwareManager.FEATURE_ANTI_FLICKER)) {
            liveDisplayPrefs.removePreference(mAntiFlicker);
            mAntiFlicker = null;
        }

        LayoutPreference preview = findPreference(KEY_COLOR_MODE_PREVIEW);
        preview.setSelectable(false);
        addViewPager(preview);
        if (savedInstanceState != null) {
            final int selectedPosition = savedInstanceState.getInt(PAGE_VIEWER_SELECTION_INDEX);
            mViewPager.setCurrentItem(selectedPosition);
            updateIndicator(selectedPosition);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PAGE_VIEWER_SELECTION_INDEX, mViewPager.getCurrentItem());
    }

    public ArrayList<Integer> getViewPagerResource() {
        return new ArrayList<Integer>(
                Arrays.asList(
                        R.layout.color_mode_view1,
                        R.layout.color_mode_view2,
                        R.layout.color_mode_view3));
    }

    void addViewPager(LayoutPreference preview) {
        final ArrayList<Integer> tmpviewPagerList = getViewPagerResource();
        mViewPager = preview.findViewById(R.id.viewpager);

        mViewPagerImages = new View[3];
        for (int idx = 0; idx < tmpviewPagerList.size(); idx++) {
            mViewPagerImages[idx] =
                    getLayoutInflater().inflate(tmpviewPagerList.get(idx), null /* root */);
        }

        mPageList = new ArrayList<View>();
        mPageList.add(mViewPagerImages[0]);
        mPageList.add(mViewPagerImages[1]);
        mPageList.add(mViewPagerImages[2]);

        mViewPager.setAdapter(new ColorPagerAdapter(mPageList));

        mViewArrowPrevious = preview.findViewById(R.id.arrow_previous);
        mViewArrowPrevious.setOnClickListener(v -> {
            final int previousPos = mViewPager.getCurrentItem() - 1;
            mViewPager.setCurrentItem(previousPos, true);
        });

        mViewArrowNext = preview.findViewById(R.id.arrow_next);
        mViewArrowNext.setOnClickListener(v -> {
            final int nextPos = mViewPager.getCurrentItem() + 1;
            mViewPager.setCurrentItem(nextPos, true);
        });

        mViewPager.addOnPageChangeListener(createPageListener());

        final ViewGroup viewGroup = (ViewGroup) preview.findViewById(R.id.viewGroup);
        mDotIndicators = new ImageView[mPageList.size()];
        for (int i = 0; i < mPageList.size(); i++) {
            final ImageView imageView = new ImageView(getContext());
            final ViewGroup.MarginLayoutParams lp =
                    new ViewGroup.MarginLayoutParams(DOT_INDICATOR_SIZE, DOT_INDICATOR_SIZE);
            lp.setMargins(DOT_INDICATOR_LEFT_PADDING, 0, DOT_INDICATOR_RIGHT_PADDING, 0);
            imageView.setLayoutParams(lp);
            mDotIndicators[i] = imageView;

            viewGroup.addView(mDotIndicators[i]);
        }

        updateIndicator(mViewPager.getCurrentItem());
    }

    private ViewPager.OnPageChangeListener createPageListener() {
        return new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                    int position, float positionOffset, int positionOffsetPixels) {
                if (positionOffset != 0) {
                    for (int idx = 0; idx < mPageList.size(); idx++) {
                        mViewPagerImages[idx].setVisibility(View.VISIBLE);
                    }
                } else {
                    mViewPagerImages[position].setContentDescription(
                            getContext().getString(R.string.colors_viewpager_content_description));
                    updateIndicator(position);
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };
    }

    private void updateIndicator(int position) {
        for (int i = 0; i < mPageList.size(); i++) {
            if (position == i) {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_focused);

                mViewPagerImages[i].setVisibility(View.VISIBLE);
            } else {
                mDotIndicators[i].setBackgroundResource(
                        R.drawable.ic_color_page_indicator_unfocused);

                mViewPagerImages[i].setVisibility(View.INVISIBLE);
            }
        }

        if (position == 0) {
            mViewArrowPrevious.setVisibility(View.INVISIBLE);
            mViewArrowNext.setVisibility(View.VISIBLE);
        } else if (position == (mPageList.size() - 1)) {
            mViewArrowPrevious.setVisibility(View.VISIBLE);
            mViewArrowNext.setVisibility(View.INVISIBLE);
        } else {
            mViewArrowPrevious.setVisibility(View.VISIBLE);
            mViewArrowNext.setVisibility(View.VISIBLE);
        }
    }

    static class ColorPagerAdapter extends PagerAdapter {
        private final ArrayList<View> mPageViewList;

        ColorPagerAdapter(ArrayList<View> pageViewList) {
            mPageViewList = pageViewList;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (mPageViewList.get(position) != null) {
                container.removeView(mPageViewList.get(position));
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mPageViewList.get(position));
            return mPageViewList.get(position);
        }

        @Override
        public int getCount() {
            return mPageViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateModeSummary();
        updateTemperatureSummary();
        updateColorProfileSummary(null);
        updateReadingModeStatus();
        SettingsHelper.get(getActivity()).startWatching(this, DISPLAY_TEMPERATURE_DAY_URI,
                DISPLAY_TEMPERATURE_MODE_URI, DISPLAY_TEMPERATURE_NIGHT_URI);
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsHelper.get(getActivity()).stopWatching(this);
    }

    private boolean updateDisplayModes() {
        final DisplayMode[] modes = mHardware.getDisplayModes();
        if (modes == null || modes.length == 0) {
            return false;
        }

        final DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
        int curId = -1;
        String[] entries = new String[modes.length];
        String[] values = new String[modes.length];
        mColorProfileSummaries = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            values[i] = String.valueOf(modes[i].id);
            entries[i] = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_TITLE);

            // Populate summary
            String summary = ResourceUtils.getLocalizedString(
                    getResources(), modes[i].name, COLOR_PROFILE_SUMMARY);
            if (summary != null) {
                summary = String.format("%s - %s", entries[i], summary);
            }
            mColorProfileSummaries[i] = summary;

            if (cur != null && modes[i].id == cur.id) {
                curId = cur.id;
            }
        }
        mColorProfile.setEntries(entries);
        mColorProfile.setEntryValues(values);
        if (curId >= 0) {
            mColorProfile.setValue(String.valueOf(curId));
        }

        return true;
    }

    private void updateColorProfileSummary(String value) {
        if (!mHasDisplayModes) {
            return;
        }

        if (value == null) {
            DisplayMode cur = mHardware.getCurrentDisplayMode() != null
                    ? mHardware.getCurrentDisplayMode() : mHardware.getDefaultDisplayMode();
            if (cur != null && cur.id >= 0) {
                value = String.valueOf(cur.id);
            }
        }

        int idx = mColorProfile.findIndexOfValue(value);
        if (idx < 0) {
            Log.e(TAG, "No summary resource found for profile " + value);
            mColorProfile.setSummary(null);
            return;
        }

        mColorProfile.setValue(value);
        mColorProfile.setSummary(mColorProfileSummaries[idx]);
    }

    private void updateModeSummary() {

        int mode = mLiveDisplayManager.getMode();

        int index = ArrayUtils.indexOf(mModeValues, String.valueOf(mode));
        if (index < 0) {
            index = ArrayUtils.indexOf(mModeValues, String.valueOf(MODE_OFF));
        }

        mLiveDisplay.setSummary(mModeSummaries[index]);
        mLiveDisplay.setValue(String.valueOf(mode));

        if (mDisplayTemperature != null) {
            mDisplayTemperature.setEnabled(mode != MODE_OFF);
        }
        if (mOutdoorMode != null) {
            mOutdoorMode.setEnabled(mode != MODE_OFF);
        }
    }

    private void updateTemperatureSummary() {
        int day = mLiveDisplayManager.getDayColorTemperature();
        int night = mLiveDisplayManager.getNightColorTemperature();

        mDisplayTemperature.setSummary(getResources().getString(
                R.string.live_display_color_temperature_summary,
                mDisplayTemperature.roundUp(day),
                mDisplayTemperature.roundUp(night)));
    }

    private void updateReadingModeStatus() {
        if (mReadingMode != null) {
            mReadingMode.setChecked(
                    mHardware.get(LineageHardwareManager.FEATURE_READING_ENHANCEMENT));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mLiveDisplay) {
            mLiveDisplayManager.setMode(Integer.valueOf((String) objValue));
        } else if (preference == mColorProfile) {
            int id = Integer.valueOf((String) objValue);
            Log.i("LiveDisplay", "Setting mode: " + id);
            for (DisplayMode mode : mHardware.getDisplayModes()) {
                if (mode.id == id) {
                    mHardware.setDisplayMode(mode, true);
                    updateColorProfileSummary((String) objValue);
                    break;
                }
            }
        } else if (preference == mReadingMode) {
            mHardware.set(LineageHardwareManager.FEATURE_READING_ENHANCEMENT, (Boolean) objValue);
        }
        return true;
    }

    @Override
    public void onSettingsChanged(Uri uri) {
        updateModeSummary();
        updateTemperatureSummary();
        updateReadingModeStatus();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVO_SETTINGS;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final LiveDisplayConfig config = LiveDisplayManager.getInstance(context).getConfig();
                    final List<String> result = new ArrayList<String>();

                    if (!config.hasFeature(FEATURE_DISPLAY_MODES)) {
                        result.add(KEY_LIVE_DISPLAY_COLOR_PROFILE);
                    }
                    if (!config.hasFeature(MODE_OUTDOOR)) {
                        result.add(KEY_LIVE_DISPLAY_AUTO_OUTDOOR_MODE);
                    }
                    if (!config.hasFeature(FEATURE_COLOR_ENHANCEMENT)) {
                        result.add(KEY_LIVE_DISPLAY_COLOR_ENHANCE);
                    }
                    if (!config.hasFeature(FEATURE_CABC)) {
                        result.add(KEY_LIVE_DISPLAY_LOW_POWER);
                    }
                    if (!config.hasFeature(FEATURE_COLOR_ADJUSTMENT)) {
                        result.add(KEY_DISPLAY_COLOR);
                    }
                    if (!config.hasFeature(FEATURE_PICTURE_ADJUSTMENT)) {
                        result.add(KEY_PICTURE_ADJUSTMENT);
                    }
                    if (!config.hasFeature(FEATURE_READING_ENHANCEMENT)) {
                        result.add(KEY_LIVE_DISPLAY_READING_ENHANCEMENT);
                    }
                    if (ColorDisplayManager.isNightDisplayAvailable(context)) {
                        if (!config.hasFeature(MODE_OUTDOOR)) {
                            result.add(KEY_LIVE_DISPLAY);
                        }
                        result.add(KEY_LIVE_DISPLAY_TEMPERATURE);
                    }
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_enableLiveDisplay)) {
                        result.add(KEY_LIVE_DISPLAY_TEMPERATURE);
                        result.add(KEY_LIVE_DISPLAY);
                    }
                    if (!config.hasFeature(FEATURE_ANTI_FLICKER)) {
                        result.add(KEY_LIVE_DISPLAY_ANTI_FLICKER);
                    }

                    return result;
                }

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final LiveDisplayConfig config = LiveDisplayManager.getInstance(context).getConfig();
                    final List<String> result = new ArrayList<>();

                    // Add keywords for supported color profiles
                    if (config.hasFeature(FEATURE_DISPLAY_MODES)) {
                        DisplayMode[] modes = LineageHardwareManager.getInstance(context).getDisplayModes();
                        if (modes != null && modes.length > 0) {
                            for (DisplayMode mode : modes) {
                                result.add(ResourceUtils.getLocalizedString(
                                        context.getResources(), mode.name, COLOR_PROFILE_TITLE));
                            }
                        }
                    }
                    final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.entries = TextUtils.join(" ", result);
                    raw.key = KEY_LIVE_DISPLAY_COLOR_PROFILE;
                    raw.title = context.getString(R.string.live_display_color_profile_title);
                    raw.rank = 2;
                    return Collections.singletonList(raw);
                }
            };
}
