package com.abewy.android.apps.klyph.messenger.adapter;

import android.content.Context;
import android.view.View;
import com.abewy.android.adapter.TypeAdapter;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.messenger.R;

public abstract class KlyphAdapter extends TypeAdapter<GraphObject>
{

	public KlyphAdapter()
	{
		
	}
		
	@Override
	public boolean isEnabled(GraphObject object)
	{
		return true;
	}
	
	protected Boolean isCompatible(View view)
	{
		return false;
	}
	
	@Override
	public void setLayoutParams(View view)
	{
		
	}

	protected Context getContext(View view)
	{
		return view.getContext();
	}
	
	protected void setHolder(View view, Object holder)
	{
		view.setTag(R.id.view_holder, holder);
	}

	protected Object getHolder(View view)
	{
		return view.getTag(R.id.view_holder);
	}

	protected void setData(View view, GraphObject data)
	{
		view.setTag(R.id.view_data, data);
	}

	protected GraphObject getData(View view)
	{
		return (GraphObject) view.getTag(R.id.view_data);
	}
}
