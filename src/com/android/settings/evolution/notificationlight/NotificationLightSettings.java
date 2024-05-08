/*
 * Copyright (C) 2012 The CyanogenMod Project
 *               2017-2022 The LineageOS Project
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

package com.android.settings.evolution.notificationlight;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.evolution.notification.LightsCapabilities;
import com.evolution.settings.preference.CustomDialogPreference;
import com.evolution.settings.preference.PackageListAdapter;
import com.evolution.settings.preference.PackageListAdapter.PackageItem;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.evolution.settings.preference.SystemSettingSwitchPreference;
import com.evolution.settings.preference.SystemSettingMainSwitchPreference;
import com.android.internal.util.evolution.ColorUtils;

public class NotificationLightSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, ApplicationLightPreference.ItemLongClickListener {
    private static final String TAG = "NotificationLightSettings";

    private static final String KEY_NOTIFICATION_LIGHTS = "notification_lights";
    private static final String NOTIFICATION_LIGHT_PULSE =
            Settings.System.NOTIFICATION_LIGHT_PULSE;
    private static final String NOTIFICATION_LIGHT_COLOR_AUTO =
            Settings.System.NOTIFICATION_LIGHT_COLOR_AUTO;
    private static final String NOTIFICATION_LIGHT_SCREEN_ON =
            Settings.System.NOTIFICATION_LIGHT_SCREEN_ON;
    private static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE =
            Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE;
    private static final String NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL =
            Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL;
    private static final String NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_ZEN =
            Settings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL_ZEN;

    private static final String ADVANCED_SECTION = "advanced_section";
    private static final String APPLICATION_SECTION = "applications_list";
    private static final String BRIGHTNESS_SECTION = "brightness_section";
    private static final String GENERAL_SECTION = "general_section";
    private static final String PHONE_SECTION = "phone_list";

    private static final String DEFAULT_PREF = "default";
    private static final String MISSED_CALL_PREF = "missed_call";
    private static final String VOICEMAIL_PREF = "voicemail";
    private static final String ADD_APPS = "custom_apps_add";

    public static final int ACTION_TEST = 0;
    public static final int ACTION_DELETE = 1;
    private static final int DIALOG_APPS = 0;

    private int mDefaultColor;
    private int mDefaultLedOn;
    private int mDefaultLedOff;
    private PackageManager mPackageManager;
    private PreferenceGroup mApplicationPrefList;
    private SystemSettingMainSwitchPreference mEnabledPref;
    private SystemSettingSwitchPreference mCustomEnabledPref;
    private SystemSettingSwitchPreference mScreenOnLightsPref;
    private SystemSettingSwitchPreference mAutoGenerateColors;
    private ApplicationLightPreference mDefaultPref;
    private ApplicationLightPreference mCallPref;
    private ApplicationLightPreference mVoicemailPref;
    private PackageListAdapter mPackageAdapter;
    private String mPackageList;
    private Map<String, Package> mPackages;
    // Supports rgb color control
    private boolean mMultiColorLed;
    // Supports adjustable pulse
    private boolean mLedCanPulse;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getContext();

        addPreferencesFromResource(R.xml.notification_light_settings);
        getActivity().getActionBar().setTitle(R.string.notification_light_title);

        PreferenceScreen prefSet = getPreferenceScreen();
        Resources resources = getResources();

        PreferenceGroup mAdvancedPrefs = prefSet.findPreference(ADVANCED_SECTION);
        PreferenceGroup mGeneralPrefs = prefSet.findPreference(GENERAL_SECTION);

        // Get the system defined default notification color
        mDefaultColor = resources.getColor(
                com.android.internal.R.color.config_defaultNotificationColor, null);

        mDefaultLedOn = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOn);
        mDefaultLedOff = resources.getInteger(
                com.android.internal.R.integer.config_defaultNotificationLedOff);

        // liblights supports brightness control
        final boolean halAdjustableBrightness = LightsCapabilities.supports(
                context, LightsCapabilities.LIGHTS_ADJUSTABLE_NOTIFICATION_LED_BRIGHTNESS);
        mLedCanPulse = LightsCapabilities.supports(
                context, LightsCapabilities.LIGHTS_PULSATING_LED);
        mMultiColorLed = LightsCapabilities.supports(
                context, LightsCapabilities.LIGHTS_RGB_NOTIFICATION_LED);

        mEnabledPref = findPreference(Settings.System.NOTIFICATION_LIGHT_PULSE);
        mEnabledPref.setOnPreferenceChangeListener(this);

        mDefaultPref = findPreference(DEFAULT_PREF);

        mAutoGenerateColors = findPreference(Settings.System.NOTIFICATION_LIGHT_COLOR_AUTO);

        // Advanced light settings
        mScreenOnLightsPref =
                findPreference(Settings.System.NOTIFICATION_LIGHT_SCREEN_ON);
        mScreenOnLightsPref.setOnPreferenceChangeListener(this);
        mCustomEnabledPref =
                findPreference(Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE);
        if (!mMultiColorLed && !halAdjustableBrightness) {
            removePreference(BRIGHTNESS_SECTION);
        }
        if (!mLedCanPulse && !mMultiColorLed) {
            mGeneralPrefs.removePreference(mDefaultPref);
            mAdvancedPrefs.removePreference(mCustomEnabledPref);
        } else {
            mCustomEnabledPref.setOnPreferenceChangeListener(this);
            mDefaultPref.setOnPreferenceChangeListener(this);
            mDefaultPref.setDefaultValues(mDefaultColor, mDefaultLedOn, mDefaultLedOff);
        }

        // Missed call and Voicemail preferences should only show on devices with voice capabilities
        TelephonyManager tm = getActivity().getSystemService(TelephonyManager.class);
        if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE
                || (!mLedCanPulse && !mMultiColorLed)) {
            removePreference(PHONE_SECTION);
        } else {
            mCallPref = findPreference(MISSED_CALL_PREF);
            mCallPref.setOnPreferenceChangeListener(this);
            mCallPref.setDefaultValues(mDefaultColor, mDefaultLedOn, mDefaultLedOff);

            mVoicemailPref = findPreference(VOICEMAIL_PREF);
            mVoicemailPref.setOnPreferenceChangeListener(this);
            mVoicemailPref.setDefaultValues(mDefaultColor, mDefaultLedOn, mDefaultLedOff);
        }

        if (!mLedCanPulse && !mMultiColorLed) {
            removePreference(APPLICATION_SECTION);
        } else {
            mApplicationPrefList = findPreference(APPLICATION_SECTION);
            mApplicationPrefList.setOrderingAsAdded(false);

            // Get launch-able applications
            mPackageManager = getActivity().getPackageManager();
            mPackageAdapter = new PackageListAdapter(getActivity());

            mPackages = new HashMap<>();

            Preference addPreference = prefSet.findPreference(ADD_APPS);
            addPreference.setOnPreferenceClickListener(preference -> {
                showDialog(DIALOG_APPS);
                return true;
            });
        }

        // Get launch-able applications
        mPackageManager = getActivity().getPackageManager();
        mPackageAdapter = new PackageListAdapter(getActivity());

        mPackages = new HashMap<String, Package>();

        if (!mMultiColorLed) {
            resetColors();
            mGeneralPrefs.removePreference(mAutoGenerateColors);
        } else {
            mAutoGenerateColors.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDefault();
        refreshCustomApplicationPrefs();
        getActivity().invalidateOptionsMenu();
    }

    private void refreshDefault() {
        ContentResolver resolver = getActivity().getContentResolver();
        int color = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        int timeOn = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, mDefaultLedOn);
        int timeOff = Settings.System.getInt(resolver,
                Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, mDefaultLedOff);

        mDefaultPref.setAllValues(color, timeOn, timeOff);

        // Get Missed call and Voicemail values
        if (mCallPref != null) {
            int callColor = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
            int callTimeOn = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, mDefaultLedOn);
            int callTimeOff = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, mDefaultLedOff);

            mCallPref.setAllValues(callColor, callTimeOn, callTimeOff);
        }

        if (mVoicemailPref != null) {
            int vmailColor = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);
            int vmailTimeOn = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, mDefaultLedOn);
            int vmailTimeOff = Settings.System.getInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, mDefaultLedOff);

            mVoicemailPref.setAllValues(vmailColor, vmailTimeOn, vmailTimeOff);
        }

        if (mLedCanPulse || mMultiColorLed) {
            mApplicationPrefList = findPreference(APPLICATION_SECTION);
            mApplicationPrefList.setOrderingAsAdded(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setChildrenStarted(getPreferenceScreen(), true);
    }

    @Override
    public void onStop() {
        super.onStop();
        setChildrenStarted(getPreferenceScreen(), false);
    }

    private void setChildrenStarted(PreferenceGroup group, boolean started) {
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof ApplicationLightPreference) {
                ApplicationLightPreference ap = (ApplicationLightPreference) pref;
                if (started) {
                    ap.onStart();
                } else {
                    ap.onStop();
                }
            } else if (pref instanceof PreferenceGroup) {
                setChildrenStarted((PreferenceGroup) pref, started);
            }
        }
    }

    private void refreshCustomApplicationPrefs() {
        Context context = getActivity();

        if (!parsePackageList()) {
            maybeDisplayApplicationHint(context);
            return;
        }

        // Add the Application Preferences
        if (mApplicationPrefList != null) {
            for (int i = 0; i < mApplicationPrefList.getPreferenceCount();) {
                Preference pref = mApplicationPrefList.getPreference(i);
                if (ADD_APPS.equals(pref.getKey())) {
                    i++;
                    continue;
                }

                mApplicationPrefList.removePreference(pref);
            }

            for (Package pkg : mPackages.values()) {
                try {
                    PackageInfo info = mPackageManager.getPackageInfo(pkg.name,
                            PackageManager.GET_META_DATA);
                    ApplicationLightPreference pref =
                            new ApplicationLightPreference(context, null,
                                    pkg.color, pkg.timeon, pkg.timeoff);

                    pref.setKey(pkg.name);
                    pref.setTitle(info.applicationInfo.loadLabel(mPackageManager));
                    pref.setIcon(info.applicationInfo.loadIcon(mPackageManager));
                    pref.setPersistent(false);
                    pref.setOnPreferenceChangeListener(this);
                    pref.setOnLongClickListener(this);
                    mApplicationPrefList.addPreference(pref);
                } catch (NameNotFoundException e) {
                    // Do nothing
                }
            }

            maybeDisplayApplicationHint(context);
            mPackageAdapter.setExcludedPackages(new HashSet(mPackages.keySet()));
        }
    }

    private void maybeDisplayApplicationHint(Context context) {
        /* Display a pref explaining how to add apps */
        if (mApplicationPrefList != null && mApplicationPrefList.getPreferenceCount() == 1) {
            String summary = getResources().getString(
                    R.string.notification_light_add_apps_empty_summary);
            String useCustom = getResources().getString(
                    R.string.notification_light_use_custom);
            Preference pref = new Preference(context);
            pref.setSummary(String.format(summary, useCustom));
            pref.setEnabled(false);
            mApplicationPrefList.addPreference(pref);
        }
    }

    private int getInitialColorForPackage(String packageName) {
        boolean autoColor = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_COLOR_AUTO, mMultiColorLed ? 1 : 0) == 1;
        int color = mDefaultColor;
        if (autoColor) {
            try {
                Drawable icon = mPackageManager.getApplicationIcon(packageName);
                color = ColorUtils.generateAlertColorFromDrawable(icon);
            } catch (NameNotFoundException e) {
                // shouldn't happen, but just return default
            }
        }
        return color;
    }

    private void addCustomApplicationPref(String packageName) {
        Package pkg = mPackages.get(packageName);
        if (pkg == null) {
            int color = getInitialColorForPackage(packageName);
            pkg = new Package(packageName, color, mDefaultLedOn, mDefaultLedOff);
            mPackages.put(packageName, pkg);
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private void removeCustomApplicationPref(String packageName) {
        if (mPackages.remove(packageName) != null) {
            savePackageList(false);
            refreshCustomApplicationPrefs();
        }
    }

    private boolean parsePackageList() {
        final String baseString = Settings.System.getString(
                getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

        if (TextUtils.equals(mPackageList, baseString)) {
            return false;
        }

        mPackageList = baseString;
        mPackages.clear();

        if (baseString != null) {
            final String[] array = TextUtils.split(baseString, "\\|");
            for (String item : array) {
                if (TextUtils.isEmpty(item)) {
                    continue;
                }
                Package pkg = Package.fromString(item);
                if (pkg != null) {
                    mPackages.put(pkg.name, pkg);
                }
            }
        }

        mPackageAdapter.setExcludedPackages(new HashSet<String>(mPackages.keySet()));

        return true;
    }

    private void savePackageList(boolean preferencesUpdated) {
        List<String> settings = new ArrayList();
        for (Package app : mPackages.values()) {
            settings.add(app.toString());
        }
        final String value = TextUtils.join("|", settings);
        if (preferencesUpdated) {
            mPackageList = value;
        }
        Settings.System.putString(getActivity().getContentResolver(),
            Settings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, value);
    }

    /**
     * Updates the default or package specific notification settings.
     *
     * @param packageName Package name of application specific settings to update
     */
    protected void updateValues(String packageName, Integer color, Integer timeon,
                                Integer timeoff) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (packageName.equals(DEFAULT_PREF)) {
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, color);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, timeon);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(MISSED_CALL_PREF)) {
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, color);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_ON, timeon);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_LED_OFF, timeoff);
            refreshDefault();
            return;
        } else if (packageName.equals(VOICEMAIL_PREF)) {
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, color);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_ON, timeon);
            Settings.System.putInt(resolver,
                    Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_LED_OFF, timeoff);
            refreshDefault();
            return;
        }

        // Find the custom package and sets its new values
        Package app = mPackages.get(packageName);
        if (app != null) {
            app.color = color;
            app.timeon = timeon;
            app.timeoff = timeoff;
            savePackageList(true);
        }
    }

    protected void resetColors() {
        ContentResolver resolver = getActivity().getContentResolver();

        // Reset to the framework default colors
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, mDefaultColor);
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_CALL_COLOR, mDefaultColor);
        Settings.System.putInt(resolver, Settings.System.NOTIFICATION_LIGHT_PULSE_VMAIL_COLOR, mDefaultColor);

        refreshDefault();
    }

    public boolean onItemLongClick(final String key) {
        if (mApplicationPrefList.findPreference(key) == null) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        removeCustomApplicationPref(key)
                )
                .setNegativeButton(android.R.string.cancel, null);

        builder.show();
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mEnabledPref || preference == mCustomEnabledPref ||
                preference == mScreenOnLightsPref ||
                preference == mAutoGenerateColors) {
            getActivity().invalidateOptionsMenu();
        } else {
            ApplicationLightPreference lightPref = (ApplicationLightPreference) preference;
            updateValues(lightPref.getKey(), lightPref.getColor(),
                    lightPref.getOnValue(), lightPref.getOffValue());
        }

        return true;
    }

    /**
     * Utility classes and supporting methods
     */
    @Override
    public Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Dialog dialog;
        switch (id) {
            case DIALOG_APPS:
                Resources res = getResources();
                int paddingTop = res.getDimensionPixelOffset(R.dimen.package_list_padding_top);

                final ListView list = new ListView(getActivity());
                list.setAdapter(mPackageAdapter);
                list.setDivider(null);
                list.setPadding(0, paddingTop, 0, 0);

                builder.setTitle(R.string.choose_app);
                builder.setView(list);
                dialog = builder.create();

                list.setOnItemClickListener((parent, view, position, id1) -> {
                    // Add empty application definition, the user will be able to edit it later
                    PackageItem info = (PackageItem) parent.getItemAtPosition(position);
                    addCustomApplicationPref(info.packageName);
                    dialog.cancel();
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_APPS) {
            return MetricsEvent.EVO_SETTINGS;
        }
        return 0;
    }

    /**
     * Application class
     */
    private static class Package {
        public String name;
        public Integer color;
        public Integer timeon;
        public Integer timeoff;

        /**
         * Stores all the application values in one call
         */
        public Package(String name, Integer color, Integer timeon, Integer timeoff) {
            this.name = name;
            this.color = color;
            this.timeon = timeon;
            this.timeoff = timeoff;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(name);
            builder.append("=");
            builder.append(color);
            builder.append(";");
            builder.append(timeon);
            builder.append(";");
            builder.append(timeoff);
            return builder.toString();
        }

        public static Package fromString(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }
            String[] app = value.split("=", -1);
            if (app.length != 2)
                return null;

            String[] values = app[1].split(";", -1);
            if (values.length != 3)
                return null;

            try {
                return new Package(app[0], Integer.parseInt(values[0]), Integer
                        .parseInt(values[1]), Integer.parseInt(values[2]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

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
}
