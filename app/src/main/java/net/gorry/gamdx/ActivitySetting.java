/**
 *
 */
package net.gorry.gamdx;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

/**
 * @author gorry
 *
 */
public class ActivitySetting extends PreferenceActivity {
	private static final String TAG = "ActivitySetting";
	private static final boolean V = false;
	private Activity me;
	private boolean noFinishIt = false;


	/* アプリの一時退避
	 * onRestoreInstanceState()は来ないので注意
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (V) Log.v(TAG, "onSaveInstanceState()");
		noFinishIt = true;
	}

	/**
	 * 作成
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		me = this;
		setTitle(R.string.activitysetting_title);

		final Bundle extras = getIntent().getExtras();
		boolean isLandscape = false;
		if (extras != null) {
			isLandscape = extras.getBoolean("islandscape");
		}
		final int pref;
		if (isLandscape) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			pref = R.xml.activitysetting_landscape;
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			pref = R.xml.activitysetting_portrait;
		}

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
		Setting.load();
		Setting.clearForPreferenceActivity(sp);
		Setting.setForPreferenceActivity(sp);
		addPreferencesFromResource(pref);

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
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (V) Log.v(TAG, "onStart()");
		super.onStart();
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public synchronized void onResume() {
		if (V) Log.v(TAG, "onResume()");
		super.onResume();
		if (!noFinishIt) {
			//
		}
		noFinishIt = false;
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (V) Log.v(TAG, "onPause()");
		super.onPause();
		if (!noFinishIt) {
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
	}

	//
	/*
	 * 破棄
	 * @see android.preference.PreferenceActivity#onDestroy()
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
	/*
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
		if (V) Log.v(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
	}
	 */

	/*
	 * キー入力
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (V) Log.v(TAG, "onConfigurationChanged()");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// アクティビティ終了として使う
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(me);
			final int rebootLevel = Setting.getFromPreferenceActivity(sp);
			Setting.save();
			Setting.clearForPreferenceActivity(sp);
			final Intent intent = new Intent();
			intent.putExtra("rebootlevel", rebootLevel);
			setResult(RESULT_OK, intent);
			finish();
			return true;
		}
		super.onKeyDown(keyCode, event);
		return false;
	}

}
