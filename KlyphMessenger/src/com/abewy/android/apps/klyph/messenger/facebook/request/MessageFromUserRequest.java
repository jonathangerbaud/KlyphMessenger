package com.abewy.android.apps.klyph.messenger.facebook.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import com.abewy.android.apps.klyph.core.fql.MessageThread;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.request.RequestQuery;
import com.abewy.android.apps.klyph.core.fql.serializer.MessageDeserializer;
import com.abewy.android.apps.klyph.core.fql.serializer.MessageThreadDeserializer;
import com.abewy.android.apps.klyph.messenger.KlyphMessenger;

public class MessageFromUserRequest extends KlyphQuery
{
	@Override
	public String getQuery(String id, String offset)
	{
		String query = "SELECT thread_id, recipients FROM thread WHERE folder_id = 0 OR folder_id = 1 LIMIT 1000";
		
		return query;
	}

	@Override
	public ArrayList<GraphObject> handleResult(JSONArray result)
	{
		MessageThreadDeserializer deserializer = new MessageThreadDeserializer();
		ArrayList<GraphObject> threads = (ArrayList<GraphObject>) deserializer.deserializeArray(result);
		
		return threads;
	}

	@Override
	public RequestQuery getNextQuery()
	{
		return new NextQuery();
	}
	
	private class NextQuery extends KlyphQuery
	{
		@Override
		public boolean isMultiQuery()
		{
			return true;
		}

		@Override
		public boolean isNextQuery()
		{
			return true;
		}

		@Override
		public String getQuery(List<GraphObject> previousResults, String id, String offset)
		{
			String threadId = "0";
			
			for (GraphObject graphObject : previousResults)
			{
				MessageThread thread = (MessageThread) graphObject;
				
				if (thread.getRecipients().size() == 2)
				{
					String id1 = thread.getRecipients().get(0);
					String id2 = thread.getRecipients().get(1);
					
					if (id1.equals(id) || id2.equals(id))
					{
						threadId = thread.getThread_id();
						break;
					}
				}
			}
			
			String query1 = "SELECT message_id, thread_id, author_id, body, created_time, attachment, viewer_id FROM message WHERE thread_id = " + threadId;
			
			if (offset != null && offset.length() > 0)
				query1 += " AND created_time > " + offset;
			
			query1 += " ORDER BY created_time DESC LIMIT 50";
			
			String query2 = "SELECT id, name, type FROM profile WHERE id IN (SELECT author_id FROM #query1)";

			String query3 = "SELECT id, url from square_profile_pic WHERE id IN (SELECT id FROM #query2) AND size = "
					+ KlyphMessenger.getStandardImageSizeForRequest() * 2;

			return multiQuery(query1, query2, query3);
		}

		@Override
		public List<GraphObject> handleResult(List<GraphObject> previousResults, JSONArray result[])
		{
			JSONArray data = result[0];
			JSONArray profiles = result[1];
			JSONArray urls = result[2];
			
			assocData(data, profiles, "author_id", "id", "author_name", "name");
			assocData(data, urls, "author_id", "id", "author_pic", "url");
			
			MessageDeserializer deserializer = new MessageDeserializer();
			ArrayList<GraphObject> messages = (ArrayList<GraphObject>) deserializer.deserializeArray(data);
			Collections.reverse(messages);

			setHasMoreData(messages.size() >= 20);
			
			return messages;
		}
	}
}
