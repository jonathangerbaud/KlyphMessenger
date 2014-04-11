package com.abewy.android.apps.klyph.messenger.adapter;

import util.EmojiUtil;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.abewy.android.apps.klyph.core.fql.Message;
import com.abewy.android.apps.klyph.core.fql.Message.Media;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.imageloader.ImageLoader;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.holder.ConversationHolder;
import com.abewy.android.apps.klyph.messenger.util.DateUtil;

public class ConversationAdapter extends KlyphAdapter
{
	public ConversationAdapter()
	{
		super();
	}

	@Override
	protected int getLayoutRes()
	{
		return R.layout.item_conversation_friend;
	}

	@Override
	protected void attachViewHolder(View view)
	{
		ImageView authorPicture = (ImageView) view.findViewById(R.id.message_author_picture);
		TextView messageTV = (TextView) view.findViewById(R.id.message_body);
		TextView date = (TextView) view.findViewById(R.id.message_date);

		setHolder(view, new ConversationHolder(authorPicture, messageTV, date));
	}

	@Override
	public void bindData(View view, GraphObject data)
	{
		ConversationHolder holder = (ConversationHolder) getHolder(view);
		
		Message message = (Message) data;

		holder.getMessageTextView().setText(EmojiUtil.getSpannableForText(holder.getMessageTextView().getContext(), message.getBody()));
		holder.getDateTextView().setText(DateUtil.getShortDateTime(message.getCreated_time()));
		
		//TextViewUtil.setElementClickable(getContext(view), holder.getAuthorName(), message.getAuthor_name(), message.getAuthor_id(), "user");
		
		ImageLoader.display(holder.getAuthorPicture(), message.getAuthor_pic());
	}

	@Override
	protected Boolean isCompatible(View view)
	{
		return getHolder(view) instanceof ConversationHolder;
	}
}
