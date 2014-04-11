package com.abewy.android.apps.klyph.messenger.adapter;

import android.view.View;
import android.widget.TextView;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.holder.HeaderHolder;
import com.abewy.klyph.items.Header;

public class HeaderAdapter extends KlyphAdapter
{
	public HeaderAdapter()
	{
		super();
	}
	
	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_header;
	}
	
	@Override
	protected void attachViewHolder(View view)
	{
		view.setTag(new HeaderHolder((TextView) view.findViewById(R.id.header_title)));
	}
	
	@Override
	public void bindData(View view, GraphObject data)
	{
		Header header = (Header) data;
		
		HeaderHolder holder = (HeaderHolder) view.getTag();
		holder.getHeaderTitle().setText(header.getName());
	}
	
	@Override
	protected Boolean isCompatible(View view)
	{
		return view.getTag() instanceof HeaderHolder;
	}

	@Override
	public boolean isEnabled(GraphObject object)
	{
		return false;
	}
}
