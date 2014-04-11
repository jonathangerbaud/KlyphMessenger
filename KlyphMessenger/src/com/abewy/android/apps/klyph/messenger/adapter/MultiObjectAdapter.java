/**
* @author Jonathan
*/

package com.abewy.android.apps.klyph.messenger.adapter;

import java.util.ArrayList;
import java.util.List;
import android.widget.AbsListView;
import com.abewy.android.adapter.MultiTypeAdapter;
import com.abewy.android.adapter.TypeAdapter;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.messenger.adapter.animation.DeleteAdapter;
import com.haarman.listviewanimations.itemmanipulation.OnDismissCallback;

public class MultiObjectAdapter extends MultiTypeAdapter<GraphObject>
{
	private DeleteAdapter		deleteAdapter;
	
	public MultiObjectAdapter(AbsListView listView)
	{
		this(listView, 0);
	}
	
	public MultiObjectAdapter(AbsListView listView, int layoutType)
	{
		super(layoutType);
		
		deleteAdapter = new DeleteAdapter(this, new OnDismissCallback() {

			@Override
			public void onDismiss(AbsListView listView, int[] reverseSortedPositions)
			{
				for (int position : reverseSortedPositions)
				{
					removeAt(position);
				}
			}
		});
		deleteAdapter.setAbsListView(listView);
	}
	
	@Override
	public void remove(GraphObject object)
	{
		remove(object, false);
	}

	public void remove(GraphObject object, Boolean animated)
	{
		if (animated == false)
		{
			super.remove(object);
			notifyDataSetChanged();
		}
		else
		{
			List<Integer> list = new ArrayList<Integer>();
			list.add(getItemPosition(object));
			deleteAdapter.animateDismiss(list);
		}
	}
	
	@Override
	public void removeAt(int index)
	{
		removeAt(index, false);
	}

	public void removeAt(int index, Boolean animated)
	{
		if (index >= 0 && index < getCount())
		{
			if (animated == false)
			{
				super.removeAt(index);
				notifyDataSetChanged();
			}
			else
			{
				List<Integer> list = new ArrayList<Integer>();
				list.add(index);
				deleteAdapter.animateDismiss(list);
			}
		}
	}

	@Override
	protected TypeAdapter<GraphObject> getAdapter(GraphObject object, int layoutType)
	{
		return AdapterSelector.getAdapter(object, layoutType, this);
	}

	private List<Integer> types = new ArrayList<Integer>();
	@Override
	protected int getItemViewType(GraphObject object)
	{
		int type = AdapterSelector.getItemViewType(object);
		int index = types.indexOf(type);
		
		if (index == -1)
		{
			index = types.size();
			types.add(type);
		}
		
		return index;
	}

}
