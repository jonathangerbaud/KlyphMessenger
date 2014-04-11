package com.abewy.android.apps.klyph.messenger.service;

import android.os.Parcel;
import android.os.Parcelable;

public class PPresence implements Parcelable
{
	public String	from;
	public String	type;
	
	public PPresence()
	{

	}

	
	
	public PPresence(Parcel in)
	{
        from = in.readString();
        type = in.readString();
	}

	public int describeContents()
	{
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags)
	{
        dest.writeString(from);
        dest.writeString(type);
	}

	public static final Parcelable.Creator<PPresence>	CREATOR	= new Parcelable.Creator<PPresence>() {
																	public PPresence createFromParcel(Parcel in)
																	{
																		return new PPresence(in);
																	}

																	public PPresence[] newArray(int size)
																	{
																		return new PPresence[size];
																	}
																};
}
