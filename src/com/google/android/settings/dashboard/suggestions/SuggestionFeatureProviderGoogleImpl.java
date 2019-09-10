package com.google.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings.Secure;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.overlay.FeatureFactory;

public class SuggestionFeatureProviderGoogleImpl extends SuggestionFeatureProviderImpl {
    public SuggestionFeatureProviderGoogleImpl(Context context) {
        super(context);
    }

    @Override
    public ComponentName getSuggestionServiceComponent() {
        return new ComponentName("com.google.android.settings.intelligence", "com.google.android.settings.intelligence.modules.suggestions.SuggestionService");
    }
}
