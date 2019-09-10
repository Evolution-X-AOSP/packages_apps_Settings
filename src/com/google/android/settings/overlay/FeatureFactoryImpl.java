package com.google.android.settings.overlay;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.google.android.settings.accounts.AccountFeatureProviderGoogleImpl;
import com.google.android.settings.applications.ApplicationFeatureProviderGoogleImpl;
import com.google.android.settings.dashboard.suggestions.SuggestionFeatureProviderGoogleImpl;
import com.google.android.settings.search.SearchFeatureProviderGoogleImpl;

public final class FeatureFactoryImpl extends com.android.settings.overlay.FeatureFactoryImpl {
    private AccountFeatureProvider mAccountFeatureProvider;
    private ApplicationFeatureProvider mApplicationFeatureProvider;
    private SearchFeatureProvider mSearchFeatureProvider;
    private SuggestionFeatureProvider mSuggestionFeatureProvider;

    @Override
    public AccountFeatureProvider getAccountFeatureProvider() {
        if (mAccountFeatureProvider == null) {
            mAccountFeatureProvider = new AccountFeatureProviderGoogleImpl();
        }
        return mAccountFeatureProvider;
    }

    @Override
    public SearchFeatureProvider getSearchFeatureProvider() {
        if (mSearchFeatureProvider == null) {
            mSearchFeatureProvider = new SearchFeatureProviderGoogleImpl();
        }
        return mSearchFeatureProvider;
    }

    @Override
    public SuggestionFeatureProvider getSuggestionFeatureProvider(Context context) {
        if (mSuggestionFeatureProvider == null) {
            mSuggestionFeatureProvider = new SuggestionFeatureProviderGoogleImpl(context.getApplicationContext());
        }
        return mSuggestionFeatureProvider;
    }

    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider(Context context) {
        if (mApplicationFeatureProvider == null) {
            Context applicationContext = context.getApplicationContext();
            DevicePolicyManager dpManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            mApplicationFeatureProvider = new ApplicationFeatureProviderGoogleImpl(applicationContext,
                    applicationContext.getPackageManager(), AppGlobals.getPackageManager(), dpManager);
        }
        return mApplicationFeatureProvider;
    }
}
