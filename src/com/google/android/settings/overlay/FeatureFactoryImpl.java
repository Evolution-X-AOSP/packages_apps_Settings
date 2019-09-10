package com.google.android.settings.overlay;

import com.android.settings.accounts.AccountFeatureProvider;
import com.google.android.settings.accounts.AccountFeatureProviderGoogleImpl;

public final class FeatureFactoryImpl extends com.android.settings.overlay.FeatureFactoryImpl {
    private AccountFeatureProvider mAccountFeatureProvider;

    @Override
    public AccountFeatureProvider getAccountFeatureProvider() {
        if (mAccountFeatureProvider == null) {
            mAccountFeatureProvider = new AccountFeatureProviderGoogleImpl();
        }
        return mAccountFeatureProvider;
    }
}
