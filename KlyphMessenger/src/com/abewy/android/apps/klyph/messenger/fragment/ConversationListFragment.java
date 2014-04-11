package com.abewy.android.apps.klyph.messenger.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import com.abewy.android.apps.klyph.core.KlyphSession;
import com.abewy.android.apps.klyph.core.fql.MessageThread;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.request.BaseAsyncRequest;
import com.abewy.android.apps.klyph.core.request.RequestError;
import com.abewy.android.apps.klyph.core.request.Response;
import com.abewy.android.apps.klyph.core.fql.serializer.MessageThreadDeserializer;
import com.abewy.android.apps.klyph.core.fql.serializer.MessageThreadSerializer;
import com.abewy.android.apps.klyph.messenger.MessengerApplication;
import com.abewy.android.apps.klyph.messenger.MessengerPreferences;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.MultiObjectAdapter;
import com.abewy.android.apps.klyph.messenger.request.AsyncRequest;
import com.abewy.android.apps.klyph.messenger.request.AsyncRequest.Query;
import com.abewy.android.apps.klyph.messenger.service.IMessengerCallback;
import com.abewy.android.apps.klyph.messenger.service.IMessengerService;
import com.abewy.android.apps.klyph.messenger.service.MessengerService;
import com.abewy.android.apps.klyph.messenger.service.PPresence;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;
import com.crashlytics.android.Crashlytics;

public class ConversationListFragment extends KlyphFragment
{
	public static interface ConversationListCallback
	{
		public void onConversationSelected(MessageThread thread);
	}

	private ConversationListCallback	listener;
	private ReadDataTask				readTask;
	private boolean						isStoredData;
	private List<PRosterEntry>			roster;
	private boolean						hasLoadedOnce	= false;
	private String						selectedConversationId;
	private String						selectedThreadId;

	public ConversationListFragment()
	{
		setRequestType(Query.THREADS);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		setListAdapter(new MultiObjectAdapter(getListView()));

		defineEmptyText(R.string.empty_list_no_message);

		PreferenceManager.getDefaultSharedPreferences(MessengerApplication.getInstance()).registerOnSharedPreferenceChangeListener(
				sharedPreferencesListener);

		getListView().setDrawSelectorOnTop(false);
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		setListVisible(false);

		setRequestType(Query.THREADS);

		super.onViewCreated(view, savedInstanceState);
	}

