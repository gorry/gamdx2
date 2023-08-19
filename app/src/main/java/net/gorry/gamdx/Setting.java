/**
 *
 */
package net.gorry.gamdx;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.util.Log;

/**
 * @author gorry
 *
 */
public class Setting {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "Setting";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static Context me = null;
	private static boolean isLandscape = false;

	/** */
	public static int verbose;
	private static String name_verbose = "verbose";

	/** */
	public static int rotateMode;
	private static String name_rotateMode = "rotatemode";

	/** */
	public static Uri mdxRootUri;
	public static String name_mdxRootUri = "mdxrooturi";

	/** */
	public static boolean analogFilter;
	private static String name_analogFilter = "analogfilter";

	/** */
	public static int sampleRate;
	private static String name_sampleRate = "samplerate";

	/** */
	public static int bufferSize;
	private static String name_bufferSize = "buffersize";

	/**
	 * 設定消去
	 */
	public static void delete() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = me.getSharedPreferences("setting", 0);
		final SharedPreferences.Editor editor = pref.edit();

		editor.remove(name_verbose);

		editor.remove(name_rotateMode);

		editor.remove(name_mdxRootUri);
		editor.remove(name_analogFilter);
		editor.remove(name_sampleRate);
		editor.remove(name_bufferSize);

		editor.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 設定を読み込む
	 */
	public static void load() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = me.getSharedPreferences("setting", 0);

		verbose = pref.getInt(name_verbose, 0);

		rotateMode = pref.getInt(name_rotateMode, 0);

		mdxRootUri = ActivitySelectMdxFile.getUriFromString(pref.getString(name_mdxRootUri, ""));
		checkMdxRootUri();

		analogFilter = pref.getBoolean(name_analogFilter, true);

		final int defSampRate = AudioTrack.getNativeOutputSampleRate(
				AudioManager.STREAM_MUSIC
		);
		sampleRate = pref.getInt(name_sampleRate, defSampRate);

		bufferSize = pref.getInt(name_bufferSize, 4);

