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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import com.abewy.android.apps.klyph.core.fql.Friend;
import com.abewy.android.apps.klyph.core.fql.serializer.FriendDeserializer;
import com.abewy.android.apps.klyph.core.fql.serializer.FriendSerializer;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.messenger.MessengerPreferences;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.MultiObjectAdapter;
import com.abewy.android.apps.klyph.messenger.request.AsyncRequest.Query;
import com.abewy.android.apps.klyph.messenger.service.IMessengerCallback;
import com.abewy.android.apps.klyph.messenger.service.IMessengerService;
import com.abewy.android.apps.klyph.messenger.service.MessengerService;
import com.abewy.android.apps.klyph.messenger.service.PPresence;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;
import com.abewy.klyph.items.Header;
import com.crashlytics.android.Crashlytics;
import android.widget.SearchView;

public class SelectionFragment extends KlyphFragment implements SearchView.OnQueryTextListener
{
	public static interface SelectionCallback
	{
		public void onFriendSelected(Friend friend);
	}

	private SelectionCallback	listener;

	private List<PRosterEntry>	roster;
	private List<GraphObject>	friends;
	private List<GraphObject>	avList;
	private List<GraphObject>	unavList;
	
	private EditText editText;

	private ReadDataTask		readTask;
	private boolean				isStoreData	= false;