	private void sendConversationToService(String id)
	{
		if (mIRemoteService != null)
		{
			try
			{
				mIRemoteService.setSelectedRecipient(id);
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendNoConversationToService()
	{
		if (mIRemoteService != null)
		{
			try
			{
				mIRemoteService.setNoSelectedRecipient();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void refreshRoster()
	{
		if (mIRemoteService != null)
		{
			try
			{
				roster = mIRemoteService.getRoster();
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
		}
	}

	private boolean refreshPresenceLists()
	{
		Log.d("ConversationListFragment", "refreshPresenceLists");

		if (roster != null && roster.size() > 0 && getAdapter().getCount() > 0)
		{
			Log.d("ConversationListFragment", "refreshPresenceLists 2");
			List<GraphObject> data = getAdapter().getItems();

			boolean presenceSet = bindPresenceToConversation(data);

			if (presenceSet)
				getAdapter().notifyDataSetChanged();
			
			return presenceSet;
		}
		
		return false;
	}

	private boolean bindPresenceToConversation(List<GraphObject> data)
	{
		boolean presenceSet = false;
		for (GraphObject graphObject : data)
		{
			if (graphObject instanceof MessageThread)
			{
				MessageThread thread = (MessageThread) graphObject;
				thread.setFriend_is_online(false);

				if (thread.getRecipients().size() == 2)
				{
					for (String id : thread.getRecipients())
					{
						if (!id.equals(KlyphSession.getSessionUserId()))
						{
							PRosterEntry entry = getRosterEntry(id);

							if (entry != null)
							{
								thread.setFriend_is_online(entry.isAvailable());
								presenceSet = true;
							}
							break;
						}
					}
				}
			}
		}

		return presenceSet;
	}

	private PRosterEntry getRosterEntry(String id)
	{
		if (roster != null)
		{
			for (PRosterEntry entry : roster)
			{
				if (entry.getId().equals(id))
				{
					return entry;
				}
			}
		}

		return null;
	}

	@Override
	protected void populate(List<GraphObject> data)
	{
		if (isFirstLoad())
			getAdapter().clear();

		final int size = getAdapter().getCount();

		bindPresenceToConversation(data);

		super.populate(data);

		if (size == 0 && !isStoredData)
		{
			new StoreDataTask().execute();
		}

		if (data.size() > 0)
			setOffset(((MessageThread) data.get(data.size() - 1)).getUpdated_time());

		if (selectedThreadId != null)
			selectThread(selectedThreadId);
		if (selectedConversationId != null)
			setSelectedConversation(selectedConversationId);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		// getListView().setItemChecked(position, true);
		
		for (GraphObject object : getAdapter().getItems())
		{
			object.setSelected(false);
		}

		MessageThread thread = (MessageThread) l.getItemAtPosition(position);
		thread.setUnread(0);
		thread.setSelected(true);

		selectedThreadId = thread.getThread_id();

		if (thread.isSingleUserConversation())
		{
			for (String uid : thread.getRecipients())
			{
				if (!uid.equals(KlyphSession.getSessionUserId()))
				{
					sendConversationToService(uid);
					break;
				}
			}
		}
		else
		{
			sendNoConversationToService();
		}

		listener.onConversationSelected(thread);
		getAdapter().notifyDataSetChanged();
	}

	public void deselect()
	{
		selectedThreadId = null;
		selectedConversationId = null;

		getListView().clearChoices();
		for (GraphObject object : getAdapter().getItems())
		{
			object.setSelected(false);
		}
		getAdapter().notifyDataSetChanged();
		sendNoConversationToService();

		// Hack to deselect the list
		/*getListView().post(new Runnable() {

			@Override
			public void run()
			{
				getAdapter().notifyDataSetChanged();
			}
		});*/
	}

	public void setSelectedConversation(String id)
	{
		selectedConversationId = id;
		selectedThreadId = null;
		Log.d("ConversationListFragment", "setSelectedConversation " + id);
		int n = getAdapter().getCount();

		for (int i = 0; i < n; i++)
		{
			GraphObject object = getAdapter().getItem(i);

			if (object instanceof MessageThread)
			{
				MessageThread thread = (MessageThread) object;
				thread.setSelected(false);

				if (thread.isSingleUserConversation())
				{
					
					for (String uid : thread.getRecipients())
					{
						if (uid.equals(id))
						{
							Log.d("ConversationListFragmen0t", "setSelectedConversation found");
							thread.setUnread(0);
							
							thread.setSelected(true);
							selectedThreadId = thread.getThread_id();
						}
					}
				}
			}
		}
		
		getAdapter().notifyDataSetChanged();

		sendConversationToService(id);
	}

	private void selectThread(String threadId)
	{
		selectedThreadId = threadId;
		selectedConversationId = null;
		
		List<GraphObject> items = getAdapter().getItems();
		int n = items.size();

		for (int i = 0; i < n; i++)
		{
			GraphObject object = items.get(i);

			if (object instanceof MessageThread)
			{
				MessageThread thread = (MessageThread) object;
				thread.setSelected(false);

				if (thread.getThread_id().equals(threadId))
				{
					Log.d("ConversationListFragment", "setSelectedThread found");
					thread.setUnread(0);
					thread.setSelected(true);
				}
				
			}
		}
		
		getAdapter().notifyDataSetChanged();
	}

	private void onMessageReceived(Bundle data)
	{
		String uid = data.getString("participant");
		String msg = data.getString("body");
		String date = data.getString("date");
		date = date.substring(0, date.length() - 3);

		int selectedIndex = getListView().getCheckedItemPosition();

		int n = getAdapter().getCount();

		boolean found = false;
		for (int i = 0; i < n; i++)
		{
			GraphObject object = getAdapter().getItem(i);

			if (object instanceof MessageThread)
			{
				MessageThread thread = (MessageThread) object;

				if (thread.isMultiUserConversation())
					continue;

				for (String id : thread.getRecipients())
				{
					if (id.equals(uid))
					{
						if (i != selectedIndex)
						{
							thread.setUnread(thread.getUnread() + 1);
						}

						thread.setSnippet(msg);
						thread.setUpdated_time(date);
						found = true;
						break;
					}
				}
			}

			if (found)
				break;
		}

		if (found)
			getAdapter().notifyDataSetChanged();
		else 
			loadNewest();

		/*
		 * if (uid.equals(recipientId))
		 * {
		 * String date = data.getString("date");
		 * date = date.substring(0, date.length() - 3);
		 * 
		 * Message msg = new Message();
		 * msg.setAuthor_id(uid);
		 * msg.setAuthor_name(data.getString("from"));
		 * msg.setCreated_time(date);
		 * msg.setAuthor_pic(recipientPicUrl != null && recipientPicUrl.length() > 0 ? recipientPicUrl : FacebookUtil.getProfilePictureURLForId(uid));
		 * msg.setBody(data.getString("body"));
		 * 
		 * getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		 * getAdapter().add(msg);
		 * getAdapter().notifyDataSetChanged();
		 * getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
		 * }
		 */
	}
	
	public void userSentMessage(String threadId, String message)
	{
		List<GraphObject> items = getAdapter().getItems();
		int n = items.size();

		for (int i = 0; i < n; i++)
		{
			GraphObject object = items.get(i);

			if (object instanceof MessageThread)
			{
				MessageThread thread = (MessageThread) object;
				thread.setSelected(false);

				if (thread.getThread_id().equals(threadId))
				{
					Log.d("ConversationFragment", "userSentMessage found");
					thread.setSnippet(message);
					thread.setSnippet_author(KlyphSession.getSessionUserId());
				}
			}
		}
		
		getAdapter().notifyDataSetChanged();
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		if (activity instanceof ConversationListCallback)
			listener = (ConversationListCallback) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		listener = null;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (hasLoadedOnce)
			loadNewest();
	}
	
	@Override
	protected void loadNewest()
	{
		String offset = null;
		
		if (getAdapter().getCount() > 0)
		{
			for (int i = 0; i < getAdapter().getCount(); i++)
			{
				GraphObject object = getAdapter().getItem(i);
				
				if (object.getItemViewType() == GraphObject.MESSAGE_THREAD)
				{
					offset = ((MessageThread) object).getUpdated_time();
					break;
				}
			}
		}
		
		new AsyncRequest(Query.THREADS_NEWEST, "", offset, new BaseAsyncRequest.Callback() {
			
			@Override
			public void onComplete(Response response)
			{
				onRequestComplete(response);
			}
		}).execute();
	}
	
	private void onRequestComplete(final Response response)
	{
		if (getActivity() != null)
		{
			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run()
				{
					if (response.getError() == null)
					{
						onRequestSuccess(response.getGraphObjectList());
					}
					else
					{
						onRequestError(response.getError());
					}
					
					//setActionBarRefreshItemLoading(false);
				}
			});
		}
	}
	
	private void onRequestSuccess(List<GraphObject> result)
	{
		Log.d("ConversationListFragment", "onRequestSuccess");

		if (getView() == null || getActivity() == null)
			return;

		List<GraphObject> items = getAdapter().getItems();
		List<GraphObject> data = new ArrayList<GraphObject>();
		
		for (int i = 0; i < result.size(); i++)
		{
			MessageThread thread = (MessageThread) result.get(i);
			
			for (int j = 0; j < items.size(); j++)
			{
				GraphObject graphObject = items.get(j);
				
				if (graphObject.getItemViewType() == GraphObject.MESSAGE_THREAD)
				{
					if (((MessageThread) graphObject).getThread_id().equals(thread.getThread_id()))
					{	
						items.remove(j);
						j--;
						data.add(thread);
						result.remove(thread);
						i--;
					}
				}
			}
		}
		
		result.addAll(data);
		for (GraphObject graphObject : items)
		{
			if (graphObject.getItemViewType() == GraphObject.MESSAGE_THREAD)
			{
				result.add(graphObject);
			}
		}
		
		Collections.sort(result, new Comparator<GraphObject>() {

			@Override
			public int compare(GraphObject lhs, GraphObject rhs)
			{
				String time1 = ((MessageThread) lhs).getUpdated_time();
				String time2 = ((MessageThread) rhs).getUpdated_time();
				
				return time2.compareTo(time1);
			}});

		getAdapter().setData(result);
		
		if (selectedThreadId != null)
			selectThread(selectedThreadId);
		if (selectedConversationId != null)
			setSelectedConversation(selectedConversationId);
		
		refreshPresenceLists();
	}

	private void onRequestError(RequestError error)
	{
		//do nothing
	}

	public void disconnectFromService()
	{
		doUnbindService();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		PreferenceManager.getDefaultSharedPreferences(MessengerApplication.getInstance()).unregisterOnSharedPreferenceChangeListener(
				sharedPreferencesListener);

		doUnbindService();
		listener = null;
		mCallback = null;
		mConnection = null;
		mIsBound = false;
		mIRemoteService = null;
	}

	@Override
	public void load()
	{
		if (getView() != null)
		{
			if (isFirstLoad())
			{
				connectToService();

				hasLoadedOnce = true;

				readTask = new ReadDataTask();
				readTask.execute();
			}
			else
			{
				setIsFirstLoad(true);
				super.load();
			}
		}
	}

	private class ReadDataTask extends AsyncTask<Void, Void, List<GraphObject>>
	{

		@Override
		protected List<GraphObject> doInBackground(Void... params)
		{
			String json = MessengerPreferences.getLastConversations();

			if (json == null)
				return null;

			JSONArray data;
			try
			{
				data = new JSONArray(json);
			}
			catch (JSONException e)
			{
				return null;
			}

			MessageThreadDeserializer mtd = new MessageThreadDeserializer();
			List<GraphObject> list = mtd.deserializeArray(data);

			return list;
		}

		@Override
		protected void onPostExecute(List<GraphObject> result)
		{
			onStoredDataLoaded(result);
		}
	}

	private void onStoredDataLoaded(final List<GraphObject> data)
	{
		if (data != null && getView() != null)
		{
			if (getActivity() != null)
			{
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run()
					{
						isStoredData = true;
						populate(data);
						isStoredData = false;
						setNoMoreData(false);
						load();
					}
				});
			}
		}
		else
		{
			if (getActivity() != null)
			{
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run()
					{
						ConversationListFragment.super.load();
					}
				});
			}
		}
	}

	private class StoreDataTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			List<MessageThread> conversations = new ArrayList<MessageThread>();
			List<GraphObject> objects = getAdapter().getItems();
			
			for (GraphObject object : objects)
			{
				if (object instanceof MessageThread)
				{
					conversations.add((MessageThread) object);
				}
			}

			MessageThreadSerializer mts = new MessageThreadSerializer();
			JSONArray json = mts.serializeArray(conversations);
			String jsonString = json.toString();
			MessengerPreferences.setLastConversations(jsonString);

			return null;
		}

