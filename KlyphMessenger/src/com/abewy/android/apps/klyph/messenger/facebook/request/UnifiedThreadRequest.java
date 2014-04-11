package com.abewy.android.apps.klyph.messenger.facebook.request;

import java.util.ArrayList;
import org.json.JSONArray;
import android.util.Log;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.fql.serializer.UnifiedThreadDeserializer;
import com.abewy.android.apps.klyph.messenger.KlyphMessenger;

public class UnifiedThreadRequest extends KlyphQuery
{
	@Override
	public boolean isMultiQuery()
	{
		return true;
	}

	@Override
	public String getQuery(String id, String offset)
	{
		String query1 = "SELECT action_id, admin_snippet, archived, auto_mute, can_reply, folder, "
						+ "former_participants, has_attachments, is_group_conversation, is_named_conversation, is_subscribed, last_visible_add_action_id, "
						+ "link, mute, name, num_messages, num_unread, object_participants, participants, pic_hash,"
						+ " read_receipts, senders, single_recipient, snippet, snippet_message_has_attachment, snippet_message_id, " 
						+ "snippet_sender, subject, tags, thread_and_participants_name, thread_fbid, thread_id, thread_participants, " 
						+ "timestamp, title, unread, unseen"
						+ " FROM unified_thread"
						+ " WHERE folder = \"inbox\"";

		if (offset != null && offset.length() > 0)
			query1 += " AND timestamp < " + offset;

		query1 += " ORDER BY timestamp DESC LIMIT 20";
		Log.d("Threads", query1);

		String query2 = "SELECT id, url from square_profile_pic WHERE id IN (SELECT participants.user_id FROM #query1) AND size = "
						+ KlyphMessenger.getStandardImageSizeForRequest() * 2;

		return multiQuery(query1, query2);
	}

	@Override
	public ArrayList<GraphObject> handleResult(JSONArray[] result)
	{
		JSONArray data = result[0];
		JSONArray user_pics = result[1];

		//assocData3(data, user_pics, "recipients", "id", "recipients_friends");

		UnifiedThreadDeserializer deserializer = new UnifiedThreadDeserializer();
		ArrayList<GraphObject> mts = (ArrayList<GraphObject>) deserializer.deserializeArray(data);
		//Collections.reverse(mts);

		setHasMoreData(mts.size() >= 15);

		return mts;
	}
}