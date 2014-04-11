package com.abewy.android.apps.klyph.messenger.facebook.request;

import java.util.ArrayList;
import org.json.JSONArray;
import com.abewy.android.apps.klyph.core.fql.MessageThread;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.fql.serializer.MessageThreadDeserializer;
import com.abewy.android.apps.klyph.messenger.KlyphMessenger;

public class ThreadWithOneFriendRequest extends KlyphQuery
{
	@Override
	public boolean isMultiQuery()
	{
		return true;
	}
	
	@Override
	public String getQuery(String id, String offset)
	{
		String query1 = "SELECT thread_id, subject, recipients, updated_time, parent_message_id, parent_thread_id, message_count, snippet, snippet_author, object_id, unread, viewer_id " +
				"FROM thread " +
				"WHERE (folder_id = 0 or folder_id = 1) " +
				"AND " + id + " IN recipients AND " + id + " IN recent_authors";

		String query2 = "SELECT uid, name, first_name FROM user WHERE uid IN (SELECT recipients FROM #query1)";

		String query3 = "SELECT id, url from square_profile_pic WHERE id IN (SELECT uid FROM #query2) AND size = " + KlyphMessenger.getStandardImageSizeForRequest() * 2;
		
		return multiQuery(query1, query2, query3);
	}

	@Override
	public ArrayList<GraphObject> handleResult(JSONArray[] result)
	{
		JSONArray data = result[0];
		JSONArray recipients = result[1];
		JSONArray user_pics = result[2];
		
		assocData(recipients, user_pics, "uid", "id", "pic", "url");
		assocData3(data, recipients, "recipients", "uid", "recipients_friends");
		
		MessageThreadDeserializer deserializer = new MessageThreadDeserializer();
		ArrayList<GraphObject> mts = (ArrayList<GraphObject>) deserializer.deserializeArray(data);
		
		for (GraphObject graphObject : mts)
		{
			if (((MessageThread) graphObject).getRecipients().size() == 2)
			{
				mts = new ArrayList<GraphObject>();
				mts.add(graphObject);
				break;
			}
		}
		
		if (mts.size() > 1)
			mts = new ArrayList<GraphObject>();

		setHasMoreData(false);
		
		return mts;
	}
}
