package com.abewy.android.apps.klyph.messenger.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.abewy.android.apps.klyph.core.KlyphDevice;
import com.abewy.android.apps.klyph.core.KlyphFlags;
import com.abewy.android.apps.klyph.core.KlyphSession;
import com.abewy.android.apps.klyph.core.fql.Friend;
import com.abewy.android.apps.klyph.core.fql.MessageThread;
import com.abewy.android.apps.klyph.core.fql.User;
import com.abewy.android.apps.klyph.core.util.AlertUtil;
import com.abewy.android.apps.klyph.core.util.AttrUtil;
import com.abewy.android.apps.klyph.core.util.FacebookUtil;
import com.abewy.android.apps.klyph.core.util.HierachyViewUtil;
import com.abewy.android.apps.klyph.messenger.MessengerApplication;
import com.abewy.android.apps.klyph.messenger.MessengerBundleExtras;
import com.abewy.android.apps.klyph.messenger.MessengerPreferences;
import com.abewy.android.apps.klyph.messenger.R;
import com.abewy.android.apps.klyph.messenger.fragment.ConversationFragment;
import com.abewy.android.apps.klyph.messenger.fragment.ConversationFragment.ConversationCallback;
import com.abewy.android.apps.klyph.messenger.fragment.ConversationListFragment;
import com.abewy.android.apps.klyph.messenger.fragment.ConversationListFragment.ConversationListCallback;
import com.abewy.android.apps.klyph.messenger.fragment.LoginFragment;
import com.abewy.android.apps.klyph.messenger.fragment.LoginFragment.LoginFragmentCallBack;
import com.abewy.android.apps.klyph.messenger.fragment.SelectionFragment;
import com.abewy.android.apps.klyph.messenger.fragment.SelectionFragment.SelectionCallback;
import com.abewy.android.apps.klyph.messenger.iab.IabHelper;
import com.abewy.android.apps.klyph.messenger.iab.IabResult;
import com.abewy.android.apps.klyph.messenger.iab.Inventory;
import com.abewy.android.apps.klyph.messenger.iab.Purchase;
import com.abewy.android.apps.klyph.messenger.service.MessengerService;
import com.facebook.Session;
import com.facebook.SessionState;

