/**
* @author Jonathan
*/

package com.abewy.android.apps.klyph.messenger.adapter;

import com.abewy.android.adapter.TypeAdapter;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.klyph.items.ItemType;

public class BaseAdapterSelector
{

	public BaseAdapterSelector()
	{
		// TODO Auto-generated constructor stub
	}
	
	public static TypeAdapter<GraphObject> getAdapter(GraphObject object, int layoutType)
	{
		switch (object.getItemViewType())
		{
			case ItemType.PROGRESS:
			{
				return new ProgressAdapter();
			}
			case ItemType.TEXT_BUTTON:
			{
				return new TextButtonItemAdapter();
			}
			case ItemType.HEADER:
			{
				return new HeaderAdapter();
			}
		}
		
		return null;
	}

}
