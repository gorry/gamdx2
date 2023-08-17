package net.gorry.gamdx;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.documentfile.provider.DocumentFile;

/**
 * 
 * メインアクティビティ
 * 
 * @author gorry
 *
 */
public class ActivityMain extends Activity {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "ActivityMain";
	private static final boolean T = true; //false;
	private static final boolean V = true; //false;
	private static final boolean D = true; //false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	/** */
	public static final int ACTIVITY_SETTING = 1;
	/** */
	public static final int ACTIVITY_SELECT_MDX = 2;
	/** */
	public static final int ACTION_OPEN_DOCUMENT_TREE = 3;

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
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		layout.saveInstanceState(outState);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		layout.restoreInstanceState(savedInstanceState);

		if (T) Log.v(TAG, M()+"@out");
	}

	private Uri getMdxRootUri() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = getSharedPreferences("setting", 0);
		final String uristr = pref.getString(Setting.name_mdxRootUri, "");
		Uri mdxRootUri = ActivitySelectMdxFile.getUriFromString(uristr);

		return mdxRootUri;
	}

	private void dialogSelectMxdrvDocumentTree() {
		if (T) Log.v(TAG, M()+"@in");

		new AlertDialog.Builder(this)
			.setTitle(R.string.title_open_document_tree_mxdrv_folder)
			.setMessage(R.string.msg_open_document_tree_mxdrv_folder)
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (T) Log.v(TAG, M()+"@in: dialog="+dialog+", which="+which);

					Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					// String path = Environment.getExternalStorageDirectory().getPath() + "/mxdrv/";
					// mdxRootUri = ActivitySelectMdxFile.getUriFromString("file://"+path);
					// intent2.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mdxRootUri);
					startActivityForResult(intent2, ACTION_OPEN_DOCUMENT_TREE);

					if (T) Log.v(TAG, M()+"@out");
				}
			})
			.show();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

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

		if (Setting.mdxRootUri == null) {
			dialogSelectMxdrvDocumentTree();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (T) Log.v(TAG, M()+"@in");

		super.onRestart();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 通知アイコンからの起動でIntentが付いてるとき
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		super.onNewIntent(intent);

		mStartIntent = intent;

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (T) Log.v(TAG, M()+"@in");

		super.onStart();

		if (T) Log.v(TAG, M()+"@out");
	}

	private void afterSetupService() {
		if (T) Log.v(TAG, M()+"@in");

		layout.musicInfoLayout_Update();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		if (T) Log.v(TAG, M()+"@in");

		super.onResume();

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

		resume_iMusicPlayerService();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (T) Log.v(TAG, M()+"@in");

		super.onPause();

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

		try {
			resume_iMusicPlayerService();
			if (iMusicPlayerService != null) {
				iMusicPlayerService.savePlayerStatus();
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (T) Log.v(TAG, M()+"@in");

		super.onStop();

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

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
				e.printStackTrace();
			}
			unbindService(svcMusicPlayer);
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	//
	/*
	 * 破棄
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");

		super.onDestroy();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * コンフィギュレーション変更
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public synchronized void onConfigurationChanged(final Configuration newConfig) {
		if (T) Log.v(TAG, M()+"@in: newConfig="+newConfig);

		super.onConfigurationChanged(newConfig);

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.dismiss();

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		final Runnable doChangeConfiguration = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				layout.changeConfiguration(newConfig);
				h.post(dismissProgressDialog);

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.setTitle(getString(R.string.activitymain_java_progress_changeconfiguration));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(doChangeConfiguration);

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		h.post(showProgressDialog);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * メインウィンドウにフォーカスが移るときの処理
	 * 子レイアウトを調整するためのエントリポイントとして使う
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public synchronized void onWindowFocusChanged(final boolean b) {
		if (T) Log.v(TAG, M()+"@in: b="+b);

		layout.updateBaseLayout();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスへのバインド
	 */
	private void myBindService() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent i1 = new Intent(this, MusicPlayerService.class);
		startService(i1);
		final Intent i2 = new Intent(IMusicPlayerService.class.getName());
		final String pkgname = me.getPackageName();
		i2.setPackage(pkgname);
		bindService(i2, svcMusicPlayer, BIND_AUTO_CREATE);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスのAPI取得処理
	 */
	private final ServiceConnection svcMusicPlayer = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			if (T) Log.v(TAG, M()+"@in: service="+service);

			final IMusicPlayerService i = IMusicPlayerService.Stub.asInterface(service);
			if (V) Log.v(TAG, M()+"iMusicPlayerService loaded");
			iMusicPlayerService = i;

			if (T) Log.v(TAG, M()+"@out");
		}
		@Override
		public void onServiceDisconnected(final ComponentName name) {
			if (T) Log.v(TAG, M()+"@in: name="+name);

			iMusicPlayerService = null;

			if (T) Log.v(TAG, M()+"@out");
		}
	};

	/**
	 * 音楽プレイヤーサービスのブロードキャストレシーバー登録
	 */
	private void myRegisterReceiver() {
		if (T) Log.v(TAG, M()+"@in");

		final IntentFilter filter = new IntentFilter(MusicPlayerService.ACTION);
		registerReceiver(mReceiver, filter);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスからのブロードキャスト受信処理
	 */
	public class MusicPlayerServiceReceiver extends BroadcastReceiver {
		@Override
		public synchronized void onReceive(final Context context, final Intent intent) {
			// if (T) Log.v(TAG, M()+"@in: context="+context+", intent="+intent);

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
					if (V) Log.v(TAG, M()+"unknown message: " + msg);
					break;
				}
			}

			// if (T) Log.v(TAG, M()+"@out");
		}
	}

	/**
	 * iMusicPlayerServiceの再読み込み
	 */
	public void resume_iMusicPlayerService() {
		if (T) Log.v(TAG, M()+"@in");

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable[] setupService = new Runnable[1];
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");
				pd.dismiss();

				if (mStartIntent != null) {
					onMyNewIntent(mStartIntent);
					mStartIntent = null;
				}

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable postSetupService = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				afterSetupService();
				h.post(dismissProgressDialog);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable waitSetupService = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				new Thread(new Runnable() {
					@Override
					public void run() {
						if (T) Log.v(TAG, M()+"@in");

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

						if (T) Log.v(TAG, M()+"@out");
					}
				} ).start();

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		setupService[0] = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				myBindService();
				myRegisterReceiver();
				h.post(waitSetupService);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.setTitle(getString(R.string.activitymain_java_progress_bindservice));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(setupService[0]);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		if (iMusicPlayerService != null) {
			try {
				// サービス生存確認
				if (V) Log.v(TAG, M()+"check Service");
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
			if (V) Log.v(TAG, M()+"service is not alive");
		}
		h.post(showProgressDialog);

		if (T) Log.v(TAG, M()+"@out");
	}



	/*
	 * オプションメニュー作成
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (T) Log.v(TAG, M()+"@in: menu="+menu);

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activitymain, menu);

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	/*
	 * オプションメニュー選択処理
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (T) Log.v(TAG, M()+"@in: item="+item);

		switch (item.getItemId()) {
			case R.id.help:
			{
				doMain.doShowHelp();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.version:
			{
				doMain.doShowVersion();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.quit:
			{
				doMain.doQuit();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.setting:
			{
				doMain.doSetting();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

		}

		if (T) Log.v(TAG, M()+"@out");
		return false;
	}

	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (T) Log.v(TAG, M()+"@in: requestCode="+requestCode+", resultCode="+resultCode+", data="+data);

		super.onActivityResult(requestCode, resultCode, data);

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
				if (V) Log.v(TAG, M()+"ACTIVITY_SETTING");
				if (intentResult >= 0) {
					Setting.load();
				}
				break;

			case ACTIVITY_SELECT_MDX:
				if (V) Log.v(TAG, M()+"ACTIVITY_SELECT_MDX");
				if (extras != null) {
					final Uri uri = ActivitySelectMdxFile.getUriFromString(extras.getString("uri"));
					final int nselect = extras.getInt("nselect");
					final int nuris = extras.getInt("nuris");
					final String[] playlist = new String[nuris];
					for (int i=0; i<nuris; i++) {
						playlist[i] = extras.getString("uri_"+i);
					}
					try {
						iMusicPlayerService.setPlayList(playlist);
						iMusicPlayerService.setPlayNumber(nselect);
						iMusicPlayerService.setPlay(true);
						/*
						iMusicPlayerService.playMusicFile(path);
						 */
						String uristr = ActivitySelectMdxFile.getStringFromUri(uri);
						iMusicPlayerService.setLastSelectedFileName(uristr);
						iMusicPlayerService.savePlayerStatus();
					} catch (final RemoteException e) {
						e.printStackTrace();
					}
				}
				break;

			case ACTION_OPEN_DOCUMENT_TREE:
				if (V) Log.v(TAG, M()+"ACTION_OPEN_DOCUMENT_TREE");
				if (resultCode != RESULT_OK) {
					if (Setting.mdxRootUri == null) {
						if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
						finish();
						return;
					}
				}
				if (data.getData() == null) {
					if (T) Log.v(TAG, M()+"@out: not received treeUri");
					finish();
					return;
				}

				Uri treeUri = data.getData();
				DocumentFile file = DocumentFile.fromTreeUri(this, treeUri);
				getContentResolver().takePersistableUriPermission(
					treeUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION
				);
				Setting.mdxRootUri = file.getUri();
				Setting.save();

				break;
		}

		resume_iMusicPlayerService();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 起動時のIntent処理
	 * @param intent Intent
	 */
	public void onMyNewIntent(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		final Bundle extras = intent.getExtras();
		if (extras != null) {  // 追加項目あり
			if (intent.getAction().equals(Intent.ACTION_VIEW)) {  // ACTION_VIEW
				final String uristr = String.valueOf(intent.getData());
				if (uristr.length() > 0) {  // URIデータがある
					final Uri uri = Uri.parse(uristr);
					if (uri != null) {  // uriがvalid
						final DocumentFile file = DocumentFile.fromSingleUri(this, uri);
						if (file.isFile()) {  // fileがファイル
							final String name = file.getName().toLowerCase();
							if (name.length() >= 5) {  // ファイル名が５文字以上
								if (name.endsWith(".mdx")) {  // 拡張子が".MDX"

									if (Setting.mdxRootUri == null) {
										return;
									}
									final Intent intent2 = new Intent(
											me,
											ActivitySelectMdxFile.class
									);
									intent2.setData(uri);
									intent2.putExtra("listOnly", true);
									intent2.putExtra("selectedFileName", name);
									startActivityForResult(intent2, ActivityMain.ACTIVITY_SELECT_MDX);
								}
							}
						}
					}
				}
			}
		}

		if (T) Log.v(TAG, M()+"@out");
	}

}

// [EOF]
