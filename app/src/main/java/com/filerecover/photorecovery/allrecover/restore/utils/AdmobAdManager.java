package com.filerecover.photorecovery.allrecover.restore.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.filerecover.photorecovery.allrecover.restore.R;
import com.filerecover.photorecovery.allrecover.restore.interfaces.AdEventListener;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

public class AdmobAdManager {

    private static AdmobAdManager singleton;
    public InterstitialAd interstitialAd;
    public boolean isAdLoad = false;
    public boolean isAdLoadProcessing = false;
    public boolean isAdLoadFailed = false;
    public NativeAd nativeAd;
    public static Context context;

    public static AdmobAdManager getInstance(Context context1) {
        if (singleton == null) {
            singleton = new AdmobAdManager();
        }
        context = context1;
        return singleton;
    }

    public void loadInterstitialAd(String interstitialAdID) {
        if (interstitialAd == null && !isAdLoadProcessing) {
            isAdLoadProcessing = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(context, interstitialAdID, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAds) {
                    isAdLoad = true;
                    isAdLoadFailed = false;
                    isAdLoadProcessing = false;
                    interstitialAd = interstitialAds;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    Log.e("TAG", "onAdFailedToLoad: " + loadAdError.getMessage());
                    isAdLoad = false;
                    isAdLoadFailed = true;
                    isAdLoadProcessing = false;
                }
            });
        }
    }

    public void showInterstitialAd(String interstitialAdID, OnAdClosedListener onAdClosedListener) {
        if (interstitialAd != null) {
            if (isAdLoad && !isAdLoadFailed && !isAdLoadProcessing) {
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        interstitialAd = null;
                        isAdLoad = false;
                        isAdLoadProcessing = false;
                        isAdLoadFailed = false;
                        if (onAdClosedListener != null) {
                            onAdClosedListener.onAdClosed();
                        }
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        isAdLoad = false;
                        isAdLoadProcessing = false;
                        isAdLoadFailed = false;
                        interstitialAd = null;
                        loadInterstitialAd(interstitialAdID);
                        if (onAdClosedListener != null) {
                            onAdClosedListener.onAdClosed();
                        }
                    }
                });
                interstitialAd.show((Activity) context);
            } else {
                if (onAdClosedListener != null) {
                    onAdClosedListener.onAdClosed();
                }
            }
        } else {
            if (onAdClosedListener != null) {
                onAdClosedListener.onAdClosed();
            }
        }
    }