public class MainActivity extends TitledFragmentActivity implements LoginFragmentCallBack, ConversationCallback, ConversationListCallback,
		SelectionCallback
{
	private static final String			TAG	= "MainActivity";

	private SlidingPaneLayout			slidingPane;
	private LinearLayout				leftContainer;
	private LinearLayout				rightContainer;
	private LoginFragment				loginFragment;
	private ConversationListFragment	conversationListFragment;
	private ConversationFragment		conversationFragment;
	private SelectionFragment			selectionFragment;
	private Fragment					rightFragment;

	private int							leftContainerWidth;
	private int							rightContainerWidth;
	private int							leftContainerLandscapeWidth;
	private int							rightContainerLandscapeWidth;

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		String id = intent.getStringExtra(MessengerBundleExtras.SHOW_FRIEND_CONVERSATION);
		if (id != null)
		{
			getFragmentManager().beginTransaction().hide(selectionFragment).show(conversationFragment).commit();
			conversationFragment.loadFriendConversation(id);
			conversationListFragment.setSelectedConversation(id);
			slidingPane.closePane();
		}
		else
		{
			slidingPane.openPane();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d("MainActivity", "onCreate: ");
		Log.d("MainActivity", "onCreate: " + loggedIn);

		displayBackArrow(false);

		String base64EncodedPublicKey = "[KEY]";

		// compute your public key and store it in base64EncodedPublicKey
		mHelper = new IabHelper(this, base64EncodedPublicKey);

		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(true);

		slidingPane = (SlidingPaneLayout) findViewById(R.id.sliding_pane);
		leftContainer = (LinearLayout) findViewById(R.id.left_container);
		rightContainer = (LinearLayout) findViewById(R.id.right_container);

		conversationListFragment = (ConversationListFragment) getFragmentManager().findFragmentById(R.id.conversation_list_fragment);
		conversationFragment = (ConversationFragment) getFragmentManager().findFragmentById(R.id.conversation_fragment);
		selectionFragment = (SelectionFragment) getFragmentManager().findFragmentById(R.id.selection_fragment);

		// slidingPane.setParallaxDistance(getResources().getDimensionPixelSize(R.dimen.parallax_distance));
		slidingPane.setSliderFadeColor(AttrUtil.getColor(this, R.attr.slidingPaneFadeColor));
		// slidingPane.setSliderFadeColor(0xFF000000);
		// slidingPane.setCoveredFadeColor(0xFFFFFFFF);

		leftContainerWidth = getResources().getDimensionPixelSize(R.dimen.left_container_width);
		rightContainerWidth = getResources().getDimensionPixelSize(R.dimen.right_container_width);
		leftContainerLandscapeWidth = getResources().getDimensionPixelSize(R.dimen.left_container_landscape_width);
		rightContainerLandscapeWidth = getResources().getDimensionPixelSize(R.dimen.right_container_landscape_width);

		slidingPane.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v)
			{
				if (slidingPane.isOpen())
					slidingPane.openPane();
			}
		});

		slidingPane.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {

			@Override
			public void onPanelSlide(View panel, float slideOffset)
			{

			}

			@Override
			public void onPanelOpened(View panel)
			{
				displayBackArrow(false);
				supportInvalidateOptionsMenu();
				setTitle(R.string.app_name);
			}

			@Override
			public void onPanelClosed(View panel)
			{
				displayBackArrow(true);
				supportInvalidateOptionsMenu();

				if (rightFragment == selectionFragment)
					setTitle(R.string.app_name);
			}
		});

		if (KlyphFlags.IS_PRO_VERSION == true)
			setTitle(R.string.app_pro_name);
		else
			setTitle(R.string.app_name);

		rightFragment = selectionFragment;

		adContainer = (ViewGroup) findViewById(R.id.ad);

		refreshContainersLayout();

		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

		Log.d("MainActivity", "onCreate: ");
		Log.d("MainActivity", "onCreate: " + loggedIn);
		if (loggedIn == false)
		{
			Log.d("MainActivity", "show login");
			loginFragment = new LoginFragment();
			getFragmentManager().beginTransaction().replace(R.id.login_container, loginFragment).commit();
			findViewById(R.id.login_container).setVisibility(View.VISIBLE);
		}
		else
		{
			Log.d("MainActivity", "don't show login");
			slidingPane.setVisibility(View.VISIBLE);
			endInit();
		}

		// Facebook HashKey
		if (KlyphFlags.LOG_FACEBOOK_HASH)
			FacebookUtil.logHash(this);

		// Hierarchy View Connector
		if (KlyphFlags.ENABLE_HIERACHY_VIEW_CONNECTOR)
			HierachyViewUtil.connectHierarchyView(this);
	}

	private void refreshContainersLayout()
	{
		if (KlyphDevice.isPortraitMode())
			setPortraitMode();
		else
			setLandscapeMode();
	}

	private void setPortraitMode()
	{
		SlidingPaneLayout.LayoutParams params = (SlidingPaneLayout.LayoutParams) rightContainer.getLayoutParams();
		params.width = KlyphDevice.getDeviceWidth();// rightContainerWidth;
		// params.leftMargin = paddingLeft;
		params.rightMargin = 0;
		rightContainer.setLayoutParams(params);

		params = (SlidingPaneLayout.LayoutParams) leftContainer.getLayoutParams();
		params.width = leftContainerWidth;
		params.leftMargin = 0;
		params.rightMargin = 0;
		leftContainer.setLayoutParams(params);

		slidingPane.requestLayout();
	}

	private void setLandscapeMode()
	{
		int rightWidth = KlyphDevice.getDeviceWidth();
		float screenWidthDpi = KlyphDevice.getDeviceWidth() / KlyphDevice.getDeviceDensity();
		if (screenWidthDpi >= 720)
			rightWidth = Math.max(rightContainerLandscapeWidth, KlyphDevice.getDeviceWidth() - leftContainerLandscapeWidth);

		LayoutParams params = rightContainer.getLayoutParams();
		params.width = rightWidth;
		rightContainer.setLayoutParams(params);

		params = leftContainer.getLayoutParams();
		params.width = leftContainerLandscapeWidth;
		leftContainer.setLayoutParams(params);
	}

	@Override
	protected int getLayout()
	{
		return R.layout.activity_main;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Log.d("MainActivity", "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);

		refreshContainersLayout();

		supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		Log.d("MainActivity", "onCreateOptionsMenu: ");

		if (loggedIn)
		{
			boolean shouldAddMenuItem = false;
			
			if (KlyphDevice.isPortraitMode())
				shouldAddMenuItem = slidingPane.isOpen();
			else
				shouldAddMenuItem = selectionFragment != null && !selectionFragment.isVisible();

			if (shouldAddMenuItem)
			{
				MenuItem item = menu.add(Menu.NONE, R.id.menu_add, Menu.NONE, R.string.menu_new_conversation);
				item.setIcon(AttrUtil.getResourceId(this, R.attr.addIcon));
				item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}

			if (menu.findItem(R.id.menu_faq) == null)
			{
				menu.add(Menu.NONE, R.id.menu_faq, Menu.NONE, R.string.menu_faq).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}

			if (menu.findItem(R.id.menu_buy_pro) == null && MessengerApplication.PRO_VERSION_CHECKED && !MessengerApplication.IS_PRO_VERSION)
			{
				menu.add(Menu.NONE, R.id.menu_buy_pro, Menu.NONE, R.string.menu_buy_pro).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
			}
		}
		

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			if (!slidingPane.isOpen())
				slidingPane.openPane();

			return true;
		}
		else if (item.getItemId() == R.id.menu_add)
		{
			rightFragment = selectionFragment;
			conversationListFragment.deselect();
			getFragmentManager().beginTransaction().hide(conversationFragment).show(selectionFragment).commit();
			slidingPane.closePane();

			return true;
		}
		else if (item.getItemId() == R.id.menu_logout)
		{
			logout();
			return true;
		}
		else if (item.getItemId() == R.id.menu_faq)
		{
			startActivity(new Intent(this, FaqActivity.class));
		}
		else if (item.getItemId() == R.id.menu_buy_pro)
		{
			String payload = "my_great_payload";

			mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST, mPurchaseFinishedListener, payload);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed()
	{
		if (!slidingPane.isOpen())
		{
			slidingPane.openPane();
		}
		else
		{
			super.onBackPressed();
		}
	}

	private void logout()
	{
		Log.d("MainActivity", "logout");
		conversationFragment.disconnectFromService();
		selectionFragment.disconnectFromService();
		conversationListFragment.disconnectFromService();

		loggedIn = false;

		KlyphSession.logout();
		stopService(new Intent(this, MessengerService.class));

		MessengerPreferences.setFriends(null);
		MessengerPreferences.setLastConversations(null);

		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
						| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		startActivity(intent);
	}

	// ___ Facebook login management ___________________________________________

	ViewGroup				adContainer;

	private boolean			sessionInitalized	= false;
	private static boolean	loggedIn			= false;

	@Override
	protected void onSessionStateChange(Session session, SessionState state, Exception exception)
	{
		Log.d("MainActivity", "onSessionStateChange");
		super.onSessionStateChange(session, state, exception);
		updateView();
	}

	@Override
	public void onUserInfoFetched(User user)
	{
		KlyphSession.setSessionUser(user);
		loggedIn = true;

		if (!sessionInitalized)
		{
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result)
				{
					Log.d(TAG, "Setup finished.");

					if (!result.isSuccess())
					{
						// Oh noes, there was a problem.
						Log.d("MainActivity.onCreate(...).new OnIabSetupFinishedListener() {...}",
								"onIabSetupFinished: Problem setting up in-app billing: " + result);
						return;
					}

					// Have we been disposed of in the meantime? If so, quit.
					if (mHelper == null)
						return;

					// IAB is fully set up. Now, let's get an inventory of stuff we own.
					Log.d(TAG, "Setup successful. Querying inventory.");
					mHelper.queryInventoryAsync(mGotInventoryListener);
				}
			});
		}

		loggedIn = true;
	}

	private void updateView()
	{
		Session session = Session.getActiveSession();

		if (session.isOpened())
		{
			if (sessionInitalized == false && KlyphSession.getSessionUserId() != null && MessengerApplication.PRO_VERSION_CHECKED)
			{
				endInit();
			}
		}
	}

	private void endInit()
	{
		Log.d("MainActivity", "endInit");
		loggedIn = true;
		if (sessionInitalized == false)
		{
			Log.d("MainActivity", "endInit");

			if (KlyphFlags.LOG_ACCESS_TOKEN)
				Log.d("MainActivity", Session.getActiveSession().getAccessToken());

			// If just logged in and notifications enabled, then start the
			// service
			if (!MessengerService.isRunning())
			{
				Intent intent = new Intent(this, MessengerService.class);
				startService(intent);
			}

			slidingPane.setVisibility(View.VISIBLE);

			if (loginFragment != null)
				getFragmentManager().beginTransaction().remove(loginFragment).hide(conversationFragment).show(selectionFragment)
						.commitAllowingStateLoss();
			else
				getFragmentManager().beginTransaction().hide(conversationFragment).show(selectionFragment).commitAllowingStateLoss();

			findViewById(R.id.login_container).setVisibility(View.GONE);

			loginFragment = null;

			rightFragment = selectionFragment;

			conversationListFragment.load();
			conversationFragment.connectToService();
			selectionFragment.load();

			slidingPane.setParallaxDistance(getResources().getDimensionPixelSize(R.dimen.parallax_distance));
			slidingPane.openPane();

			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeButtonEnabled(true);

			sessionInitalized = true;

			if (getIntent().getStringExtra(MessengerBundleExtras.SHOW_FRIEND_CONVERSATION) != null)
			{
				getFragmentManager().beginTransaction().hide(selectionFragment).show(conversationFragment).commitAllowingStateLoss();
				conversationFragment.loadFriendConversation(getIntent().getStringExtra(MessengerBundleExtras.SHOW_FRIEND_CONVERSATION));
				slidingPane.closePane();
			}

			// Licensing
			if (KlyphFlags.BANNER_ADS_ENABLED && !MessengerApplication.IS_PRO_VERSION)
			{
				adContainer.setVisibility(View.VISIBLE);
				manageAdView(adContainer, true);
			}
		}
	}

	public static class RightFrameLayout extends FrameLayout
	{
		public RightFrameLayout(Context paramContext)
		{
			super(paramContext);
		}

		public RightFrameLayout(Context paramContext, AttributeSet paramAttributeSet)
		{
			super(paramContext, paramAttributeSet);
		}

		public RightFrameLayout(Context paramContext, AttributeSet paramAttributeSet, int paramInt)
		{
			super(paramContext, paramAttributeSet, paramInt);
		}

		public boolean onTouchEvent(MotionEvent paramMotionEvent)
		{
			return true;
		}
	}

	@Override
	public void onConversationSelected(MessageThread thread)
	{
		rightFragment = conversationFragment;
		conversationFragment.loadThreadConversation(thread);

		getFragmentManager().beginTransaction().hide(selectionFragment).show(conversationFragment).commit();

		slidingPane.closePane();
	}

	@Override
	public void onFriendSelected(Friend friend)
	{
		rightFragment = conversationFragment;
		conversationListFragment.setSelectedConversation(friend.getUid());
		conversationFragment.loadFriendConversation(friend.getUid());

		getFragmentManager().beginTransaction().hide(selectionFragment).show(conversationFragment).commit();

		slidingPane.closePane();
	}

	@Override
	public void onMessageSent(String threadId, String message)
	{
		conversationListFragment.userSentMessage(threadId, message);
	}

	@Override
	protected void onResume()
	{
		Log.d("MainActivity", "onResume");
		super.onResume();

		if (sessionInitalized)
		{
			conversationFragment.connectToService();
			conversationListFragment.connectToService();
			selectionFragment.connectToService();
		}
	}

	@Override
	public void onPause()
	{
		Log.d("MainActivity", "onPause");
		super.onPause();

		// If on login view, don't disconnect fragments when going to Fb login dialog
		if (sessionInitalized)
		{
			conversationFragment.disconnectFromService();
			conversationListFragment.disconnectFromService();
			selectionFragment.disconnectFromService();
		}
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (mHelper != null)
		{
			mHelper.dispose();
			mHelper = null;
		}
	}

	// ___ InApp Billing
	private IabHelper							mHelper;

	private static final String					SKU_PREMIUM				= "com.abewy.android.apps.klyph.messenger.premium";
	// private static final String SKU_PREMIUM = "android.test.purchased";

	// (arbitrary) request code for the purchase flow
	private static final int					RC_REQUEST				= 10001;

	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener	mGotInventoryListener	= new IabHelper.QueryInventoryFinishedListener() {
																			public void onQueryInventoryFinished(IabResult result, Inventory inventory)
																			{
																				Log.d(TAG, "Query inventory finished.");
																				MessengerApplication.PRO_VERSION_CHECKED = true;
																				// Have we been disposed of in the meantime? If so, quit.
																				if (mHelper == null)
																					return;

																				// Is it a failure?
																				if (result.isFailure())
																				{
																					Log.d(TAG, "Failed to query inventory: " + result);
																					// Fail to check, so we don't display ads
																					// to avoid pro users to see ads
																					MessengerApplication.IS_PRO_VERSION = true;
																					return;
																				}

																				Log.d(TAG, "Query inventory was successful.");

																				/*
																				 * Check for items we own. Notice that for each purchase, we check
																				 * the developer payload to see if it's correct! See
																				 * verifyDeveloperPayload().
																				 */

																				// Do we have the premium upgrade?
																				Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
																				MessengerApplication.IS_PRO_VERSION = premiumPurchase != null;
																				
																				if (premiumPurchase != null)
																				{
																					Log
																							.d("IABKM",
																									"onQueryInventoryFinished: " + premiumPurchase.getPurchaseState());
																				}
																				Log.d(TAG, "User is "
																							+ (MessengerApplication.IS_PRO_VERSION ? "PREMIUM"
																									: "NOT PREMIUM"));

																				endInit();
																				Log.d(TAG, "Initial inventory query finished; enabling main UI.");
																			}
																		};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
		if (mHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
		{
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
		else
		{
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}

	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener	mPurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener() {
																			public void onIabPurchaseFinished(IabResult result, Purchase purchase)
																			{
																				Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

																				// if we were disposed of in the meantime, quit.
																				if (mHelper == null)
																					return;

																				if (result.isFailure())
																				{
																					Log.d(TAG, "Error purchasing: " + result);
																					return;
																				}

																				Log.d(TAG, "Purchase successful.");

																				if (purchase.getSku().equals(SKU_PREMIUM))
																				{
																					// bought the premium upgrade!
																					Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");

																					MessengerApplication.PRO_VERSION_CHECKED = true;
																					MessengerApplication.IS_PRO_VERSION = true;
																					hideAds();
																					invalidateOptionsMenu();

																					AlertUtil.showAlert(MainActivity.this, R.string.thank_you,
																							R.string.thank_you_purchase, R.string.ok);
																				}
																			}
																		};

	private void hideAds()
	{
		if (adContainer != null)
			manageAdView(adContainer, false);
	}
}
