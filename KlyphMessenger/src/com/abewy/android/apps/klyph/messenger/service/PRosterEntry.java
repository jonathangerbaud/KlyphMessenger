package com.abewy.android.apps.klyph.messenger.service;

import android.os.Parcel;
import android.os.Parcelable;
import com.abewy.android.apps.klyph.core.graph.GraphObject;

public class PRosterEntry extends GraphObject implements Parcelable
{
	public static final int ROSTER_ENTRY_TYPE = 1568;
	
	public static final String	AVAILABLE	= "available";
	public static final String	UNAVAILABLE	= "unavailable";

	private String				id;
	public String				user;
	public String				name;
	public String				presence;
	private String				pic;

	public PRosterEntry()
	{

	}

	// ___ GraphObject stuff ___________________________________________________

	public int getItemViewType()
	{
		return ROSTER_ENTRY_TYPE;
	}

	// ___ Parcelable stuff ____________________________________________________

	public PRosterEntry(Parcel in)
	{
		user = in.readString();
		name = in.readString();
		presence = in.readString();
	}

	public int describeContents()
	{
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(user);
		dest.writeString(name);
		dest.writeString(presence);
	}

	public static final Parcelable.Creator<PRosterEntry>	CREATOR	= new Parcelable.Creator<PRosterEntry>() {
																		public PRosterEntry createFromParcel(Parcel in)
																		{
																			return new PRosterEntry(in);
																		}

																		public PRosterEntry[] newArray(int size)
																		{
																			return new PRosterEntry[size];
																		}
																	};

	// ___ Public services _____________________________________________________

	public Boolean isAvailable()
	{
		return presence != null && presence.equals(AVAILABLE);
	}

	public Boolean isUnavailable()
	{
		return presence != null && presence.equals(UNAVAILABLE);
	}

	public String getId()
	{
		if (id == null)
		{
			id = user;
			id = id.substring(1, id.indexOf("@"));
		}

		return id;
	}
	
	public void setId(String id)
	{
		this.id = "-" + id + "@facebook.com";  
	}

	public String getName()
	{
		return name;
	}

	public String getPic()
	{
		return pic;
	}

	public void setPic(String pic)
	{
		this.pic = pic;
	}
}
