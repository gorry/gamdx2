package net.gorry.gamdx;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * 
 * メインアクティビティ
 * 
 * @author gorry
 *
 */
public class ActivityMain extends Activity {
	private static final String TAG = "ActivityMain";
	private static final boolean V = false;

	/** */
	public static final int ACTIVITY_SETTING = 1;
	/** */
	public static final int ACTIVITY_SELECT_MDX = 2;

	/** */
	private static Activity me;

	/** */
	private Intent mStartIntent = null;

	/** */
	public static Layout layout;
	/** */
	public static DoMain doMain;

	/** */
	public static IMusicPlayerService iMusicPlayerService;

	/** */
	public static boolean mShutdownServiceOnDestroy = false;

	private final MusicPlayerServiceReceiver mReceiver = new MusicPlayerServiceReceiver();


	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (V) Log.v(TAG, "onSaveInstanceState()");
		layout.saveInstanceState(outState);
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onRestoreInstanceState()");
		layout.restoreInstanceState(savedInstanceState);
	}



	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		me = this;
		setTitle(R.string.activitymain_title);

		iMusicPlayerService = null;

		layout = new Layout(ActivityMain.this);
		doMain = new DoMain(ActivityMain.this);
		Setting.setContext(ActivityMain.this);
		layout.setOrientation(true);
		Setting.load();
		layout.setRotateMode();

		layout.baseLayout_Create(true);

		setContentView(layout.mBaseLayout);

		final Intent intent = getIntent();
		if (intent != null) {
			mStartIntent = intent;
		}
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (V) Log.v(TAG, "onRestart()");
		super.onRestart();

	}

	/*
	 * 通知アイコンからの起動でIntentが付いてるとき
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(final Intent intent) {
		if (V) Log.v(TAG, "onNewIntent()");
		super.onNewIntent(intent);

		mStartIntent = intent;
	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (V) Log.v(TAG, "onStart()");
		super.onStart();
	}

	private void afterSetupService() {
		layout.musicInfoLayout_Update();

	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		if (V) Log.v(TAG, "onResume()");
		super.onResume();

		resume_iMusicPlayerService();
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (V) Log.v(TAG, "onPause()");
		super.onPause();
		try {
			resume_iMusicPlayerService();
			if (iMusicPlayerService != null) {
				iMusicPlayerService.savePlayerStatus();
			}
		} catch (final RemoteException e) {
			//
		}
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (V) Log.v(TAG, "onStop()");
		super.onStop();
		if (mShutdownServiceOnDestroy) {
			mShutdownServiceOnDestroy = false;
			unregisterReceiver(mReceiver);
			try {
				resume_iMusicPlayerService();
				if (iMusicPlayerService != null) {
					iMusicPlayerService.shutdown();
				}
			}
			catch(final RemoteException e) {
				// エラー
			}
			unbindService(svcMusicPlayer);
		}
	}

	//
	/*
	 * 破棄
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (V) Log.v(TAG, "onDestroy()");
		super.onDestroy();
	}

	/*
	 * コンフィギュレーション変更
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public synchronized void onConfigurationChanged(final Configuration newConfig) {
		if (V) Log.v(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "onConfigurationChanged(): dismissProgressDialog");
				pd.dismiss();
			}
		};

		final Runnable doChangeConfiguration = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "onConfigurationChanged(): doChangeConfiguration");
				layout.changeConfiguration(newConfig);
				h.post(dismissProgressDialog);
			}
		};

		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "onConfigurationChanged(): showProgressDialog");
				pd.setTitle(getString(R.string.activitymain_java_progress_changeconfiguration));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(doChangeConfiguration);
			}
		};

		h.post(showProgressDialog);

	}

	/*
	 * メインウィンドウにフォーカスが移るときの処理
	 * 子レイアウトを調整するためのエントリポイントとして使う
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public synchronized void onWindowFocusChanged(final boolean b) {
		if (V) Log.v(TAG, "onWindowFocusChanged()");
		layout.updateBaseLayout();
	}

	/**
	 * 音楽プレイヤーサービスへのバインド
	 */
	private void myBindService() {
		if (V) Log.v(TAG, "myBindService()");
		final Intent i1 = new Intent(this, MusicPlayerService.class);
		startService(i1);
		final Intent i2 = new Intent(IMusicPlayerService.class.getName());
		final String pkgname = me.getPackageName();
		i2.setPackage(pkgname);
		bindService(i2, svcMusicPlayer, BIND_AUTO_CREATE);
	}

	/**
	 * 音楽プレイヤーサービスのAPI取得処理
	 */
	private final ServiceConnection svcMusicPlayer = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			if (V) Log.v(TAG, "svcMusicPlayer: onServiceConnected()");
			final IMusicPlayerService i = IMusicPlayerService.Stub.asInterface(service);
			if (V) Log.v(TAG, "svcMusicPlayer: iMusicPlayerService loaded");
			iMusicPlayerService = i;
			// mSignaliIRCServiceSetup.countDown();
		}
		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (V) Log.v(TAG, "svcMusicPlayer: onServiceDisconnected()");
			iMusicPlayerService = null;
			// layout.setIIRCService(iIRCService);
		}
	};

	/**
	 * 音楽プレイヤーサービスのブロードキャストレシーバー登録
	 */
	private void myRegisterReceiver() {
		if (V) Log.v(TAG, "myRegisterReceiver()");
		final IntentFilter filter = new IntentFilter(MusicPlayerService.ACTION);
		registerReceiver(mReceiver, filter);
	}

	/**
	 * 音楽プレイヤーサービスからのブロードキャスト受信処理
	 */
	public class MusicPlayerServiceReceiver extends BroadcastReceiver {
		@Override
		public synchronized void onReceive(final Context context, final Intent intent) {
			final Bundle extras = intent.getExtras();
			final int msg = extras.getInt("msg");
			switch (msg) {
				case MusicPlayerService.TIMER_IRQ:
				{
					layout.musicInfoLayout_UpdateTimer();
					break;
				}

				case MusicPlayerService.ACCEPT_MUSIC_FILE:
				{
					layout.musicInfoLayout_Update();
					break;
				}

				case MusicPlayerService.END_PLAY:
				{
					break;
				}

				default:
				{
					if (V) Log.v(TAG, "MusicPlayerServiceReceiver: unknown message: " + msg);
					break;
				}
			}
		}
	}

	/**
	 * iMusicPlayerServiceの再読み込み
	 */
	public void resume_iMusicPlayerService() {
		if (V) Log.v(TAG, "resume_iMusicPlayerService()");

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable[] setupService = new Runnable[1];
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): dismissProgressDialog");
				pd.dismiss();

				if (mStartIntent != null) {
					onMyNewIntent(mStartIntent);
					mStartIntent = null;
				}
			}
		};
		final Runnable postSetupService = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): postSetupService");
				afterSetupService();
				h.post(dismissProgressDialog);
			}
		};
		final Runnable waitSetupService = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): waitSetupService");
				new Thread(new Runnable() {	@Override
					public void run() {
					int count;
					for (count=0; count<100; count++) {
						if (iMusicPlayerService != null) break;
						try {
							Thread.sleep(200);
							// mSignaliIRCServiceSetup.await();
						} catch (final InterruptedException e) {
							//
						}
					}
					if (count >= 100) {
						h.post(setupService[0]);
					} else {
						h.post(postSetupService);
					}
				}} ).start();
			}
		};
		setupService[0] = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): setupService");
				myBindService();
				myRegisterReceiver();
				h.post(waitSetupService);
			}
		};
		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): showProgressDialog");
				pd.setTitle(getString(R.string.activitymain_java_progress_bindservice));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(setupService[0]);
			}
		};
		if (iMusicPlayerService != null) {
			try {
				// サービス生存確認
				if (V) Log.v(TAG, "resume_iMusicPlayerService(): check Service");
				final String packageName = getPackageName();
				final PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
				final String version = packageInfo.versionName;
				final String serviceVersion = iMusicPlayerService.getVersionString();
				if (version.equals(serviceVersion)) {
					afterSetupService();
					return;
				}
				iMusicPlayerService = null;
			} catch (final Exception e) {
				iMusicPlayerService = null;
			}
		} else {
			if (V) Log.v(TAG, "resume_iMusicPlayerService(): service is not alive");
		}
		h.post(showProgressDialog);
	}



	/*
	 * オプションメニュー作成
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (V) Log.v(TAG, "onCreateOptionMenu()");
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activitymain, menu);
		return true;
	}

	/*
	 * オプションメニュー選択処理
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (V) Log.v(TAG, "onOptionsItemSelected()");
		switch (item.getItemId()) {
			case R.id.help:
			{
				doMain.doShowHelp();
				return true;
			}

			case R.id.version:
			{
				doMain.doShowVersion();
				return true;
			}

			case R.id.quit:
			{
				doMain.doQuit();
				return true;
			}

			case R.id.setting:
			{
				doMain.doSetting();
				return true;
			}

		}
		return false;
	}

	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (V) Log.v(TAG, "onActivityResult()");

		resume_iMusicPlayerService();

		Bundle extras = null;
		int intentResult = 0;
		if (data != null) {
			extras = data.getExtras();
			if (extras != null) {
				intentResult = extras.getInt("result");
			}
		}

		switch (requestCode) {
			case ACTIVITY_SETTING:
				if (V) Log.v(TAG, "onActivityResult(): ACTIVITY_SETTING");
				if (intentResult >= 0) {
					net.gorry.gamdx.Setting.load();
				}
				break;

			case ACTIVITY_SELECT_MDX:
				if (V) Log.v(TAG, "onActivityResult(): ACTIVITY_SELECT_MDX");
				if (extras != null) {
					final String path = extras.getString("path");
					@SuppressWarnings("unused")
					final String folder = extras.getString("folder");
					final int nselect = extras.getInt("nselect");
					final int nfiles = extras.getInt("nfiles");
					final String[] files = new String[nfiles];
					for (int i=0; i<nfiles; i++) {
						files[i] = extras.getString("file"+i);
					}
					try {
						iMusicPlayerService.setPlayList(files);
						iMusicPlayerService.setPlayNumber(nselect);
						iMusicPlayerService.setPlay(true);
						/*
						iMusicPlayerService.playMusicFile(path);
						 */
						iMusicPlayerService.setLastSelectedFileName(path);
						iMusicPlayerService.savePlayerStatus();
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
				}
				break;
		}
	}

	/**
	 * 起動時のIntent処理
	 * @param intent Intent
	 */
	public void onMyNewIntent(final Intent intent) {
		if (V) Log.v(TAG, "onMyNewIntent()");
		final Bundle extras = intent.getExtras();
		if (extras != null) {  // 追加項目あり
			if (intent.getAction().equals(Intent.ACTION_VIEW)) {  // ACTION_VIEW
				final String uristr = String.valueOf(intent.getData());
				if (uristr.length() > 0) {  // URIデータがある
					final Uri uri = Uri.parse(uristr);
					if (uri != null) {  // uriがvalid
						final File file = new File(uri.getPath());
						if (file.isFile()) {  // fileがファイル
							final String path = file.getPath();
							if (path.length() >= 5) {  // ファイル名が５文字以上
								if (path.substring(path.length()-4).equalsIgnoreCase(".mdx")) {  // 拡張子が".MDX"
									final Intent intent2 = new Intent(
											me,
											ActivitySelectMdxFile.class
									);
									final String dirname = file.getParent() + "/";
									final String filename = file.getName();
									final Uri uri2 = Uri.parse("file://"+dirname);
									intent2.setData(uri2);
									intent2.putExtra("listOnly", true);
									intent2.putExtra("selectedFileName", filename);
									startActivityForResult(intent2, ActivityMain.ACTIVITY_SELECT_MDX);
								}
							}
						}
					}
				}
			}
		}
	}


}