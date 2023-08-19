/**
 *
 */
package net.gorry.gamdx;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author gorry
 *
 */
public class DoMain {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "DoMain";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private final ActivityMain main;

	private final MusicPlayerServiceReceiver mReceiver = new MusicPlayerServiceReceiver();

	/** */
	public static IMusicPlayerService iMusicPlayerService;

	/** */
	private Intent mStartIntent = null;


	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	DoMain(final ActivityMain a) {
		if (T) Log.v(TAG, M()+"@in");

		main = a;

		iMusicPlayerService = null;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 完全終了
	 */
	public void doQuit() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			iMusicPlayerService.setPlay(false);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		main.mShutdownServiceOnDestroy = true;
		main.finish();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 設定へ遷移
	 */
	public void doSetting() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent(
				main,
				ActivitySetting.class
		);
		final boolean isLandscape = Setting.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		main.startActivityForResult(intent, main.ACTIVITY_SETTING);

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * バージョン表示
	 */
	public void doShowVersion() {
		if (T) Log.v(TAG, M()+"@in");

		String versionName = null;
		final PackageManager pm = main.getPackageManager();
		final String pkgname = main.getPackageName();
		try {
			PackageInfo info = null;
			info = pm.getPackageInfo(pkgname, 0);
			versionName = info.versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}
		final AlertDialog.Builder bldr = new AlertDialog.Builder(main);
		bldr.setTitle(main.getString(R.string.app_title));
		bldr.setMessage("Version " + versionName + "\n" + main.getString(R.string.copyright))
		.setIcon(R.mipmap.ic_launcher);
		bldr.create().show();

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * MDXファイルを選択する
	 */
	public void doSelectMdxFile() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent(
				main,
			ActivitySelectMdxFile.class
		);
		String uristr = null;
		try {
			uristr = iMusicPlayerService.getLastSelectedFileName();
		} catch (final RemoteException e) {
			uristr = null;
		}
		if ((uristr == null) || (uristr.length() == 0)) {
			uristr = ActivitySelectMdxFile.getStringFromUri(Setting.mdxRootUri);
		}

		final Uri uri = ActivitySelectMdxFile.getUriFromString(uristr);
		intent.setData(uri);
		intent.putExtra("rootUri", ActivitySelectMdxFile.getStringFromUri(Setting.mdxRootUri));
		main.startActivityForResult(intent, main.ACTIVITY_SELECT_MDX);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 受け取ったファイルを開く
	 */
	public void onSelectMdxFile(Bundle extras) {
		if (T) Log.v(TAG, M()+"@in: extras="+extras);

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

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 演奏開始＆一時停止
	 */
	public void doPlayMusicButton() {
		if (T) Log.v(TAG, M()+"@in");

		if (V) Log.v(TAG, "doPlayMusicButton()");
		try {
			iMusicPlayerService.setPlay(true);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
/*
		try {
			final boolean p1 = iMusicPlayerService.getPlay();
			if (p1) {
				final boolean p2 = iMusicPlayerService.getPause();
				iMusicPlayerService.setPause(!p2);
			} else {
				iMusicPlayerService.setPause(false);
				iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}
*/

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 演奏停止
	 */
	public void doStopMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			iMusicPlayerService.setPlay(false);
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 次の曲
	 */
	public void doNextMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			int ret;
			ret = iMusicPlayerService.setPlayNumberNext();
			if (ret != 0) {
				iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 前の曲
	 */
	public void doPrevMusic() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			int ret;
			ret = iMusicPlayerService.setPlayNumberPrev();
			if (ret != 0) {
				iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ヘルプの表示
	 */
	public void doShowHelp() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://gorry.hauN.org/android/gamdx/help/"));
		main.startActivity(intent);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスへのバインド
	 */
	public void doBindService() {
		if (T) Log.v(TAG, M()+"@in");

		final Intent i1 = new Intent(main, MusicPlayerService.class);
		main.startService(i1);
		final Intent i2 = new Intent(IMusicPlayerService.class.getName());
		final String pkgname = main.getPackageName();
		i2.setPackage(pkgname);
		main.bindService(i2, svcMusicPlayer, Context.BIND_AUTO_CREATE);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスのリブート
	 */
	public void doRebootMusicPlayerService() {
		if (T) Log.v(TAG, M()+"@in");

		final ProgressDialog pd = new ProgressDialog(main);
		final Handler h = new Handler();
		final Runnable[] rebootService = new Runnable[1];
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");
				pd.dismiss();
				doResumeMusicPlayerService();

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable waitShutdownService = new Runnable() {
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
							h.post(rebootService[0]);
						} else {
							h.post(dismissProgressDialog);
						}

						if (T) Log.v(TAG, M()+"@out");
					}
				} ).start();

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable shutdownService = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				doSavePlayerService();
				doUnbindPlayerService();
				h.postDelayed(waitShutdownService, 1000);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		rebootService[0] = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				doStopMusic();
				h.postDelayed(shutdownService, 1000);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.setTitle(main.getString(R.string.activitymain_java_progress_rebootservice));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(rebootService[0]);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		if (iMusicPlayerService != null) {
			h.post(showProgressDialog);
		}

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
	public void doRegisterReceiver() {
		if (T) Log.v(TAG, M()+"@in");

		final IntentFilter filter = new IntentFilter(MusicPlayerService.ACTION);
		main.registerReceiver(mReceiver, filter);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスの状態保存
	 */
	public void doSavePlayerService() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			doResumeMusicPlayerService();
			if (iMusicPlayerService != null) {
				iMusicPlayerService.savePlayerStatus();
			}
		} catch (final RemoteException e) {
			e.printStackTrace();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音楽プレイヤーサービスのアンバインド
	 */
	public void doUnbindPlayerService() {
		if (T) Log.v(TAG, M()+"@in");

		main.unregisterReceiver(mReceiver);
		try {
			doResumeMusicPlayerService();
			if (iMusicPlayerService != null) {
				iMusicPlayerService.shutdown();
			}
		}
		catch(final RemoteException e) {
			e.printStackTrace();
		}
		main.unbindService(svcMusicPlayer);

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
					main.layout.musicInfoLayout_UpdateTimer();
					break;
				}

				case MusicPlayerService.ACCEPT_MUSIC_FILE:
				{
					main.layout.musicInfoLayout_Update();
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
	public void doResumeMusicPlayerService() {
		if (T) Log.v(TAG, M()+"@in");

		final ProgressDialog pd = new ProgressDialog(main);
		final Handler h = new Handler();
		final Runnable[] setupService = new Runnable[1];
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");
				pd.dismiss();

				if (mStartIntent != null) {
					main.onMyNewIntent(mStartIntent);
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

				doBindService();
				doRegisterReceiver();
				h.post(waitSetupService);

				if (T) Log.v(TAG, M()+"@out");
			}
		};
		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.setTitle(main.getString(R.string.activitymain_java_progress_bindservice));
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
				final String packageName = main.getPackageName();
				final PackageInfo packageInfo = main.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
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

	/**
	 * iMusicPlayerServiceの再読み込みあと処理
	 */
	private void afterSetupService() {
		if (T) Log.v(TAG, M()+"@in");

		main.layout.musicInfoLayout_Update();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * doMainで使用する開始インテントの登録
	 */
	public void setStartIntent(Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		mStartIntent = intent;

		if (T) Log.v(TAG, M()+"@out");
	}



}

// [EOF]
