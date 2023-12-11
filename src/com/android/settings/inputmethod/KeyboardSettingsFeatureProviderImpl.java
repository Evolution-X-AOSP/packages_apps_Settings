/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.preference.PreferenceScreen;

/**
 * Provider implementation for keyboard settings related features.
 */
public class KeyboardSettingsFeatureProviderImpl implements KeyboardSettingsFeatureProvider {

    @Override
    public boolean supportsFirmwareUpdate() {
        return false;
    }

    @Override
    public boolean addFirmwareUpdateCategory(Context context, PreferenceScreen screen) {
        return false;
    }

    @Override
    public Drawable getActionKeyIcon(Context context) {
        return null;
    };
}
