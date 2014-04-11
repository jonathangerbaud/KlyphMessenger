package com.abewy.android.apps.klyph.messenger.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.DefaultMessageEventRequestListener;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.MessageEventNotificationListener;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.search.UserSearch;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.Html;
import android.util.Log;
import com.abewy.android.apps.klyph.core.imageloader.FakeImageLoaderListener;
import com.abewy.android.apps.klyph.core.imageloader.ImageLoader;
import com.abewy.android.apps.klyph.core.util.FacebookUtil;
import com.abewy.android.apps.klyph.messenger.KlyphMessengerNotification;
import com.abewy.android.apps.klyph.messenger.MessengerBundleExtras;
import com.abewy.android.apps.klyph.messenger.MessengerPreferences;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.app.MainActivity;
import com.abewy.android.apps.klyph.messenger.chat.SASLXFacebookPlatformMechanism;
import com.abewy.net.ConnectionState;
import com.facebook.Session;
import com.squareup.picasso.Picasso.LoadedFrom;

public class MessengerService extends Service
{
	private static final String								TAG								= "MessengerService";

	private static MessengerService							instance;

	private SmackAndroid									smack;
	private ArrayList<Messenger>							mClients						= new ArrayList<Messenger>();
	private final RemoteCallbackList<IMessengerCallback>	callbacks						= new RemoteCallbackList<IMessengerCallback>();
	private List<Chat>										chats;
	private List<PRosterEntry>								roster;
	private RosterListener									rosterListener;
	private Thread											presenceRunnable;
	private String											selectedRecipient;

	private boolean											notificationsEnabled;
	private String											ringtone;
	private String											ringtoneUri;
	private boolean											vibrateEnabled;

	private LinkedHashMap<String, Object>					pendingActions;
	private ReconnectionManager								reconnectionManager;
	private boolean											isLoggedIn						= false;

	/** Holds last value set by a client. */
	int														mValue							= 0;

	public static final int									REGISTER_CLIENT					= 1;
	public static final int									UNREGISTER_CLIENT				= 2;
	public static final int									SEND_MSG						= 3;

	public static final int									REPORT_CONNECTED				= 1;
	public static final int									REPORT_DISCONNECTED				= 2;
	public static final int									REPORT_PRESENCE_CHANGED			= 4;
	public static final int									REPORT_ROSTER_UPDATED			= 5;
	public static final int									REPORT_ROSTER_ENTRIES_ADDED		= 6;
	public static final int									REPORT_ROSTER_ENTRIES_DELETED	= 7;
	public static final int									REPORT_MESSAGE_RECEIVED			= 8;

	private static final String								ACTION_SEND_MSG					= "actionSendMsg";

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case REGISTER_CLIENT:
				{
					Log.d(TAG, "REGISTER_CLIENT");
					if (mClients.indexOf(msg.replyTo) == -1)
						mClients.add(msg.replyTo);
					break;
				}
				case UNREGISTER_CLIENT:
				{
					Log.d(TAG, "UNREGISTER_CLIENT");
					if (mClients != null)
						mClients.remove(msg.replyTo);
					break;
				}
				case SEND_MSG:
				{
					sendMessageTo((Bundle) msg.getData());
					break;
				}
				default:
				{
					super.handleMessage(msg);
				}
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger			mMessenger	= new Messenger(new IncomingHandler());

	private XMPPConnection	connection;

	public MessengerService()
	{
		Log.d(TAG, "new instance");

		if (instance != null)
			Log.d(TAG, "instance is already defined !");
	}