//    public void LoadBanner(RelativeLayout adContainerView, String bannerAdID, final AdEventListener adEventListener) {
//        try {
//            if (ExtensionsKt.isOnline(context)) {
//                AdView adView = new AdView(context);
//                adView.setAdSize(AdSize.BANNER);
//                adView.setAdUnitId(bannerAdID);
//                AdRequest adRequest = new AdRequest.Builder().build();
//                adView.setAdListener(new AdListener() {
//                    @Override
//                    public void onAdLoaded() {
//                        super.onAdLoaded();
//                        if (adEventListener != null) {
//                            adEventListener.onAdLoaded(null);
//                        }
//                    }
//
//                    @Override
//                    public void onAdClosed() {
//                        super.onAdClosed();
//                        if (adEventListener != null) {
//                            adEventListener.onAdClosed();
//                        }
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(LoadAdError loadAdError) {
//                        super.onAdFailedToLoad(loadAdError);
//
//                        if (adEventListener != null) {
//                            adEventListener.onLoadError(loadAdError.getMessage());
//                        }
//                    }
//                });
//                adView.loadAd(adRequest);
//                adContainerView.addView(adView);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e(TAG, "LoadBanner: " + e.getMessage());
//        }
//
//    }

    public void loadAdaptiveBanner(FrameLayout adContainerView, @StringRes int bannerAdID, @Nullable final AdEventListener adEventListener) {
        if (ExtensionsKt.isOnline(context)) {
            try {
                AdView adView = new AdView(context);
                adView.setAdUnitId(context.getString(bannerAdID));
                if (adContainerView.getChildCount() > 0) adContainerView.removeAllViews();
                adContainerView.addView(adView);

                final AdSize adSize = getAdSize(adContainerView);
                adView.setAdSize(adSize);

                AdRequest adRequest = new AdRequest.Builder().build();

                adView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        if (adEventListener != null) {
                            adEventListener.onAdLoaded(null);
                        }
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        if (adEventListener != null) {
                            adEventListener.onAdClosed();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        if (adEventListener != null) {
                            adEventListener.onLoadError(loadAdError.getMessage());
                        }
                    }
                });
                adView.loadAd(adRequest);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("TAG", "LoadAdaptiveBanner: " + e.getMessage());
            }
        } else {
            if (adEventListener != null) {
                adEventListener.onLoadError("No internet");
            }
        }
    }

    public AdSize getAdSize(FrameLayout adContainerView) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = adContainerView.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    public void loadNativeAd(String nativeAdID, final AdEventListener adEventListener) {
        AdLoader.Builder builder = new AdLoader.Builder(context, nativeAdID);

        builder.forNativeAd(unifiedNativeAd -> {
            if (adEventListener != null) {
                nativeAd = unifiedNativeAd;
                adEventListener.onAdLoaded(unifiedNativeAd);
            }
        }).withAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                {
                    if (adEventListener != null) {
                        adEventListener.onLoadError(loadAdError.getMessage());
                    }
                }
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
            }
        });
        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);
        AdLoader adLoader = builder.build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public void populateUnifiedHalfNativeAdView(FrameLayout frameLayout, NativeAd nativeAd, boolean isLanguage) {
        if (ExtensionsKt.isOnline(context)) {
            LayoutInflater inflater = LayoutInflater.from(context);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.admob_native_medium, null);
            AppCompatTextView adText = adView.findViewById(R.id.adAttribute);
            AppCompatTextView button = adView.findViewById(R.id.callToAction);
            if (frameLayout != null) {
                frameLayout.removeAllViews();
                frameLayout.addView(adView);
                frameLayout.setVisibility(View.VISIBLE);
            }
            try {
                if (isLanguage) {
//                        adText.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorPrimary));
                    button.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorPrimary));
                } else {
//                        adText.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorPrimary));
                    button.setBackgroundTintList(context.getResources().getColorStateList(R.color.colorPrimary));
                }

                adView.setHeadlineView(adView.findViewById(R.id.adTitle));
                adView.setBodyView(adView.findViewById(R.id.ad_body));
                adView.setIconView(adView.findViewById(R.id.adIcon));
                adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
                adView.setCallToActionView(adView.findViewById(R.id.callToAction));

                ((AppCompatTextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

                if (nativeAd.getBody() == null) {
                    adView.getBodyView().setVisibility(View.INVISIBLE);
                } else {
                    adView.getBodyView().setVisibility(View.VISIBLE);
                    ((AppCompatTextView) adView.getBodyView()).setText(nativeAd.getBody());
                }

                if (nativeAd.getCallToAction() == null) {
                    adView.getCallToActionView().setVisibility(View.INVISIBLE);
                } else {
                    adView.getCallToActionView().setVisibility(View.VISIBLE);
                    ((AppCompatButton) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
                }
                if (nativeAd.getIcon() == null) {
                    adView.getIconView().setVisibility(View.GONE);
                } else {
                    ((AppCompatImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                    adView.getIconView().setVisibility(View.VISIBLE);
                }

                if (nativeAd.getAdvertiser() == null) {
                    adView.getAdvertiserView().setVisibility(View.GONE);
                } else {
                    ((AppCompatTextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                    adView.getAdvertiserView().setVisibility(View.VISIBLE);
                }
                VideoController vc = nativeAd.getMediaContent().getVideoController();
                vc.mute(true);
                if (vc.hasVideoContent()) {
                    vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                        @Override
                        public void onVideoEnd() {
                            super.onVideoEnd();
                        }
                    });
                }
                adView.setNativeAd(nativeAd);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("TAG", "populateUnifiedNativeAdView Exception: " + e.getMessage());
            }
        }
    }

    public interface OnAdClosedListener {
        void onAdClosed();
    }
}