		setOrientation(isLandscape);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * mdxRootUriの内容チェック
	 */
	public static void checkMdxRootUri() {
		if (T) Log.v(TAG, M()+"@in");

/*
		if (mdxRootUri.length() == 0) {
			// 空のときは初期値に戻す
			mdxRootUri = mDefMdxRootUri;
		} else if (!(mdxRootUri.substring(mdxRootUri.length()-1).equals("/"))) {
			// "/"で終わってなかったら追加
			mdxRootUri += "/";
		}
*/

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 設定を保存する
	 */
	public static void save() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = me.getSharedPreferences("setting", 0);
		final SharedPreferences.Editor editor = pref.edit();

		editor.putInt(name_verbose, verbose);

		editor.putInt(name_rotateMode, rotateMode);

		editor.putString(name_mdxRootUri, ActivitySelectMdxFile.getStringFromUri(mdxRootUri));
		editor.putBoolean(name_analogFilter, analogFilter);
		editor.putInt(name_sampleRate, sampleRate);
		editor.putInt(name_bufferSize, bufferSize);

		editor.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * コンテキスト設定
	 * @param context コンテキスト
	 */
	public static void setContext(final Context context) {
		if (T) Log.v(TAG, M()+"@out: context="+context);

		if (me == null) {
			me = context;
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 回転方向設定
	 * @param orientation 横長ならtrue
	 */
	public static void setOrientation(final boolean orientation) {
		if (T) Log.v(TAG, M()+"@in: orientation="+orientation);

		isLandscape = orientation;
		if (isLandscape) {
			//
		} else {
			//
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 現在の回転方向取得
	 * @return 回転方向 横長ならtrue
	 */
	public static boolean getOrientation() {
		if (T) Log.v(TAG, M()+"@out: isLandscape="+isLandscape);
		return isLandscape;
	}

	/**
	 * PreferenceActivityから設定を転送
	 * @param sp 情報入出力先
	 * @return リブートレベル（bit0=要画面再構成、bit1=要サービス再起動、bit2=ログクリア）
	 */
	public static int getFromPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);

		int rebootLevel = 0;

		final int back_verbose = verbose;
		final int back_rotateMode = rotateMode;
		final String back_mdxRootUriStr = ActivitySelectMdxFile.getStringFromUri(mdxRootUri);
		final boolean back_analogFilter = analogFilter;
		final int back_sampleRate = sampleRate;
		final int back_bufferSize = bufferSize;

		verbose = sp_getInt(sp, "pref_sys_"+name_verbose, back_verbose);

		rotateMode = sp_getInt(sp, "pref_sys_"+name_rotateMode, back_rotateMode);

		mdxRootUri = ActivitySelectMdxFile.getUriFromString(sp_getString(sp, "pref_"+name_mdxRootUri, back_mdxRootUriStr));
		checkMdxRootUri();

		analogFilter = sp_getBoolean(sp, "pref_"+name_analogFilter, analogFilter);
		sampleRate = sp_getInt(sp, "pref_"+name_sampleRate, sampleRate);
		bufferSize = sp_getInt(sp, "pref_"+name_bufferSize, bufferSize);


		if (isLandscape) {
			//
		} else {
			//
		}

		if (back_mdxRootUriStr != ActivitySelectMdxFile.getStringFromUri(mdxRootUri)) rebootLevel |= 2;
		if (back_analogFilter != analogFilter) rebootLevel |= 2;
		if (back_sampleRate != sampleRate) rebootLevel |= 2;
		if (back_bufferSize != bufferSize) rebootLevel |= 2;
		if (back_verbose != verbose) rebootLevel |= 2;
		if (back_rotateMode != rotateMode) rebootLevel |= 1;

		if (T) Log.v(TAG, M()+"@out: rebootLevel="+rebootLevel);
		return rebootLevel;
	}

	private static int sp_getInt(final SharedPreferences sp, final String reg, final int defparam) {
		return Integer.valueOf(sp.getString(reg, Integer.toString(defparam)));
	}
	private static String sp_getString(final SharedPreferences sp, final String reg, final String defparam) {
		return sp.getString(reg, defparam);
	}
	private static boolean sp_getBoolean(final SharedPreferences sp, final String reg, final boolean defparam) {
		return sp.getBoolean(reg, defparam);
	}

	/**
	 * PreferenceActivityへ設定を転送
	 * @param sp 情報入出力先
	 */
	public static void setForPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);

		final SharedPreferences.Editor spe = sp.edit();

		spe_putInt(spe, "pref_sys_"+name_verbose, verbose);

		spe_putInt(spe, "pref_sys_"+name_rotateMode, rotateMode);

		spe_putString(spe, "pref_"+name_mdxRootUri, ActivitySelectMdxFile.getStringFromUri(mdxRootUri));
		spe_putBoolean(spe, "pref_"+name_analogFilter, analogFilter);
		spe_putInt(spe, "pref_"+name_sampleRate, sampleRate);
		spe_putInt(spe, "pref_"+name_bufferSize, bufferSize);

		spe.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

	private static void spe_putInt(final Editor spe, final String reg, final int param) {
		spe.putString(reg, Integer.toString(param));
	}
	private static void spe_putString(final Editor spe, final String reg, final String param) {
		spe.putString(reg, param);
	}
	private static void spe_putBoolean(final Editor spe, final String reg, final Boolean param) {
		spe.putBoolean(reg, param);
	}

	/**
	 * PreferenceActivityへ設定を転送
	 * @param sp 情報入出力先
	 */
	public static void clearForPreferenceActivity(final SharedPreferences sp) {
		if (T) Log.v(TAG, M()+"@in: sp="+sp);

		final SharedPreferences.Editor spe = sp.edit();

		spe.remove("pref_sys_"+name_verbose);

		spe.remove("pref_sys_"+name_rotateMode);

		spe.remove("pref_"+name_mdxRootUri);
		spe.remove("pref_"+name_analogFilter);
		spe.remove("pref_"+name_sampleRate);
		spe.remove("pref_"+name_bufferSize);

		spe.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

}

// [EOF]
