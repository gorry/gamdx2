/**
 *
 */
package net.gorry.gamdx;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * @author gorry
 *
 */
public class MusicPlayerService extends ForegroundService {
	private static final String TAG = "MusicPlayerService";
	private static final boolean V = false;
	private static final boolean D = false;

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

	/**
	 * コンストラクタ
	 */
	public MusicPlayerService() {
		super(NOTIFY_SYSTEM);
	}

	/*
	 * 作成
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		if (D) Log.d(TAG, "onCreate()");
		// Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		nm = super.getNotificationManager();
	}

	/*
	 * 開始
	 * @see android.app.Service#onStart(android.content.Intent, int)
	 */
	@Override
	public void onStart(final Intent intent, final int startId) {
		if (D) Log.d(TAG, "onStart()");
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			//
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
								R.drawable.icon,
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
								R.drawable.icon,
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

	}

	/*
	 * 破棄
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (D) Log.d(TAG, "onDestroy()");
		showNotification(
				"End", "GAMDX",
				getString(R.string.musicplayerservice_java_shutdownmusicplayerservice),
				R.drawable.icon,
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
	}

	/*
	 * バインド
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		if (D) Log.d(TAG, "onBind()");
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

		return apIMusicPlayerService;
	}

	/*
	 * 再バインド
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(final Intent intent) {
		if (D) Log.d(TAG, "onRebind()");
	}

	/*
	 * バインド解除
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(final Intent intent) {
		if (D) Log.d(TAG, "onUnbind()");
		return true;
	}

	@Override
	public void onLowMemory() {
		if (D) Log.d(TAG, "onLowMemory()");
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
		return;
	}

	//
	/**
	 * 音楽プレイヤーイベントリスナ
	 */
	private class MusicPlayerListener implements MusicPlayerEventListener {
		@Override
		public void endPlay() {
			if (D) Log.d(TAG, "endPlay()");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.END_PLAY);
			sendBroadcast(intent);
		}