	@Override
	public void onCreate()
	{
		Log.d(TAG, "onCreate");

		if (instance != null)
			Log.d(TAG, "instance is already defined !");

		instance = this;

		notificationsEnabled = MessengerPreferences.areNotificationsEnabled();
		ringtone = MessengerPreferences.getNotificationRingtone();
		ringtoneUri = MessengerPreferences.getNotificationRingtoneUri();
		vibrateEnabled = MessengerPreferences.isNotificationVibrationEnabled();

		chats = new ArrayList<Chat>();
		roster = new ArrayList<PRosterEntry>();
		savedMessages = new ArrayList<org.jivesoftware.smack.packet.Message>();
		pendingActions = new LinkedHashMap<String, Object>();

		configure(ProviderManager.getInstance());

		Connection.addConnectionCreationListener(new ConnectionCreationListener() {
			public void connectionCreated(Connection connection)
			{
				reconnectionManager = new ReconnectionManager(connection, ConnectionState.getInstance(MessengerService.this).isOnline());
				connection.addConnectionListener(reconnectionManager);
			}
		});

		registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent)
			{
				Log.i(TAG, "Connection status change to " + ConnectionState.getInstance(MessengerService.this).isOnline());
				onConnectivityChange();
			}
		}, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

		connect();
	}

	private void onConnectivityChange()
	{
		if (!isLoggedIn && isInternetActive() && !connection.isConnected())
			launchConnection();
		else if (reconnectionManager != null)
			reconnectionManager.setInternetIsActive(isInternetActive());
	}

	public static boolean isRunning()
	{
		return instance != null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.d(TAG, "onStartCommand");
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	private void addPendingAction(String action, Object data)
	{
		pendingActions.put(action, data);

		// if (ConnectionState.getInstance(this).isOnline() && !isConnecting)
		// connect();
	}

	private void executePendingActions()
	{
		if (isInternetActive() && connection.isConnected())
		{
			for (String key : pendingActions.keySet())
			{
				if (key.equals(ACTION_SEND_MSG))
				{
					sendMessageTo((Bundle) pendingActions.get(key));
				}
			}

			pendingActions = new LinkedHashMap<String, Object>();
		}
	}

	private void connect()
	{
		Log.d(TAG, "connect");
		if (smack == null)
		{
			smack = SmackAndroid.init(this);
			smack = SmackAndroid.init(this);
		}

		// Connection.DEBUG_ENABLED = true;

		SASLAuthentication.registerSASLMechanism("X-FACEBOOK-PLATFORM", SASLXFacebookPlatformMechanism.class);
		SASLAuthentication.supportSASLMechanism("X-FACEBOOK-PLATFORM", 0);

		ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 5222);

		config.setSASLAuthenticationEnabled(true);
		config.setSecurityMode(SecurityMode.required);
		config.setRosterLoadedAtLogin(true);
		config.setCompressionEnabled(false);
		// config.setDebuggerEnabled(true);
		config.setReconnectionAllowed(false);
		config.setSendPresence(true);
		config.setTruststoreType("AndroidCAStore");
		config.setTruststorePassword(null);
		config.setTruststorePath(null);

		connection = new XMPPConnection(config);

		Roster roster = connection.getRoster();
		rosterListener = new RosterListener() {

			@Override
			public void presenceChanged(Presence presence)
			{
				onPresenceChange(presence);
			}

			@Override
			public void entriesUpdated(Collection<String> arg0)
			{
				onEntriesUpdated();
			}

			@Override
			public void entriesDeleted(Collection<String> arg0)
			{
				onEntriesDeleted();
			}

			@Override
			public void entriesAdded(Collection<String> arg0)
			{
				onEntriesAdded();
			}
		};
		roster.addRosterListener(rosterListener);

		if (Session.getActiveSession() == null)
			Session.openActiveSessionFromCache(this);

		// new ConnectionTask().execute(connection);
		if (isInternetActive())
			launchConnection();
	}

	private Thread	reconnectionThread;

	synchronized private void launchConnection()
	{
		// Since there is no thread running, creates a new one to attempt
		// the reconnection.
		// avoid to run duplicated reconnectionThread -- fd: 16/09/2010
		if (reconnectionThread != null && reconnectionThread.isAlive())
			return;

		Log.d(TAG, "MessengerService launchConnection");
		reconnectionThread = new Thread() {

			private int	numAttemps	= 0;

			/**
			 * The process will try the reconnection until the connection succeed or the user
			 * cancell it
			 */
			public void run()
			{
				// The process will try to reconnect until the connection is established or
				// the user cancel the reconnection process {@link Connection#disconnect()}
				while (isInternetActive() == true && !connection.isConnected())
				{
					// Wait 3 seconds before trying to reconnect again
					int remainingSeconds = numAttemps == 0 ? 0 : 3;
					// Sleep until we're ready for the next reconnection attempt. Notify
					// listeners once per second about how much time remains before the next
					// reconnection attempt.
					while (isInternetActive() == true && !connection.isConnected() && remainingSeconds > 0)
					{
						try
						{
							Thread.sleep(1000);
							remainingSeconds--;
							// ReconnectionManager.this.notifyAttemptToReconnectIn(remainingSeconds);
						}
						catch (InterruptedException e1)
						{
							e1.printStackTrace();
							// Notify the reconnection has failed
							// ReconnectionManager.this.notifyReconnectionFailed(e1);
						}
					}

					XMPPException exception = null;

					// Makes a reconnection attempt
					try
					{
						if (isInternetActive() == true)
						{
							Log.d(TAG, "MessengerService Reconnecting");
							connection.connect();
							connection.login(getString(R.string.app_id), Session.getActiveSession().getAccessToken());
						}
					}
					catch (XMPPException e)
					{
						exception = e;
						// Fires the failed reconnection notification
						// ReconnectionManager.this.notifyReconnectionFailed(e);
					}

					if (exception == null)
					{
						onConnectionSuccess();
						break;
					}

					numAttemps++;
				}
			}
		};
		// reconnectionThread.setName("Smack Reconnection Manager");
		reconnectionThread.setDaemon(true);
		reconnectionThread.start();
	}

	private boolean isInternetActive()
	{
		return ConnectionState.getInstance(this).isOnline();
	}

	protected void onConnectionSuccess()
	{
		Log.d(TAG, "onConnectionSuccess");
		DeliveryReceiptManager.getInstanceFor(connection).enableAutoReceipts();
		isLoggedIn = true;
		//listenMsgEvents();
		listenNewChats();
		executePendingActions();
	}

	private void refreshRoster()
	{
		roster = getRosterEntries();
	}

	private ArrayList<PRosterEntry> getRosterEntries()
	{
		Log.d(TAG, "getRosterEntries");
		if (connection != null && connection.isConnected() == true)
		{
			Roster roster = connection.getRoster();
			Collection<RosterEntry> entries = roster.getEntries();

			ArrayList<PRosterEntry> data = new ArrayList<PRosterEntry>();

			for (RosterEntry rosterEntry : entries)
			{
				PRosterEntry re = new PRosterEntry();
				Presence p = roster.getPresence(rosterEntry.getUser());

				re.name = rosterEntry.getName();
				re.user = rosterEntry.getUser();
				re.presence = p.getType().name();

				data.add(re);
			}
			Log.d(TAG, "getRosterEntries " + data.size());
			return data;
		}
		else
		{
			return new ArrayList<PRosterEntry>();
		}
	}

	private MessageEventManager messageEventManager;
	private void listenMsgEvents()
	{
		messageEventManager = new MessageEventManager(connection);

		messageEventManager.addMessageEventRequestListener(new DefaultMessageEventRequestListener() {
			public void deliveredNotificationRequested(String from, String packetID, MessageEventManager messageEventManager)
			{
				super.deliveredNotificationRequested(from, packetID, messageEventManager);
				// DefaultMessageEventRequestListener automatically responds that the message was delivered when receives this request
				Log.d("MessengerService", "Delivered Notification Requested (" + from + ", " + packetID + ")");
			}

			public void displayedNotificationRequested(String from, String packetID, MessageEventManager messageEventManager)
			{
				super.displayedNotificationRequested(from, packetID, messageEventManager);
				// Send to the message's sender that the message was displayed
				messageEventManager.sendDisplayedNotification(from, packetID);
				Log.d("MessengerService", "displayedNotificationRequested (" + from + ", " + packetID + ")");
			}

			public void composingNotificationRequested(String from, String packetID, MessageEventManager messageEventManager)
			{
				super.composingNotificationRequested(from, packetID, messageEventManager);
				// Send to the message's sender that the message's receiver is composing a reply
				messageEventManager.sendComposingNotification(from, packetID);
				Log.d("MessengerService", "composingNotificationRequested (" + from + ", " + packetID + ")");
			}

			public void offlineNotificationRequested(String from, String packetID, MessageEventManager messageEventManager)
			{
				super.offlineNotificationRequested(from, packetID, messageEventManager);
				// The XMPP server should take care of this request. Do nothing.
				Log.d("MessengerService", "Offline Notification Requested (" + from + ", " + packetID + ")");
			}
		});
		
		messageEventManager.addMessageEventNotificationListener(new MessageEventNotificationListener() {
			
			@Override
			public void offlineNotification(String arg0, String arg1)
			{
				Log.d("MessengerService", "offlineNotification (" + arg0 + ", " + arg1 + ")");
			}
			
			@Override
			public void displayedNotification(String arg0, String arg1)
			{
				Log.d("MessengerService", "displayedNotification (" + arg0 + ", " + arg1 + ")");
			}
			
			@Override
			public void deliveredNotification(String arg0, String arg1)
			{
				Log.d("MessengerService", "deliveredNotification (" + arg0 + ", " + arg1 + ")");
			}
			
			@Override
			public void composingNotification(String arg0, String arg1)
			{
				Log.d("MessengerService", "composingNotification (" + arg0 + ", " + arg1 + ")");
			}
			
			@Override
			public void cancelledNotification(String arg0, String arg1)
			{
				Log.d("MessengerService", "cancelledNotification (" + arg0 + ", " + arg1 + ")");
			}
		});
		
		DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(new ReceiptReceivedListener() {
			
			@Override
			public void onReceiptReceived(String arg0, String arg1, String arg2)
			{
				Log.d("MessengerService", "onReceiptReceived (" + arg0 + ", " + arg1 + ", " + arg2 + ")");
			}
		});
	}

	private void listenNewChats()
	{
		connection.getChatManager().addChatListener(new ChatManagerListener() {
			@Override
			public void chatCreated(Chat chat, boolean createdLocally)
			{
				chats.add(chat);
				Log.d(TAG, "New chat created " + chat.getThreadID() + " " + createdLocally + " " + chat.getParticipant());

				if (!createdLocally)
					chat.addMessageListener(new MessageListener() {

						@Override
						public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message)
						{
							onMessageReceived(chat, message);
						}
					});
			}
		});
	}

	private Chat createChat(String recipient)
	{
		ChatManager chatManager = connection.getChatManager();
		Chat chat = chatManager.createChat(recipient /* "-1320153319@chat.facebook.com" */, new MessageListener() {

			@Override
			public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message)
			{
				onMessageReceived(chat, message);
			}
		});
		
		chats.add(chat);
		Log.d(TAG, "Create chat thread id " + chat.getThreadID());
		return chat;
	}

	private void onMessageReceived(Chat chat, org.jivesoftware.smack.packet.Message message)
	{
		if (message.getBody() != null)
		{
			Log.d(TAG, "processMessage " + chat.getThreadID() + " " + message.getBody() + " " + message.getTo());
			onMessageReceived(message);
			messageEventManager.sendDeliveredNotification(message.getFrom(), message.getPacketID());
		}
	}

	private void sendMessageTo(Bundle bundle)
	{
		if (!connection.isConnected())
		{
			addPendingAction(ACTION_SEND_MSG, bundle);
		}
		else
		{
			String to = bundle.getString("to");
			String message = bundle.getString("message");

			to = "-" + to + "@chat.facebook.com";
			Log.d(TAG, "sendMessage to " + to + " " + message);
			Chat toChat = null;

			for (Chat chat : chats)
			{
				if (chat.getParticipant().equals(to))
				{
					toChat = chat;
				}
			}

			if (toChat == null)
			{
				toChat = createChat(to);
			}
			
			org.jivesoftware.smack.packet.Message msg = new org.jivesoftware.smack.packet.Message();
			msg.setBody(message);
			msg.setTo(to);
			msg.setType(org.jivesoftware.smack.packet.Message.Type.chat);
			msg.setThread(toChat.getThreadID());
			
			// Add to the message all the notifications requests (offline, delivered, displayed, composing)
		    MessageEventManager.addNotificationsRequests(msg, true, true, true, true);
		    DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
		    
			try
			{
				toChat.sendMessage(msg);
			}
			catch (XMPPException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void onPresenceChange(Presence presence)
	{
		if (presenceRunnable == null)
		{
			presenceRunnable = new Thread(new Runnable() {

				@Override
				public void run()
				{
					try
					{
						synchronized (this)
						{
							wait(1000);
						}
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					broadcastPresenceChange();
				}
			});

			presenceRunnable.start();
		}
	}

	private void broadcastPresenceChange()
	{
		refreshRoster();
		onRosterUpdated();
		presenceRunnable = null;

		/*
		 * PPresence pp = new PPresence();
		 * pp.from = presence.getFrom();
		 * pp.type = presence.getType().name();
		 * 
		 * final int N = callbacks.beginBroadcast();
		 * Log.d(TAG, "callback count " + N + " roster count " + roster.size());
		 * for (int i = 0; i < N; i++)
		 * {
		 * try
		 * {
		 * callbacks.getBroadcastItem(i).onPresenceChange(pp);
		 * }
		 * catch (RemoteException e)
		 * {
		 * Log.d(TAG, "RemoteException " + e.getMessage());
		 * // Thef RemoteCallbackList will take care of removing the dead object for us.
		 * }
		 * }
		 * callbacks.finishBroadcast();
		 */
	}

	private void onEntriesUpdated()
	{
		onRosterUpdated();
	}

	private void onEntriesDeleted()
	{
		onRosterUpdated();
	}

	private void onEntriesAdded()
	{
		onRosterUpdated();
	}

	private void onRosterUpdated()
	{
		refreshRoster();

		final int N = callbacks.beginBroadcast();
		for (int i = 0; i < N; i++)
		{
			try
			{
				callbacks.getBroadcastItem(i).onRosterUpdated(roster);
			}
			catch (RemoteException e)
			{
				// Thef RemoteCallbackList will take care of removing the dead object for us.
			}
		}
		callbacks.finishBroadcast();
	}

	private void onMessageReceived(org.jivesoftware.smack.packet.Message message)
	{
		Bundle data = new Bundle();

		String uid = getUidFromXmppId(message.getFrom());

		data.putString("participant", uid);
		data.putString("body", message.getBody());
		data.putString("from", message.getFrom());
		data.putString("to", message.getTo());
		data.putString("date", String.valueOf(new Date().getTime()));

		sendMsg(REPORT_MESSAGE_RECEIVED, data);

		sendNotification(message);
	}

	private void sendMsg(int msg)
	{
		sendMsgToClients(Message.obtain(null, msg));
	}

	private void sendMsg(int msg, Bundle bundle)
	{
		Message message = Message.obtain(null, msg);
		message.setData(bundle);

		sendMsgToClients(message);
	}

	private void sendMsgToClients(Message msg)
	{

		for (int i = mClients.size() - 1; i >= 0; i--)
		{
			try
			{
				mClients.get(i).send(msg);
			}
			catch (RemoteException e)
			{
				// The client is dead. Remove it from the list;
				// we are going through the list from back to front
				// so this is safe to do inside the loop.
				mClients.remove(i);
			}
		}
	}

	private boolean noBinding()
	{
		int n = mClients.size() + callbacks.getRegisteredCallbackCount();
		return n == 0;
	}

	private List<org.jivesoftware.smack.packet.Message>	savedMessages;

	private void sendNotification(org.jivesoftware.smack.packet.Message message)
	{
		if (notificationsEnabled)
		{
			String from = getUidFromXmppId(message.getFrom());
			Log.d(TAG, "sendNotification " + from + " " + selectedRecipient);
			Log.d(TAG, "sendNotification " + ringtone + " " + ringtoneUri);

			if (noBinding() || (selectedRecipient == null || !from.equals(selectedRecipient)))
			{
				savedMessages.add(message);
				sendNotification();
			}
		}
	}

	private void sendNotification()
	{
		final Builder builder = KlyphMessengerNotification.getBuilder(this, true);
		Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
		notifyIntent.setClass(getApplicationContext(), MainActivity.class);

		int n = savedMessages.size();
		org.jivesoftware.smack.packet.Message message = savedMessages.get(savedMessages.size() - 1);
		String uid = getUidFromXmppId(message.getFrom());
		
		String name = getNameForId(message.getFrom());
		builder.setTicker(getString(R.string.new_message_from, getNameForId(message.getFrom()), message.getBody()));
		
		if (n > 1)
		{
			if (areAllSavedMessageFromSameUser())
			{
				builder.setContentTitle(name);
				builder.setContentText(getString(R.string.new_messages, n));
				
				List<String> lines = new ArrayList<String>();
				for (org.jivesoftware.smack.packet.Message msg : savedMessages)
				{
					lines.add(msg.getBody());
				}
				
				KlyphMessengerNotification.setInboxStyle(builder, name, lines);
			}
			else
			{
				builder.setContentTitle(getString(R.string.new_messages, n));
				
				List<String> lines = new ArrayList<String>();
				List<String> names = new ArrayList<String>();
				for (org.jivesoftware.smack.packet.Message msg : savedMessages)
				{
					names.add(getNameForId(msg.getFrom()));
					lines.add(Html.fromHtml(getString(R.string.new_message_from, getNameForId(msg.getFrom()), msg.getBody())).toString());
				}
				
				builder.setContentText(StringUtils.join(names, ","));
				
				KlyphMessengerNotification.setInboxStyle(builder, name, lines);
			}
		}
		else
		{
			builder.setContentTitle(name);
			builder.setContentText(message.getBody());
		}

		builder.setNumber(savedMessages.size());
		builder.setSubText(getString(R.string.app_name));

		// Override builder defaults flags because preferences changes
		// don't work in inter process
		int defaults = 0;

		if (ringtone != null && ringtone.equals("default"))
		{
			defaults |= android.app.Notification.DEFAULT_SOUND;
		}
		else if (ringtoneUri == null)
		{
			builder.setSound(null);
		}
		else
		{
			builder.setSound(Uri.parse(ringtoneUri));
		}

		if (vibrateEnabled == true)
			defaults |= android.app.Notification.DEFAULT_VIBRATE;

		builder.setDefaults(defaults);

		// Gets a PendingIntent containing the entire back stack
		PendingIntent intent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		builder.setContentIntent(intent);

		// KlyphMessengerNotification.sendNotification(MessengerService.this, builder);

		ImageLoader.loadImage(FacebookUtil.getLargeProfilePictureURLForId(uid), new FakeImageLoaderListener() {

			@Override
			public void onPrepareLoad(Drawable arg0)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void onBitmapLoaded(Bitmap image, LoadedFrom arg1)
			{
				Log.d(TAG, "onLoadingComplete");
				builder.setLargeIcon(image);
				KlyphMessengerNotification.sendNotification(MessengerService.this, builder);
			}

			@Override
			public void onBitmapFailed(Drawable arg0)
			{
				Log.d(TAG, "onLoadingFailed");
				KlyphMessengerNotification.sendNotification(MessengerService.this, builder);
			}
		});
	}

	private String getUidFromXmppId(String xmppId)
	{
		return xmppId.substring(1, xmppId.indexOf("@"));
	}

	private boolean areAllSavedMessageFromSameUser()
	{
		String id = null;

		for (org.jivesoftware.smack.packet.Message message : savedMessages)
		{
			if (id == null)
			{
				id = message.getFrom();
				continue;
			}

			if (!id.equals(message.getFrom()))
			{
				return false;
			}
		}

		return true;
	}

	private String getNameForId(String id)
	{
		for (PRosterEntry entry : getRosterEntries())
		{
			if (entry.user.equals(id))
			{
				return entry.name;
			}
		}

		return "";
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "onDestroy");
		if (connection != null)
		{
			if (connection.getRoster() != null)
				connection.getRoster().removeRosterListener(rosterListener);

			// connection.removeConnectionListener(connectionListener);

			new DeconnectionTask().execute(connection);
		}

		if (smack != null)
			smack.onDestroy();

		smack = null;
		connection = null;
		rosterListener = null;
		mClients = null;
		callbacks.kill();
		chats = null;
		roster = null;

		instance = null;
	}

	/**
	 * When binding to the service, we return an interface to our messenger
	 * for sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		Log.d(TAG, "onBind " + intent.getAction());

		// if (!connection.isConnected())
		// connect();

		if (IMessengerService.class.getName().equals(intent.getAction()))
		{
			return mBinder;
		}

		return mMessenger.getBinder();
	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IMessengerService.Stub	mBinder	= new IMessengerService.Stub() {

														@Override
														public List<PRosterEntry> getRoster() throws RemoteException
														{
															Log.d(TAG, "getRoster callback " + roster.size());
															return roster;
														}

														@Override
														public void setSelectedRecipient(String id) throws RemoteException
														{
															Log.d(TAG, "setRecipientSelected");
															selectedRecipient = id;

														}

														@Override
														public void setNoSelectedRecipient() throws RemoteException
														{
															Log.d(TAG, "setNoSelectedRecipient");
															selectedRecipient = null;
														}

														@Override
														public void registerCallback(IMessengerCallback cb) throws RemoteException
														{
															if (cb != null)
																callbacks.register(cb);
														}

														@Override
														public void unregisterCallback(IMessengerCallback cb) throws RemoteException
														{
															if (cb != null)
																callbacks.unregister(cb);
														}

														@Override
														public void clearSavedMessages() throws RemoteException
														{
															savedMessages = new ArrayList<org.jivesoftware.smack.packet.Message>();
														}

														@Override
														public void removeSavedMessages(String id) throws RemoteException
														{
															int n = savedMessages.size();
															for (int i = 0; i < n; i++)
															{
																org.jivesoftware.smack.packet.Message message = savedMessages.get(i);
																if (getUidFromXmppId(message.getFrom()).equals(id))
																{
																	savedMessages.remove(message);
																	i--;
																	n--;
																}
															}
														}

														@Override
														public void setNotificationsEnabled(boolean enabled) throws RemoteException
														{
															notificationsEnabled = enabled;
														}

														@Override
														public void setRingtone(String ringtone) throws RemoteException
														{
															Log.d(TAG, "setRingtone" + ringtone);
															MessengerService.this.ringtone = ringtone;
														}

														@Override
														public void setRingtoneUri(String uri) throws RemoteException
														{
															Log.d(TAG, "setRingtoneUri" + uri);
															ringtoneUri = uri;
														}

														@Override
														public void setVibrateEnabled(boolean enabled) throws RemoteException
														{
															vibrateEnabled = enabled;
														}
													};

	class DeconnectionTask extends AsyncTask<XMPPConnection, Void, Void>
	{
		protected Void doInBackground(XMPPConnection... connections)
		{
			connections[0].disconnect();
			return null;
		}

		protected void onPostExecute(Void param)
		{}
	}

	public void configure(ProviderManager pm)
	{

		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try
		{
			pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
		}
		catch (ClassNotFoundException e)
		{
			Log.w("TestClient", "Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());

		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());

		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());

		// Version
		try
		{
			pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
		}
		catch (ClassNotFoundException e)
		{
			// Not sure what's happening here.
		}

		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());

		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
		pm.addIQProvider("command", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new AdHocCommandDataProvider.SessionExpiredError());
	}

	/**
	 * Derived class from Asmack
	 * Reconnect only when there is an available internet connection
	 * 
	 * Handles the automatic reconnection process. Every time a connection is dropped without
	 * the application explictly closing it, the manager automatically tries to reconnect to
	 * the server.
	 * <p>
	 * 
	 * The reconnection mechanism will try to reconnect periodically:
	 * <ol>
	 * <li>For the first minute it will attempt to connect once every ten seconds.
	 * <li>For the next five minutes it will attempt to connect once a minute.
	 * <li>If that fails it will indefinitely try to connect once every five minutes.
	 * </ol>
	 * 
	 * @author Francisco Vives
	 */
	public static class ReconnectionManager implements ConnectionListener
	{
		// Holds the connection to the server
		private Connection	connection;
		private Thread		reconnectionThread;
		private boolean		isInternetActive;

		// Holds the state of the reconnection
		boolean				done	= false;

		private ReconnectionManager(Connection connection, boolean internetIsActive)
		{
			this.connection = connection;
		}

		/**
		 * Returns true if the reconnection mechanism is enabled.
		 * 
		 * @return true if automatic reconnections are allowed.
		 */
		private boolean isReconnectionAllowed()
		{
			return !done && !connection.isConnected() && isInternetActive;
		}

		private void setInternetIsActive(boolean active)
		{
			isInternetActive = active;

			if (isReconnectionAllowed())
				reconnect();
		}

		/**
		 * Starts a reconnection mechanism if it was configured to do that.
		 * The algorithm is been executed when the first connection error is detected.
		 * <p/>
		 * The reconnection mechanism will try to reconnect periodically in this way:
		 * <ol>
		 * <li>First it will try 6 times every 10 seconds.
		 * <li>Then it will try 10 times every 1 minute.
		 * <li>Finally it will try indefinitely every 5 minutes.
		 * </ol>
		 */
		synchronized protected void reconnect()
		{
			Log.d(TAG, "reconnect");
			if (this.isReconnectionAllowed())
			{
				// Since there is no thread running, creates a new one to attempt
				// the reconnection.
				// avoid to run duplicated reconnectionThread -- fd: 16/09/2010
				if (reconnectionThread != null && reconnectionThread.isAlive())
					return;

				reconnectionThread = new Thread() {

					private int	numAttemps	= 0;

					/**
					 * The process will try the reconnection until the connection succeed or the user
					 * cancell it
					 */
					public void run()
					{
						// The process will try to reconnect until the connection is established or
						// the user cancel the reconnection process {@link Connection#disconnect()}
						while (ReconnectionManager.this.isReconnectionAllowed())
						{
							// Wait 3 seconds before trying to reconnect again
							int remainingSeconds = numAttemps == 0 ? 0 : 3;
							// Sleep until we're ready for the next reconnection attempt. Notify
							// listeners once per second about how much time remains before the next
							// reconnection attempt.
							while (ReconnectionManager.this.isReconnectionAllowed() && remainingSeconds > 0)
							{
								try
								{
									Thread.sleep(1000);
									remainingSeconds--;
									ReconnectionManager.this.notifyAttemptToReconnectIn(remainingSeconds);
								}
								catch (InterruptedException e1)
								{
									e1.printStackTrace();
									// Notify the reconnection has failed
									ReconnectionManager.this.notifyReconnectionFailed(e1);
								}
							}

							// Makes a reconnection attempt
							try
							{
								if (ReconnectionManager.this.isReconnectionAllowed())
								{
									Log.d(TAG, "ReconnectionManager Reconnecting");
									connection.connect();
								}
							}
							catch (XMPPException e)
							{
								// Fires the failed reconnection notification
								ReconnectionManager.this.notifyReconnectionFailed(e);
							}

							numAttemps++;
						}
					}
				};
				reconnectionThread.setName("Smack Reconnection Manager");
				reconnectionThread.setDaemon(true);
				reconnectionThread.start();
			}
		}

		/**
		 * Fires listeners when a reconnection attempt has failed.
		 * 
		 * @param exception the exception that occured.
		 */
		protected void notifyReconnectionFailed(Exception exception)
		{
			if (isReconnectionAllowed())
			{
				/*
				 * for (ConnectionListener listener : connection.connectionListeners) {
				 * listener.reconnectionFailed(exception);
				 * }
				 */
			}
		}

		/**
		 * Fires listeners when The Connection will retry a reconnection. Expressed in seconds.
		 * 
		 * @param seconds the number of seconds that a reconnection will be attempted in.
		 */
		protected void notifyAttemptToReconnectIn(int seconds)
		{
			if (isReconnectionAllowed())
			{
				/*
				 * for (ConnectionListener listener : connection.connectionListeners) {
				 * listener.reconnectingIn(seconds);
				 * }
				 */
			}
		}

		public void connectionClosed()
		{
			done = true;
		}

		public void connectionClosedOnError(Exception e)
		{
			done = false;
			if (e instanceof XMPPException)
			{
				XMPPException xmppEx = (XMPPException) e;
				StreamError error = xmppEx.getStreamError();

				// Make sure the error is not null
				if (error != null)
				{
					String reason = error.getCode();

					if ("conflict".equals(reason))
					{
						return;
					}
				}
			}

			if (this.isReconnectionAllowed())
			{
				this.reconnect();
			}
		}

		public void reconnectingIn(int seconds)
		{
			Log.d(TAG, "reconnectionIn");
		}

		public void reconnectionFailed(Exception e)
		{
			Log.d(TAG, "reconnectionFailed");
		}

		/**
		 * The connection has successfull gotten connected.
		 */
		public void reconnectionSuccessful()
		{
			Log.d(TAG, "reconnectionSuccessful");
			// MessengerService.this.executePendingActions();
		}
	}
}