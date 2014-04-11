package com.abewy.android.apps.klyph.messenger;

import java.util.ArrayList;
import java.util.List;
import android.preference.PreferenceManager;
import com.abewy.android.ads.BannerAdManager;
import com.abewy.android.ads.IBannerAd;
import com.abewy.android.apps.klyph.core.BaseApplication;
import com.abewy.android.apps.klyph.core.KlyphLocale;
import com.abewy.android.apps.klyph.core.imageloader.ImageLoader;
import com.abewy.android.apps.klyph.messenger.ads.AdmobBanner;

public class MessengerApplication extends BaseApplication
{
	public static boolean IS_PRO_VERSION = false;
	public static boolean PRO_VERSION_CHECKED = false;
	@Override
	public void onCreate()
	{
		super.onCreate();
	}

	public static MessengerApplication getInstance()
	{
		return (MessengerApplication) BaseApplication.getInstance();
	}

	@Override
	protected void initPreferences()
	{
		//PreferenceManager.setDefaultValues(this, MessengerPreferences.PREFERENCES_FILE_NAME, Context.MODE_PRIVATE, R.xml.preferences, true);
		PreferenceManager.setDefaultValues(getBaseContext(), R.xml.preferences, false);
	}

	@Override
	protected void initGlobals()
	{
		// Klyph.defineFacebookId();

		KlyphLocale.setAppLocale(KlyphLocale.getAppLocale());
	}
	

	@Override
	protected void initAds()
	{
		List<IBannerAd> bannerAds = new ArrayList<IBannerAd>();
		bannerAds.add(new AdmobBanner(getString(R.string.admob_id)));
		BannerAdManager.setBannerAds(bannerAds);
	}

	@Override
	protected void initOthers()
	{
		ImageLoader.initImageLoader(getApplicationContext());
	}

	@Override
	public void onLogout()
	{

	}
}
