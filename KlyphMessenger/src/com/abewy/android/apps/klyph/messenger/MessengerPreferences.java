package com.abewy.android.apps.klyph.messenger;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import com.abewy.android.apps.klyph.core.KlyphFlags;

public class MessengerPreferences
{
	public static final String	PREFERENCES_FILE_NAME					= "klyph_messenger";

	public static final String	PREFERENCE_THEME						= "preference_theme";
	public static final String	PREFERENCE_APP_LANGUAGE					= "preference_app_language";
	public static final String	PREFERENCE_FB_LANGUAGE					= "preference_fb_language";
	public static final String	LAST_CONVERSATIONS						= "last_conversations";
	public static final String	FRIENDS									= "friends";
	public static final String	PREFERENCE_NOTIFICATIONS				= "preference_notifications";
	public static final String	PREFERENCE_NOTIFICATIONS_VIBRATE		= "preference_notifications_vibrate";
	public static final String	PREFERENCE_NOTIFICATIONS_RINGTONE		= "preference_notifications_ringtone";
	public static final String	PREFERENCE_NOTIFICATIONS_RINGTONE_URI	= "preference_notifications_ringtone_uri";
	public static final String	PERFORMANCES_ROUNDED_PICTURES			= "preference_performances_rounded_profile_picture";

	static SharedPreferences getPreferences()
	{
		//return MessengerApplication.getInstance().getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
		return PreferenceManager.getDefaultSharedPreferences(MessengerApplication.getInstance());
	}

	public static Boolean areAdsEnabled()
	{
		return KlyphFlags.BANNER_ADS_ENABLED && KlyphFlags.IS_PRO_VERSION == false;
	}

	public static int getTheme()
	{
		String theme = getPreferences().getString(MessengerPreferences.PREFERENCE_THEME,
				MessengerApplication.getInstance().getString(R.string.theme_light_blue));

		if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_light_blue)))
		{
			return R.style.KlyphMessenger_Light_Blue;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_dark_blue)))
		{
			return R.style.KlyphMessenger_Dark_Blue;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_black_blue)))
		{
			return R.style.KlyphMessenger_Black_Blue;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_light_green)))
		{
			return R.style.KlyphMessenger_Light_Green;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_dark_green)))
		{
			return R.style.KlyphMessenger_Dark_Green;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_black_green)))
		{
			return R.style.KlyphMessenger_Black_Green;
		}

		return R.style.KlyphMessenger_Light_Blue;
	}

	public static int getPreferencesTheme()
	{
		String theme = getPreferences().getString(MessengerPreferences.PREFERENCE_THEME,
				MessengerApplication.getInstance().getString(R.string.theme_light_blue));

		if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_light_blue)))
		{
			return R.style.KlyphMessenger_Light;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_dark_blue)))
		{
			return R.style.KlyphMessenger_Dark;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_black_blue)))
		{
			return R.style.KlyphMessenger_Dark;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_light_green)))
		{
			return R.style.KlyphMessenger_Light;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_dark_green)))
		{
			return R.style.KlyphMessenger_Dark;
		}
		else if (theme.equals(MessengerApplication.getInstance().getString(R.string.theme_black_green)))
		{
			return R.style.KlyphMessenger_Dark;
		}

		return R.style.KlyphMessenger_Light;
	}

	public static String getLastConversations()
	{
		return getPreferences().getString(LAST_CONVERSATIONS, null);
	}

	public static void setLastConversations(String data)
	{
		Editor editor = getPreferences().edit();
		editor.putString(LAST_CONVERSATIONS, data);
		editor.commit();
	}

	public static String getFriends()
	{
		return getPreferences().getString(FRIENDS, null);
	}

	public static void setFriends(String data)
	{
		Editor editor = getPreferences().edit();
		editor.putString(FRIENDS, data);
		editor.commit();
	}

	public static boolean areNotificationsEnabled()
	{
		return getPreferences().getBoolean(PREFERENCE_NOTIFICATIONS, true);
	}

	public static boolean isNotificationVibrationEnabled()
	{
		return getPreferences().getBoolean(PREFERENCE_NOTIFICATIONS_VIBRATE, true);
	}

	public static String getNotificationRingtone()
	{
		return getPreferences().getString(PREFERENCE_NOTIFICATIONS_RINGTONE, "default");
	}

	public static void setNotificationRingtone(String ringtone)
	{
		Editor editor = getPreferences().edit();
		editor.putString(PREFERENCE_NOTIFICATIONS_RINGTONE, ringtone);
		editor.commit();
	}

	public static String getNotificationRingtoneUri()
	{
		Log.d("MessengerPreferences", "getNotificationRingtoneUri " + getPreferences().getString(PREFERENCE_NOTIFICATIONS_RINGTONE_URI, null));
		return getPreferences().getString(PREFERENCE_NOTIFICATIONS_RINGTONE_URI, null);
	}

	public static void setNotificationRingtoneUri(String uri)
	{
		Log.d("MessengerPreferences", "setNotificationRingtoneUri " + uri);
		Editor editor = getPreferences().edit();
		editor.putString(PREFERENCE_NOTIFICATIONS_RINGTONE_URI, uri);
		editor.commit();
	}

	public static Boolean isRoundedPictureEnabled()
	{
		return getPreferences().getBoolean(PERFORMANCES_ROUNDED_PICTURES, false);
	}
}
