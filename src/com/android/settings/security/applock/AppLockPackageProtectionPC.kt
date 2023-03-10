/*
 * Copyright (C) 2022 FlamingoOS Project
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

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val KEY = "main_switch"

class AppLockPackageProtectionPC(
    context: Context,
    private val packageName: String,
    private val coroutineScope: CoroutineScope
) : AppLockTogglePreferenceController(context, KEY) {

    private val appLockManager = context.getSystemService(AppLockManager::class.java)
    private var isProtected = false
    private var preference: Preference? = null

    init {
        coroutineScope.launch {
            isProtected = withContext(Dispatchers.Default) {
                appLockManager.packageData.find {
                    it.packageName == packageName
                }?.shouldProtectApp == true
            }
            preference?.let {
                updateState(it)
            }
        }
    }

    override fun getAvailabilityStatus() = AVAILABLE

    override fun isChecked() = isProtected

    override fun setChecked(checked: Boolean): Boolean {
        if (isProtected == checked) return false
        isProtected = checked
        coroutineScope.launch(Dispatchers.Default) {
            appLockManager.setShouldProtectApp(packageName, isProtected)
        }
        return true
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preference = screen.findPreference(preferenceKey)
    }
}