		@Override
		public void timerEvent(final int playAt) {
			if (D) Log.d(TAG, "timerEvent()");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.TIMER_IRQ);
			sendBroadcast(intent);
		}

		@Override
		public void acceptMusicFile() {
			if (D) Log.d(TAG, "acceptMusicFile()");
			final Intent intent = new Intent(ACTION);
			intent.putExtra("msg", MusicPlayerService.ACCEPT_MUSIC_FILE);
			sendBroadcast(intent);
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
		if (D) Log.d(TAG, "showNotification()");
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

		Notification notification = builder.build();
		nm.cancel(id);
		nm.notify(id, notification);
		return notification;
	}

	/**
	 * 通知表示の消去
	 * @param ticker ティッカー
	 * @param title タイトル
	 * @param message メッセージ
	 * @param id ID
	 */
	private void clearNotification(final int id) {
		if (D) Log.d(TAG, "clearNotification()");
		nm.cancel(id);
	}

	/**
	 * IRCサービスAPI
	 */
	private final IMusicPlayerService.Stub apIMusicPlayerService = new IMusicPlayerService.Stub() {
		@Override
		public void shutdown() {
			if (V) Log.v(TAG, "shutdown()");
			stopSelf();
		}

		@Override
		public void reloadSetting() {
			if (V) Log.v(TAG, "reloadSetting()");
			Setting.load();
		}

		@Override
		public void receivePong() throws RemoteException {
			if (V) Log.v(TAG, "receivePong()");
		}

		@Override
		public void clearLowMemoryNotification() throws RemoteException {
			if (V) Log.v(TAG, "clearLowMemoryNotice()");
			clearNotification(NOTIFY_LOWMEMORY);
		}

		@Override
		public String getVersionString() throws RemoteException {
			if (V) Log.v(TAG, "getVersionString()");
			try {
				final String packageName = getPackageName();
				final PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
				final String version = packageInfo.versionName;
				return version;
			} catch (final NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public int setPlayList(final String[] playList) throws RemoteException {
			if (V) Log.v(TAG, "setPlayList()");
			player.setPlayList(playList);
			return 0;
		}

		@Override
		public int setPlayListAsFolder(final String path) throws RemoteException {
			if (V) Log.v(TAG, "setPlayListAsFolder()");
			return 0;
		}

		@Override
		public String[] getPlayList() throws RemoteException {
			if (V) Log.v(TAG, "getPlayList()");
			return player.getPlayList();
		}

		@Override
		public int setPlayNumber(final int n) throws RemoteException {
			if (V) Log.v(TAG, "setPlayNumber()");
			return (player.setPlayNumber(n) ? 1 : 0);
		}

		@Override
		public int setPlayNumberNext() throws RemoteException {
			if (V) Log.v(TAG, "setPlayNumberNext()");
			return (player.setPlayNumberNext() ? 1 : 0);
		}

		@Override
		public int setPlayNumberPrev() throws RemoteException {
			if (V) Log.v(TAG, "setPlayNumberPrev()");
			return (player.setPlayNumberPrev() ? 1 : 0);
		}

		@Override
		public int getPlayNumber() throws RemoteException {
			if (V) Log.v(TAG, "getPlayNumber()");
			return player.getPlayNumber();
		}

		@Override
		public int getDuration(final int n) throws RemoteException {
			if (V) Log.v(TAG, "getDuration()");
			return 0;
		}

		@Override
		public String getTitle(final int n) throws RemoteException {
			if (V) Log.v(TAG, "getTitle()");
			return "";
		}

		@Override
		public void setPlayAt(final int playat, final int loop, final int fadeout) throws RemoteException {
			if (V) Log.v(TAG, "setPlayAt()");
			player.setPlayAt(playat, loop, fadeout);
		}

		@Override
		public int getPlayAt() throws RemoteException {
			// if (V) Log.v(TAG, "getPlayAt()");
			return player.getPlayAt();
		}

		@Override
		public int getCurrentDuration() throws RemoteException {
			if (V) Log.v(TAG, "getCurrentDuration()");
			return player.getCurrentDuration();
		}

		@Override
		public String getCurrentTitle() throws RemoteException {
			if (V) Log.v(TAG, "getCurrentTitle()");
			return player.getCurrentTitle();
		}

		@Override
		public int setPlay(final boolean f) throws RemoteException {
			if (V) Log.v(TAG, "setPlay()");
			player.setPlay(f);
			return 0;
		}

		@Override
		public boolean getPlay() throws RemoteException {
			if (V) Log.v(TAG, "getPlay()");
			return player.getPlay();
		}

		@Override
		public int setPause(final boolean f) throws RemoteException {
			if (V) Log.v(TAG, "setPause()");
			return 0;
		}

		@Override
		public boolean getPause() throws RemoteException {
			if (V) Log.v(TAG, "getPause()");
			return false;
		}

		@Override
		public int playMusicFile(final String path) throws RemoteException {
			if (V) Log.v(TAG, "playMusicFile()");
			final String playList[] = new String[1];
			playList[0] = path;
			if (player.setPlayList(playList)) {
				if (player.setPlayNumber(0)) {
					player.setPlay(true);
					return 1;
				}
			}
			return 0;
		}

		@Override
		public String getCurrentFileType() throws RemoteException {
			if (V) Log.v(TAG, "getCurrentFileType()");
			return player.getCurrentFileType();
		}

		@Override
		public String getCurrentFileName(final int id) throws RemoteException {
			if (V) Log.v(TAG, "getCurrentFileName()");
			return player.getCurrentFileName(id);
		}

		@Override
		public void setLastSelectedFileName(final String filename) throws RemoteException {
			if (V) Log.v(TAG, "setLastSelectedFileName()");
			player.setLastSelectedFileName(filename);
		}

		@Override
		public String getLastSelectedFileName() throws RemoteException {
			if (V) Log.v(TAG, "getLastSelectedFileName()");
			return player.getLastSelectedFileName();
		}

		@Override
		public void savePlayerStatus() throws RemoteException {
			if (V) Log.v(TAG, "savePlayerStatus()");
			player.save();
		}
	};

}
