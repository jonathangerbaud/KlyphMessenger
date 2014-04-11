package com.abewy.android.apps.klyph.messenger.fragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import util.EmojiUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import com.abewy.android.apps.klyph.core.KlyphSession;
import com.abewy.android.apps.klyph.core.fql.Friend;
import com.abewy.android.apps.klyph.core.fql.Message;
import com.abewy.android.apps.klyph.core.fql.MessageThread;
import com.abewy.android.apps.klyph.core.graph.GraphObject;
import com.abewy.android.apps.klyph.core.request.BaseAsyncRequest;
import com.abewy.android.apps.klyph.core.request.RequestError;
import com.abewy.android.apps.klyph.core.request.Response;
import com.abewy.android.apps.klyph.core.util.AlertUtil;
import com.abewy.android.apps.klyph.core.util.FacebookUtil;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.adapter.MultiObjectAdapter;
import com.abewy.android.apps.klyph.messenger.fragment.ConversationListFragment.ConversationListCallback;
import com.abewy.android.apps.klyph.messenger.request.AsyncRequest;
import com.abewy.android.apps.klyph.messenger.request.AsyncRequest.Query;
import com.abewy.android.apps.klyph.messenger.service.IMessengerCallback;
import com.abewy.android.apps.klyph.messenger.service.IMessengerService;
import com.abewy.android.apps.klyph.messenger.service.MessengerService;
import com.abewy.android.apps.klyph.messenger.service.PPresence;
import com.abewy.android.apps.klyph.messenger.service.PRosterEntry;
import com.abewy.net.ConnectionState;
import com.abewy.util.Android;
import com.abewy.util.PhoneUtil;
import com.crashlytics.android.Crashlytics;

public class ConversationFragment extends KlyphFragment
{
	public static interface ConversationCallback
	{
		public void onMessageSent(String threadId, String message);
	}

	private ConversationCallback	listener;
	private static final int		NO_PENDING_ACTION	= -1;
	private static final int		SEND_REMOVE_MESSAGE	= 1;

	private EditText				editText;
	private ImageButton				sendButton;
	private ImageButton				emojiButton;
	private GridView				emojiGrid;
	private String					recipientId;
	private String					recipientPicUrl;
	private String					mePicUrl;
	private int						pendingAction		= NO_PENDING_ACTION;
	private String					pendingId;
	private String					title;

	private boolean					canSendMessage		= false;

	private TextWatcher				textwatcher			= new TextWatcher() {
															@Override
															public void onTextChanged(CharSequence s, int start, int before, int count)
															{
																if (s.length() > 0)
																{
																	sendButton.setEnabled(canSendMessage);
																}
																else
																{
																	sendButton.setEnabled(false);
																}
															}

															@Override
															public void beforeTextChanged(CharSequence s, int start, int count, int after)
															{

															}

															@Override
															public void afterTextChanged(Editable s)
															{

															}
														};

	public ConversationFragment()
	{
		setRequestType(Query.MESSAGES);
	}

	private void setThread(MessageThread thread)
	{
		title = "";
		mePicUrl = null;
		recipientPicUrl = null;

		if (thread.isSingleUserConversation())
		{
			for (Friend friend : thread.getRecipients_friends())
			{
				String id = friend.getUid();
				if (id.equals(KlyphSession.getSessionUserId()))
				{
					mePicUrl = friend.getPic();
				}
				else
				{
					recipientPicUrl = friend.getPic();
					title = friend.getName();
					getActivity().setTitle(title);
				}
			}
		}
		else
		{
			List<String> names = new ArrayList<String>();
			for (Friend friend : thread.getRecipients_friends())
			{
				String id = friend.getUid();
				if (!id.equals(KlyphSession.getSessionUserId()))
				{
					names.add(friend.getFirst_name());
				}
			}
			title = StringUtils.join(names, ", ");
			getActivity().setTitle(title);
		}

		canSendMessage = thread.getRecipients().size() == 2;
		updateEditTextProperties();
	}
	
	public String getTitle()
	{
		return title;
	}

	private void noAvailableThread()
	{
		canSendMessage = true;
		updateEditTextProperties();
	}

