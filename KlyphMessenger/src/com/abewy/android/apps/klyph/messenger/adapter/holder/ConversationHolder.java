package com.abewy.android.apps.klyph.messenger.adapter.holder;

import android.widget.ImageView;
import android.widget.TextView;

public class ConversationHolder
{
	private ImageView	authorPicture;
	private TextView	messageTextView;
	private TextView	dateTextView;

	public ConversationHolder(ImageView authorPicture, TextView messageTextView, TextView dateTextView)
	{
		this.authorPicture = authorPicture;
		this.messageTextView = messageTextView;
		this.dateTextView = dateTextView;
	}

	public ImageView getAuthorPicture()
	{
		return authorPicture;
	}
	
	public TextView getDateTextView()
	{
		return dateTextView;
	}

	public TextView getMessageTextView()
	{
		return messageTextView;
	}
}
