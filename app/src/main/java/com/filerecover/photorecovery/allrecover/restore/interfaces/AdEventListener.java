package com.filerecover.photorecovery.allrecover.restore.interfaces;

import com.google.android.gms.ads.nativead.NativeAd;

public interface AdEventListener {
    void onAdLoaded(NativeAd nativeAd);

    void onAdClosed();

    void onLoadError(String errorCode);
}