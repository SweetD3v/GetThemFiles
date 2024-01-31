package com.filerecover.photorecovery.allrecover.restore.interfaces;

import com.google.android.gms.ads.AdView;

public interface AdBannerEventListener {
    void onAdLoaded(AdView adView);

    void onAdClosed();

    void onLoadError(String errorCode);
}