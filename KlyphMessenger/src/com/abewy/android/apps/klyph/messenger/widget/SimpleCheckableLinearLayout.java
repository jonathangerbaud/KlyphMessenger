package com.abewy.android.apps.klyph.messenger.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class SimpleCheckableLinearLayout extends LinearLayout implements Checkable
{
	private boolean		checked = false;

	public SimpleCheckableLinearLayout(Context context)
	{
		super(context);
	}

	public SimpleCheckableLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	@Override
	public boolean isChecked()
	{
		return checked;
	}

	@Override
	public void toggle()
	{
		setChecked(!checked);
	}
}
