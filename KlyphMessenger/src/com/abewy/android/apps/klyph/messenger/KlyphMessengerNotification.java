/**
 * @author Jonathan
 */

package com.abewy.android.apps.klyph.messenger;

import java.util.List;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.NotificationCompat.InboxStyle;
import com.abewy.android.apps.klyph.core.util.AttrUtil;

public class KlyphMessengerNotification
{

	public static Builder getBuilder(Context context, boolean alert)
	{
		Builder builder = new Builder(context).setSmallIcon(R.drawable.ic_notification).setAutoCancel(true).setOnlyAlertOnce(!alert);

		int defaults = 0;
		
		if (alert == true)
		{
			if (MessengerPreferences.getNotificationRingtone() != null && MessengerPreferences.getNotificationRingtone().equals("default"))
			{
				defaults |= android.app.Notification.DEFAULT_SOUND;
			}
			else if (MessengerPreferences.getNotificationRingtoneUri() == null)
			{
				builder.setSound(null);
			}
			else
			{
				builder.setSound(Uri.parse(MessengerPreferences.getNotificationRingtoneUri()));
			}

			if (MessengerPreferences.isNotificationVibrationEnabled() == true)
				defaults |= android.app.Notification.DEFAULT_VIBRATE;

			defaults |= android.app.Notification.DEFAULT_LIGHTS;
			
			builder.setDefaults(defaults);
		}
		
		//int defaults = android.app.Notification.DEFAULT_SOUND | android.app.Notification.DEFAULT_VIBRATE;
		builder.setDefaults(defaults);

		return builder;
	}

	public static void setInboxStyle(Builder builder, String title, List<String> lines)
	{
		builder.setNumber(lines.size());
		InboxStyle inboxStyle = new InboxStyle();

		inboxStyle.setBigContentTitle(title);

		for (String line : lines)
		{
			inboxStyle.addLine(line);
		}

		builder.setStyle(inboxStyle);
	}

	public static void sendNotification(Context context, Builder builder)
	{
		final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		final String tag = AttrUtil.getString(context, R.string.app_name);
		final int id = 0;

		// pair (tag, id) must be unique
		// because n.getObject_id() may not be converted to an int
		// tag is the unique key
		mNotificationManager.notify(tag, id, builder.build());

	}

	public static void setNoSound(Builder builder)
	{
		builder.setSound(null);
	}

	public static void setNoVibration(Builder builder)
	{
		builder.setVibrate(null);
	}
}
