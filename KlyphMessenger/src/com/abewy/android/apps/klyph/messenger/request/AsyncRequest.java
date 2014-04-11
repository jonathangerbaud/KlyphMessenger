package com.abewy.android.apps.klyph.messenger.request;

import android.os.Bundle;
import com.abewy.android.apps.klyph.core.request.BaseAsyncRequest;
import com.abewy.android.apps.klyph.core.request.RequestError;
import com.abewy.android.apps.klyph.core.request.RequestQuery;
import com.abewy.android.apps.klyph.messenger.facebook.request.FriendsRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.MessageFromUserRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.MessageRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.ThreadNewestRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.ThreadRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.ThreadWithOneFriendRequest;
import com.abewy.android.apps.klyph.messenger.facebook.request.UnifiedThreadRequest;

public class AsyncRequest extends BaseAsyncRequest
{
	public AsyncRequest(int query, String id, String offset, Callback callBack)
	{
		super(query, id, offset, callBack);
	}

	public AsyncRequest(int query, String id, Bundle params, Callback callBack)
	{
		super(query, id, params, callBack);
	}

	public static final class Query
	{
		public static final int	NONE				= -1;
		public static final int	THREADS				= 26;
		public static final int	MESSAGES			= 27;
		public static final int	MESSAGES_FROM_USER	= 62;
		public static final int	UNIFIED_THREADS		= 67;
		public static final int	FRIENDS				= 68;
		public static final int	THREAD_WITH_FRIEND	= 69;
		public static final int	THREADS_NEWEST		= 70;
	}

	@Override
	protected void doCallBack(RequestError error)
	{
		// Crashlytics report on request error
		/*
		 * if (getQuery() == Query.NEWSFEED || getQuery() == Query.NEWSFEED_NEWEST)
		 * {
		 * Crashlytics.setString("Query " + getQuery(), error.getMessage());
		 * 
		 * try
		 * {
		 * throw new Exception("Class : " + this.getClass().getName() + "\n, Request " + getQuery() + ", Id " + getId() + ", Offset " + getOffset()
		 * + "\n, Error " + error.getMessage());
		 * }
		 * catch (Exception e)
		 * {
		 * Crashlytics.logException(e);
		 * }
		 * }
		 */

		super.doCallBack(error, null, null);
	}

	@Override
	protected RequestQuery getSubQuery(int query)
	{
		switch (query)
		{
			case Query.THREADS:
			{
				return new ThreadRequest();
			}
			case Query.MESSAGES:
			{
				return new MessageRequest();
			}
			case Query.MESSAGES_FROM_USER:
			{
				return new MessageFromUserRequest();
			}
			case Query.THREAD_WITH_FRIEND:
			{
				return new ThreadWithOneFriendRequest();
			}
			case Query.UNIFIED_THREADS:
			{
				return new UnifiedThreadRequest();
			}
			case Query.FRIENDS:
			{
				return new FriendsRequest();
			}
			case Query.THREADS_NEWEST:
			{
				return new ThreadNewestRequest();
			}
		}

		return null;
	}
}
