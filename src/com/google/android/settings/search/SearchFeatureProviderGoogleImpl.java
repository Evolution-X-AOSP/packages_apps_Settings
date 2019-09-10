package com.google.android.settings.search;

import android.content.Context;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.google.android.settings.external.SignatureVerifier;

public class SearchFeatureProviderGoogleImpl extends SearchFeatureProviderImpl {
    @Override
    public boolean isSignatureWhitelisted(Context context, String str) {
        return SignatureVerifier.isPackageWhitelisted(context, str);
    }
}
