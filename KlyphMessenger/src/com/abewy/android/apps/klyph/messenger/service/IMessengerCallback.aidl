package com.abewy.android.apps.klyph.messenger.service;

import com.abewy.android.apps.klyph.messenger.service.PPresence;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;

oneway interface IMessengerCallback {
	
	void onPresenceChange(out PPresence presence);
	void onRosterUpdated(out List<PRosterEntry> roster);
}
