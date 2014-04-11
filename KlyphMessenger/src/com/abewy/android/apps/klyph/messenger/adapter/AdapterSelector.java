
package com.abewy.android.apps.klyph.messenger.adapter;

import android.util.Log;
import com.abewy.android.adapter.TypeAdapter;
import com.abewy.android.apps.klyph.core.KlyphSession;
import com.abewy.android.apps.klyph.core.fql.Message;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;

public class AdapterSelector
{
	private static final int MESSAGE_USER_SESSION = 1001;
	
	public AdapterSelector()
	{
		
	}

	static TypeAdapter<GraphObject> getAdapter(GraphObject object, int layoutType, MultiObjectAdapter parentAdapter)
	{
		TypeAdapter<GraphObject> adapter = BaseAdapterSelector.getAdapter(object, layoutType);
		
		if (adapter != null)
			return adapter;
		
		switch (getItemViewType(object))
		{
			case GraphObject.MESSAGE_THREAD:
			{
				return new ThreadAdapter();
			}
			case GraphObject.MESSAGE:
			{
				return new ConversationAdapter();
			}
			case MESSAGE_USER_SESSION:
			{
				return new ConversationSessionUserAdapter();
			}
			case PRosterEntry.ROSTER_ENTRY_TYPE:
			{
				return new RosterEntryAdapter();
			}
		}
		
		Log.e("AdapterSelector", "No adapter defined for type " + object);
		return null;
	}
	
	static int getItemViewType(GraphObject object)
	{
		if (object.getItemViewType() == GraphObject.MESSAGE)
		{
			if (((Message) object).getAuthor_id().equals(KlyphSession.getSessionUserId()))
			{
				return MESSAGE_USER_SESSION;
			}
			
			return GraphObject.MESSAGE;
		}
		
		return object.getItemViewType();
	}
}
