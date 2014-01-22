/*
 * efectotequila is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * efectotequila is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with efectotequila.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.efectotequila.android.feedgoal;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.efectotequila.android.feedgoal.common.Feed;
import net.efectotequila.android.feedgoal.common.Item;
import net.efectotequila.android.feedgoal.common.TrackerAnalyticsHelper;
import net.efectotequila.android.feedgoal.storage.DbFeedAdapter;
import net.efectotequila.android.feedgoal.storage.DbSchema;
import net.efectotequila.android.feedgoal.storage.SharedPreferencesHelper;
import net.efectotequila.android.feedgoal.util.DrawableUtils;
import net.efectotequila.android.feedgoal.util.FontUtils;

import org.apache.commons.lang3.StringUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class FeedItemActivity extends Activity {

	private static final String LOG_TAG = "FeedItemActivity";
	private static final int KILL_ACTIVITY_CODE = 1;

	private DbFeedAdapter mDbFeedAdapter;
	private long mItemId = -1;

	final Handler myHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbFeedAdapter = new DbFeedAdapter(this);
		mDbFeedAdapter.open();

		TrackerAnalyticsHelper.createTracker(this);

		mItemId = savedInstanceState != null ? savedInstanceState
				.getLong(DbSchema.ItemSchema._ID) : -1;

		if (mItemId == -1) {
			Bundle extras = getIntent().getExtras();
			mItemId = extras != null ? extras.getLong(DbSchema.ItemSchema._ID)
					: -1;
		}

		Item item = mDbFeedAdapter.getItem(mItemId);
		if (item.isFavorite())
			setContentView(R.layout.item_favorite);
		else
			setContentView(R.layout.item_notfavorite);

		/*
		 * To test ads in emulator, remove ads:loadAdOnCreate="true" in layout
		 * if (SharedPreferencesHelper.useAdmob(this)) { int resourceId =
		 * getResources().getIdentifier("adView", "id", this.getPackageName());
		 * AdView adView = (AdView)this.findViewById(resourceId); //AdView
		 * adView = (AdView)this.findViewById(R.id.adView); AdRequest request =
		 * new AdRequest(); request.addTestDevice(AdRequest.TEST_EMULATOR);
		 * adView.loadAd(request); }
		 */
		// AdRequest request = new AdRequest();
		// request.addTestDevice(AdRequest.TEST_EMULATOR);

		TextView title = (TextView) findViewById(R.id.title);
		title.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				TrackerAnalyticsHelper.trackEvent(FeedItemActivity.this,
						LOG_TAG, "Link_Title", mDbFeedAdapter.getItem(mItemId)
								.getLink().toString(), 1);
				adjustLinkableTextColor(v);
				startItemWebActivity();
			}
		});

		TextView channel = (TextView) findViewById(R.id.channel);
		channel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Feed feed = mDbFeedAdapter.getFeed(mDbFeedAdapter
						.getItemFeedId(mItemId));
				String channelHomepage = feed.getHomePage().toString();
				TrackerAnalyticsHelper.trackEvent(FeedItemActivity.this,
						LOG_TAG, "Link_Channel", channelHomepage, 1);
				adjustLinkableTextColor(v);
				if (SharedPreferencesHelper.isOnline(FeedItemActivity.this)) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri
							.parse(channelHomepage));
					startActivity(intent);
				} else
					showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
			}
		});

		/*
		 * Button read_online = (Button) findViewById(R.id.read);
		 * read_online.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { TrackerAnalyticsHelper
		 * .trackEvent(FeedItemActivity.this, LOG_TAG, "Button_ReadOnline",
		 * mDbFeedAdapter.getItem(mItemId).getLink() .toString(), 1);
		 * startItemWebActivity(); } });
		 */

		registerForContextMenu(findViewById(R.id.item));

		ViewGroup godfatherView = (ViewGroup) this.getWindow().getDecorView();
		FontUtils.setRobotoFont(this, godfatherView);
	}

	private void startItemWebActivity() {
		if (SharedPreferencesHelper.isOnline(FeedItemActivity.this)) {
			Intent intent = new Intent(FeedItemActivity.this,
					FeedWebActivity.class);
			intent.putExtra(DbSchema.ItemSchema._ID, mItemId);
			startActivity(intent);
		} else
			showDialog(SharedPreferencesHelper.DIALOG_NO_CONNECTION);
	}

	@SuppressLint("ResourceAsColor")
	private void adjustLinkableTextColor(View v) {
		TextView textView = (TextView) v;
		textView.setTextColor(R.color.color2);
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void displayItemView() {
		if (mItemId != -1) {
			Item item = mDbFeedAdapter.getItem(mItemId);
			TextView titleView = (TextView) findViewById(R.id.title);
			TextView channelView = (TextView) findViewById(R.id.channel);
			TextView pubdateView = (TextView) findViewById(R.id.pubdate);
			// TextView contentView = (TextView) findViewById(R.id.content);
			WebView contentView = (WebView) findViewById(R.id.content);

			WebSettings settings = contentView.getSettings();
			settings.setDefaultTextEncodingName("utf-8");

			contentView.setHorizontalScrollBarEnabled(false);
			/*
			 * contentView.setOnTouchListener(new View.OnTouchListener() {
			 * 
			 * @Override public boolean onTouch(View view, MotionEvent event) {
			 * float m_downX = 0; switch (event.getAction()) { case
			 * MotionEvent.ACTION_DOWN: { // save the x m_downX = event.getX();
			 * } break;
			 * 
			 * case MotionEvent.ACTION_MOVE: case MotionEvent.ACTION_CANCEL:
			 * case MotionEvent.ACTION_UP: { // set x so that it doesn't move
			 * event.setLocation(m_downX, event.getY()); } break;
			 * 
			 * }
			 * 
			 * return false; } });
			 */

			final JavaScriptInterface myJavaScriptInterface = new JavaScriptInterface(
					this);

			// contentView.getSettings().setLightTouchEnabled(true);
			contentView.getSettings().setJavaScriptEnabled(true);
			contentView.addJavascriptInterface(myJavaScriptInterface,
					"AndroidFunction");

			// contentView.setWidth(40);
			// contentView.setGravity(Gravity.FILL_HORIZONTAL); // attempt at
			// justifying text

			if (titleView != null) {
				titleView.setTextSize(22f);
				titleView.setText(item.getTitle());
			}
			if (channelView != null) {
				Feed feed = mDbFeedAdapter.getFeed(mDbFeedAdapter
						.getItemFeedId(mItemId));
				if (feed != null)
					channelView.setText(feed.getTitle());
			}
			if (pubdateView != null) {
				// DateFormat df = new
				// SimpleDateFormat(getResources().getText(R.string.pubdate_format_pattern);
				// pubdateView.setText(df.format(item.getPubdate()));
				CharSequence formattedPubdate = DateFormat
						.format(getResources().getText(
								R.string.pubdate_format_pattern),
								item.getPubdate());
				pubdateView.setText(formattedPubdate);
			}
			if (contentView != null) {
				String content_description = item.getContent();
				if (content_description == null)
					content_description = item.getDescription();
				if (content_description != null) {
					Display display = getWindowManager().getDefaultDisplay();
					Integer maxWidth = display.getWidth() - 20;

					// contentView.setText(content_description,TextView.BufferType.SPANNABLE);
					// contentView.setText(content_description);
					/*
					 * content_description = content_description.replaceAll(
					 * "(\r\n|\n)", "<br />");
					 */
					// Integer width = contentView.getWidth();
					String js = "";
					boolean hasYTVideo = false;
					/*if (StringUtils.containsIgnoreCase(content_description,
							"Youtube Video")) {
						//System.out.println("Video");
						hasYTVideo = true;
						String vId = StringUtils.substringBetween(
								content_description, "/vi/", "/");
						js = "<script type=\"text/javascript\"> function launchYTVideo() { AndroidFunction.launchYTVideo('"
								+ vId + "'); } </script>";
						String spanHref = StringUtils.substringBetween(
								content_description, "<espan href=", ">");
						content_description = StringUtils.replace(
								content_description, "<espan href=" + spanHref,
								"<a href=\"javascript:launchYTVideo()\"");
						content_description = StringUtils.replace(
								content_description, "</espan>", "</a>");

					}*/
					String text = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><style>@font-face { font-family: 'roboto'; src: url('file:///android_asset/fonts/Roboto-Regular.ttf'); } img { border:0; padding:0; margin: 0; max-width:"
							+ maxWidth
							+ "px; } p, span { text-align:justify; line-height: 130%; } p.video { padding-top: 5px; padding-bottom: 5px; } a.videolink { display: block; } </style>"
							+ js
							+ "</head><body style=\"font-size: 16px; font-family: 'Roboto', Verdana, sans-serif; text-align:justify; line-height: 130%;\"><p align=\"justify\">"
							+ content_description
							+ "</p> "
							+ (hasYTVideo ? "<p class=\"video\"><a class=\"videolink\" href=\"javascript:launchYTVideo()\">Ver el video</a></p>"
									: "") + "</body></html>";
					System.out.println(text);
					/*
					 * contentView.loadDataWithBaseURL("file:///android_asset/",
					 * text, "text/html", "utf-8", null);
					 */
					// ByteBuffer bb = Charset.forName("UTF-8").encode(text);
					// String ntext = new String(bb.array(),
					// Charset.forName("UTF-8"));
					contentView.loadDataWithBaseURL("file:///android_asset/", text, "text/html",
							"UTF-8", null);
				}
			}

			// set item as read (case when item is displayed from next/previous
			// contextual menu or buttons)
			ContentValues values = new ContentValues();
			values.put(DbSchema.ItemSchema.COLUMN_READ, DbSchema.ON);
			mDbFeedAdapter.updateItem(mItemId, values, null);

			TrackerAnalyticsHelper.trackPageView(this, "/offlineItemView");
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		TrackerAnalyticsHelper.startTracker(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayItemView();
	}

	@Override
	protected void onStop() {
		super.onStop();
		TrackerAnalyticsHelper.stopTracker(this);
	}

	@Override
	protected void onDestroy() {
		View view = findViewById(R.id.item);

		if (view != null) {
			DrawableUtils.unbindDrawables(view);
		}

		super.onDestroy();
		mDbFeedAdapter.close();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(DbSchema.ItemSchema._ID, mItemId);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (SharedPreferencesHelper.isDynamicMode(this)) {
			inflater.inflate(R.menu.opt_item_menu_public_mode, menu);
			MenuItem channelsMenuItem = (MenuItem) menu
					.findItem(R.id.menu_opt_channels);
			channelsMenuItem.setIntent(new Intent(this,
					FeedChannelsActivity.class));
		} else {
			inflater.inflate(R.menu.opt_item_menu_private_mode, menu);

			// Channels menu item
			if (mDbFeedAdapter.getFeeds().size() > 1) {
				MenuItem channelsMenuItem = (MenuItem) menu
						.findItem(R.id.menu_opt_channels);
				SubMenu subMenu = channelsMenuItem.getSubMenu();

				List<Feed> feeds = mDbFeedAdapter.getFeeds();
				Iterator<Feed> feedIterator = feeds.iterator();
				Feed feed = null;
				MenuItem channelSubMenuItem = null;
				Intent intent = null;
				int order = 0;
				while (feedIterator.hasNext()) {
					feed = feedIterator.next();
					channelSubMenuItem = subMenu.add(
							SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP,
							Menu.NONE, order, feed.getTitle());

					if (feed.getId() == SharedPreferencesHelper
							.getPrefTabFeedId(this, mDbFeedAdapter
									.getFirstFeed().getId()))
						channelSubMenuItem.setChecked(true);

					intent = new Intent(this, FeedTabActivity.class);
					intent.putExtra(DbSchema.FeedSchema._ID, feed.getId());
					channelSubMenuItem.setIntent(intent);

					order++;
				}

				subMenu.setGroupCheckable(
						SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP, true,
						true);
			} else {
				menu.removeItem(R.id.menu_opt_channels);
			}
		}

		// Home menu item
		MenuItem menuItem = (MenuItem) menu.findItem(R.id.menu_opt_home);
		menuItem.setIntent(new Intent(this, FeedTabActivity.class));

		// Preferences menu item
		MenuItem preferencesMenuItem = (MenuItem) menu
				.findItem(R.id.menu_opt_preferences);
		preferencesMenuItem.setIntent(new Intent(this, FeedPrefActivity.class));

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_opt_home:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG, "OptionMenu_Home",
					"Home", 1);
			// Kill the FeedTabActivity that started this FeedItemActivity,
			// because tab channel id may have changed and wouldn't be correct
			// (wouldn't be the initial FeedTabActivity channel id) if back
			// button is pressed
			setResult(RESULT_OK);
			startActivity(item.getIntent());
			// finish();
			return true;
		case R.id.menu_opt_channels:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"OptionMenu_Channels", "Channels", 1);
			if (SharedPreferencesHelper.isDynamicMode(this)) {
				// Kill the FeedTabActivity that started this FeedItemActivity,
				// because tab channel id may have changed and wouldn't be
				// correct (wouldn't be the initial FeedTabActivity channel id)
				// if back button is pressed
				setResult(RESULT_OK);
				startActivityForResult(item.getIntent(), KILL_ACTIVITY_CODE);
			} else {
				// do nothing, default case will be handled
			}
			return true;
		case R.id.menu_opt_preferences:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"OptionMenu_Preferences", "Preferences", 1);
			startActivity(item.getIntent());
			return true;
		case R.id.menu_opt_about:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"OptionMenu_AboutDialog", "About", 1);
			showDialog(SharedPreferencesHelper.DIALOG_ABOUT);
			return true;
		default:
			if (item.getGroupId() == SharedPreferencesHelper.CHANNEL_SUBMENU_GROUP) {
				// Kill the FeedTabActivity that started this FeedItemActivity,
				// because tab channel id is now changing and won't be correct
				// (won't be the initial FeedTabActivity channel id) if back
				// button is pressed
				setResult(RESULT_OK);
				startActivity(item.getIntent());
				// finish();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		switch (requestCode) {
		case KILL_ACTIVITY_CODE:
			if (resultCode == RESULT_OK)
				finish();
			break;
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		if (v.getId() == R.id.item) {
			menu.setHeaderTitle(R.string.ctx_menu_title);
			MenuInflater inflater = getMenuInflater();

			Item item = mDbFeedAdapter.getItem(mItemId);

			if (item != null) {
				long feedId = mDbFeedAdapter.getItemFeedId(mItemId);
				boolean isFirstItem = false;
				boolean isLastItem = false;
				if (mItemId == mDbFeedAdapter.getFirstItem(feedId).getId())
					isFirstItem = true;
				else if (mItemId == mDbFeedAdapter.getLastItem(feedId).getId())
					isLastItem = true;

				if (item.isFavorite()) {
					if (isFirstItem)
						inflater.inflate(
								R.menu.ctx_menu_item_offline_notfav_next, menu);
					else if (isLastItem)
						inflater.inflate(
								R.menu.ctx_menu_item_offline_notfav_prev, menu);
					else
						inflater.inflate(
								R.menu.ctx_menu_item_offline_notfav_next_prev,
								menu);
				} else {
					if (isFirstItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_fav_next,
								menu);
					else if (isLastItem)
						inflater.inflate(R.menu.ctx_menu_item_offline_fav_prev,
								menu);
					else
						inflater.inflate(
								R.menu.ctx_menu_item_offline_fav_next_prev,
								menu);
				}
			}
		}
	}

	public boolean onContextItemSelected(MenuItem menuItem) {
		Item item = mDbFeedAdapter.getItem(mItemId);
		ImageView favView = (ImageView) findViewById(R.id.fav);
		ContentValues values = null;
		Intent intent = null;
		long feedId = -1;

		switch (menuItem.getItemId()) {
		case R.id.read_online:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_ReadOnline", item.getLink().toString(), 1);
			startItemWebActivity();
			return true;
		case R.id.add_fav:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_AddFavorite", item.getLink().toString(), 1);
			// item.favorite();
			values = new ContentValues();
			values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.ON);
			mDbFeedAdapter.updateItem(mItemId, values, null);
			favView.setImageResource(R.drawable.fav);
			Toast.makeText(this, R.string.add_fav_msg, Toast.LENGTH_SHORT)
					.show();
			return true;
		case R.id.remove_fav:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_RemoveFavorite", item.getLink().toString(), 1);
			// item.unfavorite();
			Date now = new Date();
			long diffTime = now.getTime() - item.getPubdate().getTime();
			long maxTime = SharedPreferencesHelper.getPrefMaxHours(this) * 60 * 60 * 1000; // Max
																							// hours
																							// expressed
																							// in
																							// milliseconds
			// test if item has expired
			if (maxTime > 0 && diffTime > maxTime) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.remove_fav_confirmation)
						.setCancelable(false)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										ContentValues values = new ContentValues();
										values.put(
												DbSchema.ItemSchema.COLUMN_FAVORITE,
												DbSchema.OFF);
										ImageView favView = (ImageView) findViewById(R.id.fav);
										favView.setImageResource(R.drawable.no_fav);
										mDbFeedAdapter.updateItem(mItemId,
												values, null);
										Toast.makeText(FeedItemActivity.this,
												R.string.remove_fav_msg,
												Toast.LENGTH_SHORT).show();
									}
								})
						.setNegativeButton(R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				builder.create().show();
			} else {
				values = new ContentValues();
				values.put(DbSchema.ItemSchema.COLUMN_FAVORITE, DbSchema.OFF);
				mDbFeedAdapter.updateItem(mItemId, values, null);
				favView.setImageResource(R.drawable.no_fav);
				Toast.makeText(this, R.string.remove_fav_msg,
						Toast.LENGTH_SHORT).show();
			}
			return true;
		case R.id.next:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_NextItem", item.getLink().toString(), 1);
			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
			intent = new Intent(this, FeedItemActivity.class);
			intent.putExtra(DbSchema.ItemSchema._ID,
					mDbFeedAdapter.getNextItemId(feedId, mItemId));
			startActivity(intent);
			finish();
			return true;
		case R.id.previous:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_PreviousItem", item.getLink().toString(), 1);
			feedId = mDbFeedAdapter.getItemFeedId(mItemId);
			intent = new Intent(this, FeedItemActivity.class);
			intent.putExtra(DbSchema.ItemSchema._ID,
					mDbFeedAdapter.getPreviousItemId(feedId, mItemId));
			startActivity(intent);
			finish();
			return true;
		case R.id.share:
			TrackerAnalyticsHelper.trackEvent(this, LOG_TAG,
					"ContextMenu_Share", item.getLink().toString(), 1);
			item = mDbFeedAdapter.getItem(mItemId);
			if (item != null) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(
						getString(R.string.share_subject),
						getString(R.string.app_name)));
				shareIntent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + " "
						+ item.getLink().toString());
				shareIntent.setType("text/plain");
				startActivity(Intent.createChooser(shareIntent,
						getString(R.string.share)));
			}
			return true;
		default:
			return super.onContextItemSelected(menuItem);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		CharSequence title = null;
		LayoutInflater inflater = null;
		View dialogLayout = null;
		AlertDialog.Builder builder = null;
		switch (id) {
		case SharedPreferencesHelper.DIALOG_ABOUT:
			// title = getResources().getText(R.string.app_name) + " - " +
			// getResources().getText(R.string.version) + " " +
			// SharedPreferencesHelper.getVersionName(this);
			title = getString(R.string.app_name) + " - "
					+ getString(R.string.version) + " "
					+ SharedPreferencesHelper.getVersionName(this);

			/*
			 * Without cancel button dialog = new Dialog(this);
			 * dialog.setContentView(R.layout.dialog_about);
			 * dialog.setTitle(title);
			 */
			inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			dialogLayout = inflater.inflate(R.layout.dialog_about, null);
			TextView childView = null;
			if (getString(R.string.website).equals("")) {
				childView = (TextView) dialogLayout.findViewById(R.id.website);
				childView.setVisibility(View.GONE);
			}
			if (getString(R.string.email).equals("")) {
				childView = (TextView) dialogLayout.findViewById(R.id.email);
				childView.setVisibility(View.GONE);
			}
			if (getString(R.string.contact).equals("")) {
				childView = (TextView) dialogLayout.findViewById(R.id.contact);
				childView.setVisibility(View.GONE);
			}
			if (getString(R.string.powered).equals("")) {
				childView = (TextView) dialogLayout.findViewById(R.id.powered);
				childView.setVisibility(View.GONE);
			}
			builder = new AlertDialog.Builder(this);
			builder.setView(dialogLayout)
					.setTitle(title)
					.setIcon(R.drawable.ic_dialog)
					.setNeutralButton(R.string.close,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case SharedPreferencesHelper.DIALOG_NO_CONNECTION:
			title = getString(R.string.error);
			inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			dialogLayout = inflater
					.inflate(R.layout.dialog_no_connection, null);
			builder = new AlertDialog.Builder(this);
			builder.setView(dialogLayout)
					.setTitle(title)
					.setIcon(R.drawable.ic_dialog)
					.setNeutralButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private void launchYoutubeApp(String videoId) {
		// System.out.println("Hoal.");
		/*
		 * startActivity(new Intent(Intent.ACTION_VIEW,
		 * Uri.parse("http://www.youtube.com/watch?v=" + videoId)));
		 */
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("vnd.youtube://" + videoId));
			startActivity(intent);
		} catch (Exception e) {
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://www.youtube.com/watch?v=" + videoId)));
		}
	}

	public class JavaScriptInterface {
		Context mContext;

		JavaScriptInterface(Context c) {
			mContext = c;
		}

		public void launchYTVideo(final String YTId) {
			System.out.println("launchYTVideo(String YTId)");
			showWaitMsg();
			// final String msgeToast = webMessage;
			//launchYoutubeApp(YTId);
			//if(false)
			myHandler.post(new Runnable() {
				@Override
				public void run() {
					// This gets executed on the UI thread so it can safely
					// modify Views
					// myTextView.setText(msgeToast);
					launchYoutubeApp(YTId);
				}
			});

			// Toast.makeText(mContext, webMessage, Toast.LENGTH_SHORT).show();
		}
	}

	private void showWaitMsg() {
		Context context = getApplicationContext();
		CharSequence text = "Cargando el video, por favor espera...";
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
}