	public SelectionFragment()
	{
		setRequestType(Query.FRIENDS);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		setListAdapter(new MultiObjectAdapter(getListView()));

		defineEmptyText(R.string.empty_list_no_friend);

		getListView().setPadding(16, 0, 16, 0);
		getListView().setScrollBarStyle(AbsListView.SCROLLBARS_OUTSIDE_OVERLAY);
		getListView().setClipChildren(false);

		setListVisible(false);

		avList = new ArrayList<GraphObject>();
		unavList = new ArrayList<GraphObject>();
		roster = new ArrayList<PRosterEntry>();
		friends = new ArrayList<GraphObject>();
		
		editText = (EditText) view.findViewById(R.id.search_text_edit);
		editText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				populate();
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
				
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				
			}
		});

		setRequestType(Query.FRIENDS);

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	protected int getCustomLayout()
	{
		return R.layout.fragment_selection;
	}

	@Override
	protected void populate(List<GraphObject> data)
	{
		friends = data;

		if (!isStoreData)
			new StoreDataTask().execute();

		refreshPresenceLists();

		populate();
	}

	@Override
	public boolean onQueryTextSubmit(String query)
	{
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText)
	{
		if (newText.length() > 0)
		{
			getAdapter().getFilter().filter(newText);
		}
		else
		{
			populate();
		}

		return true;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		PRosterEntry p = (PRosterEntry) l.getItemAtPosition(position);

		Friend friend = new Friend();
		friend.setUid(p.getId());
		friend.setName(p.getName());

		listener.onFriendSelected(friend);
	}

	@Override
	public void load()
	{
		Log.d("SelectionFragment", "load");
		if (getView() != null)
		{
			if (isFirstLoad())
			{
				connectToService();

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
	
	public void disconnectFromService()
	{
		doUnbindService();
	}

	private void refreshPresenceLists()
	{
		avList = new ArrayList<GraphObject>();
		unavList = new ArrayList<GraphObject>();

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

		//Log.d("SelectionFragment", "roster " + roster.size() + " friends " + friends.size());
		if (roster.size() > 0 && friends.size() > 0)
		{
			Collections.sort(roster, new Comparator<PRosterEntry>() {

				@Override
				public int compare(PRosterEntry lhs, PRosterEntry rhs)
				{
					return lhs.name.compareTo(rhs.name);
				}
			});

			final int n = roster.size();
			for (int i = 0; i < n; i++)
			{
				PRosterEntry p = roster.get(i);
				String id = p.getId();

				int m = friends.size();
				for (int j = 0; j < m; j++)
				{
					Friend friend = (Friend) friends.get(j);

					if (id.equals(friend.getUid()))
					{
						p.setPic(friend.getPic());
						m--;
						j--;
						break;
					}
				}

				if (p.isAvailable())
				{
					avList.add(p);
				}
				else
				{
					unavList.add(p);
				}
			}
		}
		else if (friends.size() > 0)
		{
			for (GraphObject f : friends)
			{
				Friend friend = (Friend) f;

				PRosterEntry p = new PRosterEntry();
				p.name = friend.getName();
				p.setId(friend.getUid());
				p.setPic(friend.getPic());
				p.presence = PRosterEntry.UNAVAILABLE;

				unavList.add(p);
			}
		}
	}

	private void populate()
	{
		//Log.d("SelectionFragment", "avList " + avList.size());
		List<GraphObject> avList = this.avList;
		List<GraphObject> unavList = this.unavList;
		
		if (editText.getText().length() > 0)
		{
			String key = editText.getText().toString().toLowerCase();
			
			List<GraphObject> filteredAvList = new ArrayList<GraphObject>();
			List<GraphObject> filteredUnavList = new ArrayList<GraphObject>();
			
			int n = avList.size();
			for (int i = 0; i < n; i++)
			{
				PRosterEntry friend = (PRosterEntry) avList.get(i);
				if (friend.getName().toLowerCase().startsWith(key.toString()))
				{
					filteredAvList.add(friend);
				}
			}
			n = unavList.size();
			for (int i = 0; i < n; i++)
			{
				PRosterEntry friend = (PRosterEntry) unavList.get(i);
				if (friend.getName().toLowerCase().startsWith(key.toString()))
				{
					filteredUnavList.add(friend);
				}
			}
			
			avList = filteredAvList;
			unavList = filteredUnavList;
		}
		List<GraphObject> allList = new ArrayList<GraphObject>();

		if (avList.size() > 0)
		{
			Header h = new Header();
			h.setName(getString(R.string.online).toUpperCase());
			allList.add(h);
			allList.addAll(avList);
		}

		if (unavList.size() > 0)
		{
			Header h = new Header();
			h.setName(getString(R.string.offline).toUpperCase());
			allList.add(h);
			allList.addAll(unavList);
		}

		getAdapter().clear();
		
		super.populate(allList);

		getActivity().invalidateOptionsMenu();
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		if (activity instanceof SelectionCallback)
			listener = (SelectionCallback) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		listener = null;
	}

	@Override
	public void onDestroy()
	{
		Log.d("SelectionFragment", "onDestroy");
		doUnbindService();

		if (readTask != null)
		{
			readTask.cancel(true);
			readTask = null;
		}

		mConnection = null;
		listener = null;
		super.onDestroy();
		friends = null;
		avList = null;
		unavList = null;
		editText = null;
	}

	// ___ Service communication

	/** Messenger for communicating with service. */
	Messenger	mService	= null;
	/** Flag indicating whether we have called bind on the service. */
	boolean		mIsBound;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			Log.d("SelectionFragment", "handleMessage " + msg.what);
			switch (msg.what)
			{
				case MessengerService.REPORT_PRESENCE_CHANGED:
					Log.d("SelectionFragment", "REPORT_PRESENCE_CHANGED");
					refreshPresenceLists();
					populate();
					break;
				case MessengerService.REPORT_ROSTER_UPDATED:
				case MessengerService.REPORT_ROSTER_ENTRIES_ADDED:
				case MessengerService.REPORT_ROSTER_ENTRIES_DELETED:
					Log.d("SelectionFragment", "onRosterUpdate");
					/*
					 * try
					 * {
					 * roster = mIRemoteService.getRoster();
					 * }
					 * catch (RemoteException e)
					 * {
					 * e.printStackTrace();
					 * }
					 */
					refreshPresenceLists();
					populate();
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

	IMessengerService			mIRemoteService;

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
															Log.d("SelectionFragment", "register connection 1");
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
																		Log.d("SelectionFragment", "onPresenceChange");
																		refreshPresenceLists();
																		populate();
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
																		Log.d("SelectionFragment", "onRosterUpdated");
																		refreshPresenceLists();
																		populate();
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
															Log.d("SelectionFragment", "register connection 2");
															mIRemoteService = IMessengerService.Stub.asInterface(service);

															try
															{
																mIRemoteService.registerCallback(mCallback);
															}
															catch (RemoteException e)
															{
																Log.d("SelectionFragment", "registerCallback error");
																Crashlytics.log(Log.DEBUG, "SelectionFragment", "Can't connect to service " + e.getMessage());
															}

															try
															{
																roster = mIRemoteService.getRoster();
															}
															catch (RemoteException e)
															{
																Log.d("SelectionFragment", "getRoster error");
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
																Log.d("SelectionFragment", "unregisterCallback error");
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
		Log.d("SelectionFragment", "bind connection 1");
		getActivity().getApplicationContext().bindService(new Intent(getActivity(), MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.d("SelectionFragment", "bind connection 2");
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

	// ___ Async tasks

	private class ReadDataTask extends AsyncTask<Void, Void, List<GraphObject>>
	{

		@Override
		protected List<GraphObject> doInBackground(Void... params)
		{
			Log.d("SelectionFragment", "begin");
			String json = MessengerPreferences.getFriends();
			
			if (json == null)
				return null;

			JSONArray data;
			try
			{
				data = new JSONArray(json);
			}
			catch (JSONException e)
			{
				Log.d("SelectionFragment", e.getMessage());
				return null;
			}

			FriendDeserializer fd = new FriendDeserializer();
			List<GraphObject> list = fd.deserializeArray(data);
			Log.d("SelectionFragment", "end " + list.size());

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
						if (data.size() > 0)
						{
							isStoreData = true;
							populate(data);
							isStoreData = false;
							setNoMoreData(false);
						}

						setIsFirstLoad(false);

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
						SelectionFragment.super.load();
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
			FriendSerializer fs = new FriendSerializer();
			JSONArray json = fs.serializeArray(friends);
			String jsonString = json.toString();
			MessengerPreferences.setFriends(jsonString);

			return null;
		}

		@Override
		protected void onPostExecute(Void params)
		{

		}
	}
}
