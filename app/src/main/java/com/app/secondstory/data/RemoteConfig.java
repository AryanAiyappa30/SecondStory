package com.app.secondstory.data;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.app.secondstory.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class RemoteConfig {

    // firebase remote config key property
    private static final String PUBLISHER_KEY = "publisher_id";
    private static final String PRIVACY_POLICY_URL = "privacy_policy_url";

    private static final String INTERSTITIAL_KEY = "interstitial_ad_unit_id";
    private static final String BANNER_KEY = "banner_ad_unit_id";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    public RemoteConfig() {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .build();

        // Get Remote Config instance.
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

    }

    public void fetchData(Activity activity) {
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(activity, new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    boolean updated = task.getResult();
                    Log.d("RemoteConfig", "Config params updated: " + updated);
                } else {
                    Log.d("RemoteConfig", "Fetch failed");
                }
            }
        });
    }

    public String getPublisherId() {
        return mFirebaseRemoteConfig.getString(PUBLISHER_KEY);
    }

    public String getBannerUnitId() {
        return mFirebaseRemoteConfig.getString(BANNER_KEY);
    }

    public String getInterstitialUnitId() {
        return mFirebaseRemoteConfig.getString(INTERSTITIAL_KEY);
    }

    public String getPrivacyPolicyUrl() {
        return mFirebaseRemoteConfig.getString(PRIVACY_POLICY_URL);
    }
}
