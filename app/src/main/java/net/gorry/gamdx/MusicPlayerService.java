/**
 *
 */
package net.gorry.gamdx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gorry
 *
 */
public class MusicPlayerService extends ForegroundService {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "MusicPlayerService";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	/** MusicPlayerServiceのアクション名 */
	public static final String ACTION = "MusicPlayerService";

	/** MusicPlayerServiceの機能番号 */
	public static final int PING_PONG = 1;
	/** */
	public static final int TIMER_IRQ = 2;
	/** */
	public static final int ACCEPT_MUSIC_FILE = 3;
	/** */
	public static final int END_PLAY = 4;

	private MusicPlayer player;
	private Context me = null;

	private NotificationManager nm;
	private boolean mSetAlarm = false;

	private static final int NOTIFY_SYSTEM = 0;
	private static final int NOTIFY_LOWMEMORY = 1;

	private static final String CHANNEL_GAMDX="channel_gamdx";

	/**
	 * コンストラクタ
	 */
	public MusicPlayerService() {
		super(NOTIFY_SYSTEM);
		if (T) Log.v(TAG, M()+"@in");

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 作成
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		if (T) Log.v(TAG, M()+"@in");

		super.onCreate();
		// Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		nm = super.getNotificationManager();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 開始
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent+", startId="+startId);

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			//
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	private void setupNotification() {
		if (T) Log.v(TAG, M()+"@in");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel channel = new NotificationChannel(
				CHANNEL_GAMDX,
				"GAMDX",
				NotificationManager.IMPORTANCE_DEFAULT
			);
			channel.enableLights(false);
			channel.setLightColor(Color.WHITE);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			manager.createNotificationChannel(channel);
		}

		SharedPreferences pref = getApplicationContext().getSharedPreferences("musicplayerservice", 0);
		final boolean killedByLowMemory = pref.getBoolean("killedbylowmemory", false);
		if (killedByLowMemory) {
			try {
				// ここで例外発生により落ちる事例がいくつかあるので防止。原因不明
				startForegroundCompat(
						showNotification(
								"Restart", "GAMDX",
								getString(R.string.musicplayerservice_java_restartmusicplayerservicebylowmemory),
								R.mipmap.ic_notification,
								Notification.FLAG_ONGOING_EVENT,
								NOTIFY_SYSTEM
						)
				);
			} catch (final Exception e) {
				e.printStackTrace();
			}
			final SharedPreferences.Editor editor = pref.edit();
			editor.putBoolean("killedbylowmemory", false);
			editor.commit();
		} else {
			try {
				// ここで例外発生により落ちる事例がいくつかあるので防止。原因不明
				startForegroundCompat(
						showNotification(
								"Start", "GAMDX",
								getString(R.string.musicplayerservice_java_startmusicplayerservice),
								R.mipmap.ic_notification,
								Notification.FLAG_ONGOING_EVENT,
								NOTIFY_SYSTEM
						)
				);
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		clearNotification(NOTIFY_LOWMEMORY);
		pref = null;

		// startTimer();
		if (!mSetAlarm) {
			MyAlarmManager.setAlarmManager(this,new Runnable() {
				@Override
				public void run() {
					if (D) Log.d(TAG, "sendPing()");
					// ActivityMainにKeepalive Pingを送る
					final Intent intent2 = new Intent(ACTION);
					intent2.putExtra("msg", MusicPlayerService.PING_PONG);
					sendBroadcast(intent2);
				}
			});
			mSetAlarm = true;
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 破棄
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");

		showNotification(
				"End", "GAMDX",
				getString(R.string.musicplayerservice_java_shutdownmusicplayerservice),
				R.mipmap.ic_notification,
				Notification.FLAG_ONGOING_EVENT,
				NOTIFY_SYSTEM
		);
		// if (ircServerList != null) {
		// 	ircServerList.closeAll();
		// }
		SharedPreferences pref = getApplicationContext().getSharedPreferences("musicplayerservice", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("killedbylowmemory", false);
		editor.commit();
		pref = null;
		clearNotification(NOTIFY_SYSTEM);
		clearNotification(NOTIFY_LOWMEMORY);
		// stopTimer();
		MyAlarmManager.resetAlarmManager(this);
		player.save();
		player.dispose();
		player = null;

		super.onDestroy();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * バインド
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		me = this;
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			//
		}

		Setting.setContext(me);
		Setting.load();

		if (player == null) {
			player = new MusicPlayer(me, true);
			player.load();
			player.addEventListener(new MusicPlayerListener());
		}

		if (!IMusicPlayerService.class.getName().equals(intent.getAction())) {
			return null;
		}

		setupNotification();

		if (T) Log.v(TAG, M()+"@out: apIMusicPlayerService="+apIMusicPlayerService);
		return apIMusicPlayerService;
	}

	/*
	 * 再バインド
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * バインド解除
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	@Override
	public void onLowMemory() {
		if (T) Log.v(TAG, M()+"@in");

		showNotification(
				"Low Memory", "GAMDX",
				getString(R.string.musicplayerservice_java_onlowmemory),
				R.drawable.icon_warn,
				0,
				NOTIFY_LOWMEMORY
		);
		SharedPreferences pref = getApplicationContext().getSharedPreferences("musicplayerservice", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("killedbylowmemory", true);
		editor.commit();
		pref = null;

		if (T) Log.v(TAG, M()+"@out");
	}

	//
	/**
	 * 音楽プレイヤーイベントリスナ
	 */
	private class MusicPlayerListener implements MusicPlayerEventListener {
		@Override
		public void endPlay() {
			if (T) Log.v(TAG, M()+"@in");

			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.END_PLAY);
			sendBroadcast(intent);

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public void timerEvent(final int playAt) {
			// if (T) Log.v(TAG, M()+"@in");

			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.TIMER_IRQ);
			sendBroadcast(intent);

			// if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public void acceptMusicFile() {
			if (T) Log.v(TAG, M()+"@in");

			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.ACCEPT_MUSIC_FILE);
			sendBroadcast(intent);

			if (T) Log.v(TAG, M()+"@out");
		}

	}

