package com.abewy.android.apps.klyph.messenger;

import com.abewy.android.apps.klyph.core.KlyphDevice;
import com.abewy.android.apps.klyph.core.KlyphFlags;

public class KlyphMessenger
{
	public static String	FACEBOOK_APP_ID				= "";

	public static void defineFacebookId()
	{
		FACEBOOK_APP_ID = KlyphFlags.IS_PRO_VERSION ? "[FB_FREE_ID]" : "[FB_PRO_ID]";
	}

	public static int getStandardImageSizeForRequest()
	{
		int imageSize = 50;
		
		switch (KlyphDevice.getDeviceDPI())
		{
			case 213:
			case 240:
			{
				imageSize *= 2;
				break;
			}
			case 320:
			case 480:
			{
				imageSize *= 3;
				break;
			}
		}

		return imageSize;
	}
	
	public static int getStandardImageSizeForNotification()
	{
		int imageSize = 56;

		switch (KlyphDevice.getDeviceDPI())
		{
			case 213:
			case 240:
			{
				imageSize *= 2;
				break;
			}
			case 320:
			{
				imageSize *= 3;
				break;
			}
			case 480:
			{
				imageSize *= 4;
				break;
			}
		}

		return imageSize;
	}
}
