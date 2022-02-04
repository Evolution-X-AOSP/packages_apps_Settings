/*
 * Copyright (C) 2022 AOSP-Krypton Project
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

package com.android.settings.security.applock

import android.app.AppLockManager
import android.content.Context

import androidx.preference.ListPreference
import androidx.preference.Preference

import com.evolution.settings.EvolutionBasePreferenceController

class AppLockTimeoutPreferenceController(
    context: Context,
    key: String,
) : EvolutionBasePreferenceController(context, key),
        Preference.OnPreferenceChangeListener {

    private val appLockManager = context.getSystemService(AppLockManager::class.java)

    override fun getAvailabilityStatus() = AVAILABLE

    override fun updateState(preference: Preference) {
        val timeout = appLockManager.getTimeout()
        (preference as ListPreference).value = if (timeout == -1L) null else timeout.toString()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        appLockManager.setTimeout((newValue as String).toLong())
        return true
    }
}
