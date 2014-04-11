package com.abewy.android.apps.klyph.messenger.adapter;

import android.util.Log;
import android.view.View;
import com.abewy.android.apps.klyph.messenger.R;

public class ConversationSessionUserAdapter extends ConversationAdapter
{
	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_conversation_owner;
	}
}
