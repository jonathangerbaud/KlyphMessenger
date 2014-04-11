package com.abewy.android.apps.klyph.messenger.service;

import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;
import com.abewy.android.apps.klyph.messenger.service.IMessengerCallback;

interface IMessengerService
{
	List<PRosterEntry> getRoster();
	
	void setSelectedRecipient(String id);
	void setNoSelectedRecipient();
	void clearSavedMessages();
	void removeSavedMessages(String id);
	void setNotificationsEnabled(boolean enabled);
	void setRingtone(String ringtone);
	void setRingtoneUri(String uri);
	void setVibrateEnabled(boolean enabled);
	
	/**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(IMessengerCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(IMessengerCallback cb);
}