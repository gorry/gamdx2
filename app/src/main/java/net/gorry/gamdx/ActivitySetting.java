/**
 *
 */
package net.gorry.gamdx;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;

/**
 * @author gorry
 *
 */
public class ActivitySetting extends PreferenceActivity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivitySetting";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private Activity me;
	private boolean noFinishIt = false;


	/* アプリの一時退避
	 * onRestoreInstanceState()は来ないので注意
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		noFinishIt = true;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 作成
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

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

		PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("pref_mdxrooturi");
		preferenceScreen.setOnPreferenceClickListener(OnPreferenceClickMdxRootUri);

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
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (T) Log.v(TAG, M()+"@in");

		super.onStart();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public synchronized void onResume() {
		if (T) Log.v(TAG, M()+"@in");

		super.onResume();
		if (!noFinishIt) {
			//
		}
		noFinishIt = false;

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
		if (!noFinishIt) {
			//
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

		if (T) Log.v(TAG, M()+"@out");
	}

	//
	/*
	 * 破棄
	 * @see android.preference.PreferenceActivity#onDestroy()
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
		if (T) Log.v(TAG, M()+"@in: keyCode="+keyCode+", event="+event);

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

		if (T) Log.v(TAG, M()+"@out");
		return false;
	}

	private Preference.OnPreferenceClickListener OnPreferenceClickMdxRootUri = new Preference.OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (T) Log.v(TAG, M()+"@in");

			ActivityMain.Instance().DialogSelectMxdrvDocumentTree();

			if (T) Log.v(TAG, M()+"@out");
			return false;
		}

	};

}

// [EOF]