		@Override
		protected void onPostExecute(Void params)
		{

		}
	}

	// Service
	/** Messenger for communicating with service. */
	Messenger		mService	= null;
	private boolean	mIsBound;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(android.os.Message msg)
		{
			switch (msg.what)
			{
				case MessengerService.REPORT_MESSAGE_RECEIVED:
					Log.d("ConvearsationFragment", "REPORT_MESSAGE_RECEIVED");
					onMessageReceived(msg.getData());
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger				mMessenger			= new Messenger(new IncomingHandler());

	private IMessengerService	mIRemoteService;

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection	mConnection			= new ServiceConnection() {
														public void onServiceConnected(ComponentName className, IBinder service)
														{
															// This is called when the connection with the service has been
															// established, giving us the service object we can use to
															// interact with the service. We are communicating with our
															// service through an IDL interface, so get a client-side
															// representation of that from the raw service object.
															Log.d("ConversationListFragment", "register connection 1");
															mService = new Messenger(service);

															// We want to monitor the service for as long as we are
															// connected to it.
															try
															{
																Message msg = Message.obtain(null, MessengerService.REGISTER_CLIENT);
																msg.replyTo = mMessenger;
																mService.send(msg);
															}
															catch (RemoteException e)
															{
																Crashlytics.log(Log.DEBUG, "ConversationListFragment", "Can't connect to service "
																														+ e.getMessage());
																// In this case the service has crashed before we could even
																// do anything with it; we can count on soon being
																// disconnected (and then reconnected if it can be restarted)
																// so there is no need to do anything here.
															}
														}

														public void onServiceDisconnected(ComponentName className)
														{
															// This is called when the connection with the service has been
															// unexpectedly disconnected -- that is, its process crashed.
															mService = null;
														}
													};

	private IMessengerCallback	mCallback			= new IMessengerCallback.Stub() {

														@Override
														public void onPresenceChange(PPresence presence) throws RemoteException
														{
															if (getActivity() != null)
															{
																getActivity().runOnUiThread(new Runnable() {

																	@Override
																	public void run()
																	{
																		Log.d("ConversationListFragment", "onPresenceChange");
																		refreshRoster();
																		refreshPresenceLists();
																	}
																});
															}

														}

														@Override
														public void onRosterUpdated(List<PRosterEntry> roster) throws RemoteException
														{
															if (getActivity() != null)
															{
																getActivity().runOnUiThread(new Runnable() {

																	@Override
																	public void run()
																	{
																		Log.d("ConversationListFragment", "onRosterUpdated");
																		refreshRoster();
																		refreshPresenceLists();
																	}
																});
															}
														}
													};

	private ServiceConnection	mSecondConnection	= new ServiceConnection() {
														public void onServiceConnected(ComponentName className, IBinder service)
														{
															// This is called when the connection with the service has been
															// established, giving us the service object we can use to
															// interact with the service. We are communicating with our
															// service through an IDL interface, so get a client-side
															// representation of that from the raw service object.
															Log.d("ConversationListFragment", "register connection 2");
															mIRemoteService = IMessengerService.Stub.asInterface(service);

															try
															{
																mIRemoteService.registerCallback(mCallback);
															}
															catch (RemoteException e)
															{
																Log.d("ConversationListFragment", "registerCallback error");
															}

															try
															{
																roster = mIRemoteService.getRoster();
															}
															catch (RemoteException e)
															{
																Log.d("ConversationListFragment", "getRoster error");
															}
														}

														public void onServiceDisconnected(ComponentName className)
														{
															try
															{
																mIRemoteService.unregisterCallback(mCallback);
															}
															catch (RemoteException e)
															{
																Log.d("ConversationListFragment", "unregisterCallback error");
															}

															// This is called when the connection with the service has been
															// unexpectedly disconnected -- that is, its process crashed.
															mIRemoteService = null;
														}
													};

	public void connectToService()
	{
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		getActivity().getApplicationContext().bindService(new Intent(getActivity(), MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
		getActivity().getApplicationContext().bindService(new Intent(IMessengerService.class.getName()), mSecondConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService()
	{
		if (mIsBound)
		{
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (mService != null)
			{
				try
				{
					Message msg = Message.obtain(null, MessengerService.UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				}
				catch (RemoteException e)
				{
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			try
			{
				getActivity().getApplicationContext().unbindService(mConnection);
			}
			catch (IllegalArgumentException e)
			{

			}

			try
			{
				getActivity().getApplicationContext().unbindService(mSecondConnection);
			}
			catch (IllegalArgumentException e)
			{

			}
			mIsBound = false;
		}
	}

	private SharedPreferences.OnSharedPreferenceChangeListener	sharedPreferencesListener	= new SharedPreferences.OnSharedPreferenceChangeListener() {

																								@Override
																								public void onSharedPreferenceChanged(
																										SharedPreferences sharedPreferences,
																										String key)
																								{
																									if (key.equals(MessengerPreferences.PREFERENCE_NOTIFICATIONS))
																									{
																										try
																										{
																											mIRemoteService
																													.setNotificationsEnabled(MessengerPreferences
																															.areNotificationsEnabled());
																										}
																										catch (RemoteException e)
																										{
																											e.printStackTrace();
																										}
																									}
																									else if (key
																											.equals(MessengerPreferences.PREFERENCE_NOTIFICATIONS_RINGTONE))
																									{
																										Log.d("ConversationListFragment",
																												"onSharedPreferenceChanged ringtone "
																														+ MessengerPreferences
																																.getNotificationRingtone());
																										try
																										{
																											mIRemoteService
																													.setRingtone(MessengerPreferences
																															.getNotificationRingtone());
																										}
																										catch (RemoteException e)
																										{
																											e.printStackTrace();
																										}
																									}
																									else if (key
																											.equals(MessengerPreferences.PREFERENCE_NOTIFICATIONS_RINGTONE_URI))
																									{
																										Log.d("ConversationListFragment",
																												"onSharedPreferenceChanged ringtoneUri "
																														+ MessengerPreferences
																																.getNotificationRingtoneUri());
																										try
																										{
																											mIRemoteService
																													.setRingtoneUri(MessengerPreferences
																															.getNotificationRingtoneUri());
																										}
																										catch (RemoteException e)
																										{
																											e.printStackTrace();
																										}
																									}
																									else if (key
																											.equals(MessengerPreferences.PREFERENCE_NOTIFICATIONS_VIBRATE))
																									{
																										try
																										{
																											mIRemoteService
																													.setVibrateEnabled(MessengerPreferences
																															.isNotificationVibrationEnabled());
																										}
																										catch (RemoteException e)
																										{
																											e.printStackTrace();
																										}
																									}
																								}
																							};
}