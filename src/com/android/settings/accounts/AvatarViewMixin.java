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

package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.UserManager;
import android.util.Log;
import android.widget.ImageView;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Avatar related work to the onStart method of registered observable classes
 * in {@link SettingsHomepageActivity}.
 */
public class AvatarViewMixin implements LifecycleObserver {
    private static final String TAG = "AvatarViewMixin";

    private final Context mContext;
    private final ImageView mAvatarView;
    private final MutableLiveData<Bitmap> mAvatarImage;

    private String mAccountName;

    /**
     * @return true if the avatar icon is supported.
     */
    public static boolean isAvatarSupported(Context context) {
        if (!context.getResources().getBoolean(R.bool.config_show_avatar_in_homepage)) {
            Log.d(TAG, "Feature disabled by config. Skipping");
            return false;
        }
        return true;
    }

    public AvatarViewMixin(SettingsHomepageActivity activity, ImageView avatarView) {
        mContext = activity.getApplicationContext();
        mAvatarView = avatarView;
        mAvatarView.setOnClickListener(v -> {
            final Intent intent = new Intent(mContext, Settings.UserSettingsActivity.class);
            activity.startActivity(intent);
        });

        mAvatarImage = new MutableLiveData<>();
        mAvatarImage.observe(activity, bitmap -> {
            avatarView.setImageBitmap(bitmap);
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (hasAccount()) {
            loadAccount();
        } else {
            mAccountName = null;
            mAvatarView.setImageResource(R.drawable.ic_account_circle_24dp);
        }
    }

    private boolean hasAccount() {
        final Account accounts[] = FeatureFactory.getFactory(
                mContext).getAccountFeatureProvider().getAccounts(mContext);
        return (accounts != null) && (accounts.length > 0);
    }

    private void loadAccount() {
        ThreadUtils.postOnBackgroundThread(() -> {
            final UserManager um = UserManager.get(mContext);
            final Bitmap bitmap = um.getUserIcon();
            mAccountName = um.getUserName();
            if (bitmap != null) {
                mAvatarImage.postValue(bitmap);
            }
        });
    }
}