	/**
	 * 通知表示
	 * @param ticker ティッカー
	 * @param title タイトル
	 * @param message メッセージ
	 * @param id ID
	 */
	private Notification showNotification(final String ticker, final String title, final String message, final int icon, final int flag, final int id) {
		if (T) Log.v(TAG, M()+"@in: ticker="+ticker+", title="+title+", message="+message+", icon="+icon+", flag="+flag+", id="+id);

		final Intent intent = new Intent(this, net.gorry.gamdx.ActivityMain.class);
		if (id == 1) {
			//
		}
		// final Notification notification = new Notification(icon, ticker, System.currentTimeMillis());
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		final PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		builder.setSmallIcon(icon)
				.setTicker(ticker)
				.setContentTitle(title)
				.setContentText(message)
				.setContentIntent(pi)
				.setWhen(System.currentTimeMillis());

		if ((flag & Notification.FLAG_ONGOING_EVENT) != 0) {
			builder.setOngoing(true);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(CHANNEL_GAMDX);
		}

		Notification notification = builder.build();
		nm.cancel(id);
		nm.notify(id, notification);

		if (T) Log.v(TAG, M()+"@out: notification="+notification);
		return notification;
	}

	/**
	 * 通知表示の消去
	 * @param id ID
	 */
	private void clearNotification(final int id) {
		if (T) Log.v(TAG, M()+"@in: id="+id);

		nm.cancel(id);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * IRCサービスAPI
	 */
	private final IMusicPlayerService.Stub apIMusicPlayerService = new IMusicPlayerService.Stub() {
		@Override
		public void shutdown() {
			if (T) Log.v(TAG, M()+"@in");

			stopSelf();

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public void reloadSetting() {
			if (T) Log.v(TAG, M()+"@in");

			Setting.load();

			if (T) Log.v(TAG, M()+"@in");
		}

		@Override
		public void receivePong() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");
			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public void clearLowMemoryNotification() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			clearNotification(NOTIFY_LOWMEMORY);

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public String getVersionString() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			try {
				final String packageName = getPackageName();
				final PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
				final String version = packageInfo.versionName;
				return version;
			} catch (final NameNotFoundException e) {
				e.printStackTrace();
			}

			if (T) Log.v(TAG, M()+"@out");
			return null;
		}

		@Override
		public int setPlayList(final String[] playList) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: playList="+playList);

			player.setPlayList(playList);

			if (T) Log.v(TAG, M()+"@out");
			return 0;
		}

/*
		@Override
		public int setPlayListAsFolder(final String path) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: path="+path);
			if (T) Log.v(TAG, M()+"@out");
			return 0;
		}
*/

		@Override
		public String[] getPlayList() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			String[] ret = player.getPlayList();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int setPlayNumber(final int n) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: n="+n);

			int ret = (player.setPlayNumber(n) ? 1 : 0);

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int setPlayNumberNext() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = (player.setPlayNumberNext() ? 1 : 0);

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int setPlayNumberPrev() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = (player.setPlayNumberPrev() ? 1 : 0);

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int getPlayNumber() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = player.getPlayNumber();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int getDuration(final int n) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = 0;

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public String getTitle(final int n) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			String ret = "";

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public void setPlayAt(final int playat, final int loop, final int fadeout) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			player.setPlayAt(playat, loop, fadeout);

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public int getPlayAt() throws RemoteException {
			// if (T) Log.v(TAG, M()+"@in");

			int ret = player.getPlayAt();

			// if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int getCurrentDuration() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = player.getCurrentDuration();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public String getCurrentTitle() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			String ret = player.getCurrentTitle();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int setPlay(final boolean f) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			player.setPlay(f);

			if (T) Log.v(TAG, M()+"@out");
			return 0;
		}

		@Override
		public boolean getPlay() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			boolean ret = player.getPlay();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int setPause(final boolean f) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			int ret = 0;

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public boolean getPause() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			boolean ret = false;

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public int playMusicFile(final String uristr) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: uristr="+uristr);

			int ret = 0;
			String[] playList = new String[1];
			playList[0] = uristr;
			if (player.setPlayList(playList)) {
				if (player.setPlayNumber(0)) {
					player.setPlay(true);
					ret = 1;
				}
			}

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public String getCurrentFileType() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			String ret = player.getCurrentFileType();

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public String getCurrentFileName(final int id) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: id="+id);

			String ret = player.getCurrentFileName(id);

			if (T) Log.v(TAG, M()+"@out: ret="+ret);
			return ret;
		}

		@Override
		public void setLastSelectedFileName(final String filename) throws RemoteException {
			if (T) Log.v(TAG, M()+"@in: filename="+filename);

			Uri uri = ActivitySelectMdxFile.getUriFromString(filename);
			player.setLastSelectedFileUri(uri);

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public String getLastSelectedFileName() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			Uri uri = player.getLastSelectedFileUri();
			String uristr = ActivitySelectMdxFile.getStringFromUri(uri);

			if (T) Log.v(TAG, M()+"@out: uristr="+uristr);
			return uristr;
		}

		@Override
		public void savePlayerStatus() throws RemoteException {
			if (T) Log.v(TAG, M()+"@in");

			player.save();

			if (T) Log.v(TAG, M()+"@out");
		}
	};

}

// [EOF]
