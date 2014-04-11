package com.abewy.android.apps.klyph.messenger.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.imageloader.ImageLoader;
import com.abewy.android.apps.klyph.core.util.AttrUtil;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.holder.RosterEntryHolder;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;

public class RosterEntryAdapter extends KlyphAdapter
{
	public RosterEntryAdapter()
	{
		super();
	}

	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_roster_entry;
	}

	@Override
	protected void attachViewHolder(View view)
	{
		ImageView friendPicture = (ImageView) view.findViewById(R.id.picture);
		TextView friendName = (TextView) view.findViewById(R.id.primary_text);

		setHolder(view, new RosterEntryHolder(friendPicture, friendName));
	}

	@Override
	public void bindData(View view, GraphObject data)
	{
		RosterEntryHolder holder = (RosterEntryHolder) getHolder(view);

		PRosterEntry friend = (PRosterEntry) data;

		holder.getPrimaryText().setText(friend.name);

		ImageLoader.display(holder.getPicture(), friend.getPic(), AttrUtil.getResourceId(getContext(view), R.attr.picturePlaceHolder));
	}

	@Override
	protected Boolean isCompatible(View view)
	{
		return getHolder(view) instanceof RosterEntryHolder;
	}
}
