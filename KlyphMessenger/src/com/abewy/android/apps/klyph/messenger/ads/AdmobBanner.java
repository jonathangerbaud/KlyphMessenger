package com.abewy.android.apps.klyph.messenger.ads;

import android.app.Activity;
import android.database.sqlite.SQLiteDiskIOException;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewDatabase;
import com.abewy.android.ads.IBannerAd;
import com.abewy.android.ads.IBannerCallback;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class AdmobBanner implements IBannerAd
{
	private String adMobId;
	
	public AdmobBanner(String adMobId)
	{
		this.adMobId = adMobId;
	}

	@Override
	public View createAdView(Activity activity, ViewGroup adContainer, final IBannerCallback callback)
	{
		// Prevent some crashes in some particular cases
		try
		{
			WebViewDatabase.getInstance(activity).clearFormData();
		}
		catch (SQLiteDiskIOException e)
		{
			
		}
		
		final AdView adView = new AdView(activity, AdSize.SMART_BANNER, adMobId);
		
		adView.setAdListener(new AdListener() {

			@Override
			public void onReceiveAd(Ad arg0)
			{
				callback.onReceiveAd(adView);
			}

			@Override
			public void onPresentScreen(Ad arg0)
			{

			}

			@Override
			public void onLeaveApplication(Ad arg0)
			{

			}

			@Override
			public void onFailedToReceiveAd(Ad arg0, ErrorCode errorCode)
			{
				callback.onFailedToReceiveAd(adView, errorCode.name());
			}

			@Override
			public void onDismissScreen(Ad arg0)
			{

			}
		});
		
		return adView;
	}

	@Override
	public void loadAd(View adView)
	{
		((AdView) adView).loadAd(new AdRequest());
	}
	
	@Override
	public void destroyAdView(View adView)
	{
		if (adView != null)
			((AdView) adView).destroy();
	}
}