	private void updateEditTextProperties()
	{
		editText.setHint(canSendMessage ? R.string.send_message : R.string.soon);
		editText.setEnabled(canSendMessage);
		emojiButton.setEnabled(canSendMessage);

		if (canSendMessage == false)
			emojiGrid.setVisibility(View.GONE);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		editText = (EditText) view.findViewById(R.id.send_text_edit);
		editText.addTextChangedListener(textwatcher);
		editText.clearFocus();

		sendButton = (ImageButton) view.findViewById(R.id.send_button);
		sendButton.setEnabled(false);
		sendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				sendMessage();
			}
		});

		emojiButton = (ImageButton) view.findViewById(R.id.emoji_button);
		emojiButton.setEnabled(false);
		emojiButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				toggleGridVisibility();
			}
		});

		emojiGrid = (GridView) view.findViewById(R.id.emoji_grid);
		emojiGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
			{
				// editText.removeTextChangedListener(textwatcher);
				editText.getText().append((String) emojiGrid.getAdapter().getItem(position) + " ");
				EmojiUtil.convertTextToEmoji(editText);
				// editText.addTextChangedListener(textwatcher);
				// sendButton.setEnabled(true);
			}
		});
		emojiGrid.setAdapter(new GridAdapter());

		setListAdapter(new MultiObjectAdapter(getListView()));

		registerForContextMenu(getListView());

		defineEmptyText(R.string.empty_list_no_message);

		getListView().setStackFromBottom(true);
		getListView().setDrawSelectorOnTop(false);
		getListView().setSelector(R.drawable.transparent_selector);
		// getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		setListVisible(false);

		setRequestType(Query.MESSAGES);

		setLoadingObjectAsFirstItem(true);

		super.onViewCreated(view, savedInstanceState);
	}

	public void loadThreadConversation(MessageThread thread)
	{
		title = "";
		final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

		if (thread.isSingleUserConversation())
		{
			for (String id : thread.getRecipients())
			{
				if (!id.equals(KlyphSession.getSessionUserId()))
				{
					removeSendMessage(id);
					break;
				}
			}
		}

		setElementId(thread.getThread_id());
		setRecipientFromThread(thread);
		setThread(thread);

		editText.setText("");
		emojiGrid.setVisibility(View.GONE);

		clearAndRefresh();
	}

	private void setRecipientFromThread(MessageThread thread)
	{
		if (thread.getRecipients().size() == 2)
		{
			for (String id : thread.getRecipients())
			{
				if (!id.equals(KlyphSession.getSessionUserId()))
				{
					recipientId = id;
					break;
				}
			}
		}
	}

	public void loadFriendConversation(String friendId)
	{
		Log.d("ConversationFragment", "loadFriendConversation " + friendId);
		final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

		removeSendMessage(friendId);

		recipientId = friendId;
		setElementId("0");

		editText.setText("");
		emojiGrid.setVisibility(View.GONE);

		canSendMessage = true;
		updateEditTextProperties();

		getAdapter().clear();
		setIsFirstLoad(true);
		setListVisible(false);

		new AsyncRequest(Query.THREAD_WITH_FRIEND, friendId, "", new BaseAsyncRequest.Callback() {

			@Override
			public void onComplete(Response response)
			{
				onRequestComplete(response);
			}
		}).execute();
	}

	private void removeSendMessage(String id)
	{
		if (mIRemoteService != null)
		{
			try
			{
				mIRemoteService.removeSavedMessages(id);
			}
			catch (RemoteException e)
			{

			}
		}
		else
		{
			pendingAction = SEND_REMOVE_MESSAGE;
			pendingId = id;
		}
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
				}
			});
		}
	}

	private void onRequestSuccess(List<GraphObject> result)
	{
		Log.d("ConversationFragment", "onRequestSuccess " + result.size());
		// Check if view is created
		if (getView() != null && getListView() != null)
		{
			if (result.size() > 0)
			{
				MessageThread thread = (MessageThread) result.get(0);
				Log.d("ConversationFragment", "get thread " + thread.getSnippet_author());
				setElementId(thread.getThread_id());
				setThread(thread);
				clearAndRefresh();
			}
			else
			{
				noAvailableThread();
				populate(new ArrayList<GraphObject>());
			}
		}
	}

	private void onRequestError(RequestError error)
	{
		Log.d("ConversationFragment", "onRequestError " + error.getMessage());
		noAvailableThread();
		populate(new ArrayList<GraphObject>());
	}

	@Override
	protected void populate(List<GraphObject> data)
	{
		if (isFirstLoad() == true)
		{
			for (GraphObject graphObject : data)
			{
				getAdapter().add(graphObject);
			}
		}
		else
		{
			int n = data.size();
			for (int i = 0; i < n; i++)
			{
				getAdapter().insert(data.get(i), i);
			}
		}

		// getAdapter().notifyDataSetChanged();*/
		endLoading();

		getListView().setSelection(data.size()-1);

		if (data.size() == 0)
			setNoMoreData(true);
		else
			setOffset(((Message) data.get(0)).getCreated_time());
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
	{
		if (!isLoading() && !isFirstLoad() && !hasNoMoreData())
		{
			boolean loadMore = firstVisibleItem == 0;

			if (loadMore)
			{
				refresh();
			}
		}
	}

	@Override
	protected int getCustomLayout()
	{
		return R.layout.fragment_conversation;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final Message message = (Message) getAdapter().getItem(position);

		List<String> list = new ArrayList<String>();

		int copyText = -1;
		int downloadImage = -1;

		String body = message.getBody();

		if (body.length() > 0)
		{
			list.add(getString(R.string.copy_text));
			copyText = list.size() - 1;

			Spannable spannable = new SpannableString(body);
			Linkify.addLinks(spannable, Linkify.WEB_URLS);

			URLSpan[] urls = spannable.getSpans(0, spannable.length(), URLSpan.class);
			if (urls.length > 0)
			{
				for (URLSpan urlSpan : urls)
				{
					list.add(urlSpan.getURL());
				}
			}
		}

		/*
		 * if (message.getAttachment() != null)
		 * {
		 * list.add(getString(R.string.download_image));
		 * downloadImage = list.size() - 1;
		 * }
		 */

		final int fcopyText = copyText;
		final int fdownloadImage = downloadImage;

		final String[] items = list.toArray(new String[0]);

		// For Api 8 to 10
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which)
			{
				if (which == fcopyText)
				{
					handleCopyTextAction(message);
				}
				else if (which == fdownloadImage)
				{
					handleDownloadAction(message);
				}
				else
				{
					handleUrlAction(items[which]);
				}
			}
		});
		builder.create().show();
	}

	@TargetApi(11)
	private void handleCopyTextAction(Message message)
	{
		if (Android.isMinAPI(11))
		{
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Message", message.getBody());
			clipboard.setPrimaryClip(clip);
		}
		else
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(message.getBody());
		}
	}

	private void handleDownloadAction(Message message)
	{

	}

	private void handleUrlAction(String url)
	{
		PhoneUtil.openURL(getActivity(), url);
	}

	private static class GridAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return EmojiUtil.EMOJIS.size();
		}

		@Override
		public Object getItem(int position)
		{
			return EmojiUtil.EMOJIS.keySet().toArray()[position];
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (convertView == null)
			{
				LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.item_emoji, parent, false);
			}

			ImageView imageView = (ImageView) convertView.findViewById(R.id.emoji_image);
			imageView.setImageResource(EmojiUtil.EMOJIS.get(getItem(position)));

			return convertView;
		}

	}

	// ___ Chat service features

	public void connectToService()
	{
		Log.d("ConversationFragment", "connectToService");
		getActivity().getApplicationContext().bindService(new Intent(getActivity(), MessengerService.class), mConnection, Context.BIND_AUTO_CREATE);
		getActivity().getApplicationContext().bindService(new Intent(IMessengerService.class.getName()), mSecondConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	public void disconnectFromService()
	{
		try
		{
			getActivity().getApplicationContext().unbindService(mConnection);
		}
		catch (IllegalArgumentException e)
		{

		}
	}

	private void sendMessage()
	{
		if (!ConnectionState.getInstance(getActivity()).isOnline())
		{
			AlertUtil.showAlert(getActivity(), R.string.error, R.string.request_connexion_error, R.string.ok);
			return;
		}

		String message = editText.getText().toString();

		Bundle bundle = new Bundle();
		bundle.putString("to", recipientId);
		bundle.putString("message", message);

		try
		{
			android.os.Message msg = android.os.Message.obtain(null, MessengerService.SEND_MSG);
			msg.setData(bundle);
			msg.replyTo = mMessenger;
			mService.send(msg);
		}
		catch (RemoteException e)
		{
			Log.d("ConversationFragment", "RemoteException can send message " + e.getMessage());
		}

		editText.setText("");

		String now = String.valueOf(new Date().getTime());
		now = now.substring(0, now.length() - 3);

		Message m = new Message();
		m.setAuthor_id(KlyphSession.getSessionUserId());
		m.setAuthor_name(KlyphSession.getSessionUserName());
		m.setAuthor_pic(mePicUrl != null && mePicUrl.length() > 0 ? mePicUrl
				: FacebookUtil.getProfilePictureURLForId(KlyphSession.getSessionUserId()));
		m.setCreated_time(now);
		m.setBody(message);

		getAdapter().add(m);
		getAdapter().notifyDataSetChanged();
		getListView().setSelection(getAdapter().getCount() - 1);

		listener.onMessageSent(getElementId(), message);
	}

	private void onMessageReceived(Bundle data)
	{
		String uid = data.getString("participant");

		if (uid.equals(recipientId))
		{
			String date = data.getString("date");
			date = date.substring(0, date.length() - 3);

			Message msg = new Message();
			msg.setAuthor_id(uid);
			msg.setAuthor_name(data.getString("from"));
			msg.setCreated_time(date);
			msg.setAuthor_pic(recipientPicUrl != null && recipientPicUrl.length() > 0 ? recipientPicUrl : FacebookUtil.getProfilePictureURLForId(uid));
			msg.setBody(data.getString("body"));

			getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			getAdapter().add(msg);
			getAdapter().notifyDataSetChanged();
			getListView().setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
		}
	}

	private void toggleGridVisibility()
	{
		emojiGrid.setVisibility(emojiGrid.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
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
																		Log.d("ConversationFragment", "onPresenceChange");
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
																		Log.d("ConversationFragment", "onRosterUpdated");
																	}
																});
															}
														}
													};
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
															Log.d("ConvearsationFragment", "register connection 1");
															mService = new Messenger(service);

															// We want to monitor the service for as long as we are
															// connected to it.
															try
															{
																android.os.Message msg = android.os.Message.obtain(null,
																		MessengerService.REGISTER_CLIENT);
																msg.replyTo = mMessenger;
																mService.send(msg);
															}
															catch (RemoteException e)
															{
																// In this case the service has crashed before we could even
																// do anything with it; we can count on soon being
																// disconnected (and then reconnected if it can be restarted)
																// so there is no need to do anything here.
																Crashlytics.log(Log.DEBUG, "ConversationFragment",
																		"Can't connect to service " + e.getMessage());
															}
														}

														public void onServiceDisconnected(ComponentName className)
														{
															// This is called when the connection with the service has been
															// unexpectedly disconnected -- that is, its process crashed.
															mService = null;
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
															Log.d("ConversationFragment", "register connection 2");
															mIRemoteService = IMessengerService.Stub.asInterface(service);

															try
															{
																mIRemoteService.registerCallback(mCallback);
															}
															catch (RemoteException e)
															{
																Log.d("ConversationFragment", "registerCallback error");
															}

															if (pendingAction == SEND_REMOVE_MESSAGE)
															{
																try
																{
																	mIRemoteService.removeSavedMessages(pendingId);
																}
																catch (RemoteException e)
																{

																}
																finally
																{
																	pendingAction = NO_PENDING_ACTION;
																	pendingId = null;
																}
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
																Log.d("ConversationFragment", "unregisterCallback error");
															}

															// This is called when the connection with the service has been
															// unexpectedly disconnected -- that is, its process crashed.
															mIRemoteService = null;
														}
													};

	void doBindService()
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
					android.os.Message msg = android.os.Message.obtain(null, MessengerService.UNREGISTER_CLIENT);
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
	
	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		if (activity instanceof ConversationListCallback)
			listener = (ConversationCallback) activity;
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
		Log.d("ConversationFragment", "onDestroy");
		doUnbindService();

		mConnection = null;
		mSecondConnection = null;
		mIRemoteService = null;
		mService = null;
		// mCallback = null;
		super.onDestroy();
		editText = null;
		sendButton = null;
		emojiButton = null;
		emojiGrid = null;
		listener = null;
	}

}