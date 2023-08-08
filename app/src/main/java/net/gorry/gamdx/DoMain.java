/**
 *
 */
package net.gorry.gamdx;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author gorry
 *
 */
public class DoMain {
	private static final String TAG = "DoMain";
	private static final boolean V = false;
	private final ActivityMain me;

	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	DoMain(final ActivityMain a) {
		me = a;
	}

	/**
	 * 完全終了
	 */
	public void doQuit() {
		if (V) Log.v(TAG, "doQuit()");
		ActivityMain.mShutdownServiceOnDestroy = true;
		me.finish();
	}

	/**
	 * 設定へ遷移
	 */
	public void doSetting() {
		if (V) Log.v(TAG, "doSetting()");
		final Intent intent = new Intent(
				me,
				ActivitySetting.class
		);
		final boolean isLandscape = Setting.getOrientation();
		intent.putExtra("islandscape", isLandscape);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_SETTING);
	}


	/**
	 * バージョン表示
	 */
	public void doShowVersion() {
		if (V) Log.v(TAG, "doShowVersion()");
		String versionName = null;
		final PackageManager pm = me.getPackageManager();
		final String pkgname = me.getPackageName();
		try {
			PackageInfo info = null;
			info = pm.getPackageInfo(pkgname, 0);
			versionName = info.versionName;
		} catch (final NameNotFoundException e) {
			e.printStackTrace();
		}
		final AlertDialog.Builder bldr = new AlertDialog.Builder(me);
		bldr.setTitle(me.getString(R.string.app_title));
		bldr.setMessage("Version " + versionName + "\n" + me.getString(R.string.copyright))
		.setIcon(R.drawable.icon);
		bldr.create().show();
	}


	/**
	 * ファイルを開く
	 */
	public void doSelectMdxFile() {
		if (V) Log.v(TAG, "doSelectMdxFile()");
		final Intent intent = new Intent(
				me,
				ActivitySelectMdxFile.class
		);
		String lastPath = null;
		try {
			lastPath = ActivityMain.iMusicPlayerService.getLastSelectedFileName();
		} catch (final RemoteException e) {
			lastPath = null;
		}
		if ((lastPath == null) || (lastPath.length() == 0)) {
			lastPath = Setting.mdxRootPath;
		}

		final Uri uri = Uri.parse("file://" + lastPath);
		intent.setData(uri);
		me.startActivityForResult(intent, ActivityMain.ACTIVITY_SELECT_MDX);
	}


	/**
	 * 演奏開始＆一時停止
	 */
	public void doPlayMusicButton() {
		if (V) Log.v(TAG, "doPlayMusicButton()");
		try {
			final boolean p1 = ActivityMain.iMusicPlayerService.getPlay();
			final boolean p2 = ActivityMain.iMusicPlayerService.getPause();
			if (p1 && p2) {
				ActivityMain.iMusicPlayerService.setPause(false);
			} else {
				ActivityMain.iMusicPlayerService.setPause(false);
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			//
		}
	}


	/**
	 * 演奏停止
	 */
	public void doStopMusic() {
		if (V) Log.v(TAG, "doStopMusic()");
		try {
			ActivityMain.iMusicPlayerService.setPlay(false);
		} catch (final RemoteException e) {
			//
		}
	}


	/**
	 * 次の曲
	 */
	public void doNextMusic() {
		if (V) Log.v(TAG, "doNextMusic()");
		try {
			int ret;
			ret = ActivityMain.iMusicPlayerService.setPlayNumberNext();
			if (ret != 0) {
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			//
		}
	}


	/**
	 * 次の曲
	 */
	public void doPrevMusic() {
		if (V) Log.v(TAG, "doPrevMusic()");
		try {
			int ret;
			ret = ActivityMain.iMusicPlayerService.setPlayNumberPrev();
			if (ret != 0) {
				ActivityMain.iMusicPlayerService.setPlay(true);
			}
		} catch (final RemoteException e) {
			//
		}
	}

	/**
	 * ヘルプの表示
	 */
	public void doShowHelp() {
		if (V) Log.v(TAG, "doShowHelp()");
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("http://gorry.hauN.org/android/gamdx/help/"));
		me.startActivity(intent);
	}


}
