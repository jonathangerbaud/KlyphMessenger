package com.abewy.android.apps.klyph.messenger.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import com.abewy.android.apps.klyph.core.KlyphFlags;
import com.abewy.android.apps.klyph.core.KlyphLocale;
import com.abewy.android.apps.klyph.messenger.MessengerPreferences;
import com.abewy.android.apps.klyph.messenger.R;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private static final String			ABOUT_KEY				= "preference_about";
	private static final String			CHANGELOG_KEY			= "preference_changelog";
	private static final String			BUY_PRO_VERSION_KEY		= "preference_buy_pro_version";
	
	private static final int			RINGTONE_CODE			= 159;
	private static final int			SONG_CODE				= 167;
	
	private String						previousRingtone;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(MessengerPreferences.getPreferencesTheme());
		KlyphLocale.defineLocale(getBaseContext());
		
		super.onCreate(savedInstanceState);
		
		getActionBar().setIcon(R.drawable.ic_ab_launcher);

		addPreferencesFromResource(R.xml.preferences);

		refreshAppLanguage();
		refreshFbLanguage();
		refreshRingtoneSummary();
		
		previousRingtone = MessengerPreferences.getNotificationRingtone();
		
		Preference aboutPref = findPreference(ABOUT_KEY);
		Preference changelogPref = findPreference(CHANGELOG_KEY);

		aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				Intent intent = new Intent(PreferencesActivity.this, AboutActivity.class);
				startActivity(intent);
				return true;
			}
		});

		changelogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference)
			{
				Intent intent = new Intent(PreferencesActivity.this, ChangeLogActivity.class);
				startActivity(intent);
				return true;
			}
		});

		if (KlyphFlags.IS_PRO_VERSION == true)
		{
			Preference buyProPref = findPreference(BUY_PRO_VERSION_KEY);
			buyProPref.setEnabled(false);
			buyProPref.setShouldDisableView(true);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == RINGTONE_CODE)
		{
			if (resultCode == Activity.RESULT_CANCELED)
			{
				MessengerPreferences.setNotificationRingtone(previousRingtone);
			}
			else
			{
				Uri ringtoneURI = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				
				if (ringtoneURI != null)
				{
					String ringtoneString = null;
					try
					{
						ringtoneString = RingtoneManager.getRingtone(this, ringtoneURI).getTitle(this);

					}
					catch (final Exception e)
					{
						Log.d("PreferencesActivity", "error " + e.getMessage());
						ringtoneString = "unknown";
					}
					
					Log.d("PreferencesActivity", "uri " + ringtoneURI);
					
					MessengerPreferences.setNotificationRingtone(ringtoneString);
					MessengerPreferences.setNotificationRingtoneUri(ringtoneURI.toString());
				}
				else
				{
					MessengerPreferences.setNotificationRingtone(getString(R.string.none));
					MessengerPreferences.setNotificationRingtoneUri(null);
					
				}
				refreshRingtoneSummary();
			}
		}
		else if (requestCode == SONG_CODE)
		{
			if (resultCode == Activity.RESULT_CANCELED)
			{
				MessengerPreferences.setNotificationRingtone(previousRingtone);
			}
			else
			{
				String path = data.getDataString();
				String name = path;
				Log.d("PreferencesActivity", "title " + data.getStringExtra("title"));
				Log.d("PreferencesActivity", "name " + data.getStringExtra("name"));
				int index = name.lastIndexOf("/");
				if (index != -1)
					name = name.substring(index + 1);
				MessengerPreferences.setNotificationRingtone(name);
				MessengerPreferences.setNotificationRingtoneUri(path);
				refreshRingtoneSummary();
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.d("PreferencesActivity", "onSharedPreferenceChanged " + key);
		if (key.equals(MessengerPreferences.PREFERENCE_THEME))
		{
			restart();
		}
		else if (key.equals(MessengerPreferences.PREFERENCE_APP_LANGUAGE))
		{
			String l = sharedPreferences.getString(key, "default");

			KlyphLocale.setAppLocale(l);
			
			restart();

		}
		else if (key.equals(MessengerPreferences.PREFERENCE_FB_LANGUAGE))
		{
			refreshFbLanguage();
		}
		else if (key.equals(MessengerPreferences.PREFERENCE_NOTIFICATIONS_RINGTONE))
		{
			if (MessengerPreferences.getNotificationRingtone().equals("ringtone"))
			{
				Intent ringtoneManager = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);

				// specifies what type of tone we want, in this case "ringtone", can be notification if you want
				ringtoneManager.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);

				// gives the title of the RingtoneManager picker title
				ringtoneManager.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.preference_notification_ringtone_chooser));

				// returns true shows the rest of the songs on the device in the default location
				ringtoneManager.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);

				startActivityForResult(ringtoneManager, RINGTONE_CODE);
			}
			else if (MessengerPreferences.getNotificationRingtone().equals("song"))
			{
				Intent intent = new Intent();
				intent.setType("audio/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(Intent.createChooser(intent, getString(R.string.preference_notification_ringtone_chooser)), SONG_CODE);
			}
			else
			{
				MessengerPreferences.setNotificationRingtoneUri(null);
				refreshRingtoneSummary();
			}
		}
	}
	
	private void restart()
	{
		Log.d("PreferencesActivity", "restart");
		Intent localIntent = new Intent(getApplicationContext(), PreferencesActivity.class);
	    localIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	    startActivityForResult(localIntent, Activity.RESULT_CANCELED);
	    finish();
	    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	private void refreshAppLanguage()
	{
		ListPreference p = (ListPreference) findPreference(MessengerPreferences.PREFERENCE_APP_LANGUAGE);
		p.setSummary(p.getEntry());
	}

	private void refreshFbLanguage()
	{
		ListPreference p = (ListPreference) findPreference(MessengerPreferences.PREFERENCE_FB_LANGUAGE);
		p.setSummary(p.getEntry());
	}
	
	private void refreshRingtoneSummary()
	{
		Preference p = findPreference(MessengerPreferences.PREFERENCE_NOTIFICATIONS_RINGTONE);
		p.setSummary(MessengerPreferences.getNotificationRingtone());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
