/**
 *
 */
package net.gorry.gamdx;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gorry
 *
 */
public class MusicPlayer {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "MusicPlayer";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private Context me;

	private ArrayList<MusicPlayerEventListener> mListeners = new ArrayList<MusicPlayerEventListener>();

	private Uri mLastSelectedFileUri = null;
	private Uri[] mPlayList = new Uri[0];
	private int mPlayNumber;

	private Mxdrvg mMxdrvg = null;
	private boolean mAutoPlayNextMusic = false;

	/**
	 * コンストラクタ
	 * @param context コンテキスト
	 * @param connect MXDRVGサーバに接続する実体として使用するときはtrue
	 */
	public MusicPlayer(final Context context, final boolean connect) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", connect="+connect);

		me = context;
		DocumentFile mdxRootDir = DocumentFile.fromTreeUri(me, Setting.mdxRootUri);
		DocumentFile pdxDir = mdxRootDir.findFile("pdx");
		if (pdxDir == null) {
			pdxDir = mdxRootDir.findFile("PDX");
			if (pdxDir == null) {
				pdxDir = mdxRootDir;
			}
		}
		mMxdrvg = new Mxdrvg(Setting.sampleRate, (Setting.analogFilter ? 3 : 1), 1024*1024, 1024*1024*2);
		mMxdrvg.setContext(me);
		mMxdrvg.addEventListener(new MxdrvgListener());
		mMxdrvg.setPdxFolderUri(pdxDir.getUri());
		mMxdrvg.attachAudioTrack();
		mMxdrvg.attachMonitorTimer();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 破棄
	 */
	public void dispose() {
		if (T) Log.v(TAG, M()+"@in");

		mMxdrvg.detachMonitorTimer();
		mMxdrvg.detachAudioTrack();
		mMxdrvg.dispose();
		mMxdrvg = null;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 状態のセーブ
	 */
	public void save() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = me.getSharedPreferences("musicplayer", 0);
		final SharedPreferences.Editor editor = pref.edit();
		editor.putInt("PlayListSize", mPlayList.length);
		for (int i=0; i<mPlayList.length; i++) {
			editor.putString("PlayList_"+i, ActivitySelectMdxFile.getStringFromUri(mPlayList[i]));
		}
		editor.putInt("PlayNumber", mPlayNumber);
		editor.putString("LastSelectedFileName", ActivitySelectMdxFile.getStringFromUri(mLastSelectedFileUri));

		editor.commit();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 状態のロード
	 */
	public void load() {
		if (T) Log.v(TAG, M()+"@out");

		final SharedPreferences pref = me.getSharedPreferences("musicplayer", 0);
		final int playListSize = pref.getInt("PlayListSize", 0);
		mPlayList = new Uri[playListSize];
		for (int i=0; i<playListSize; i++) {
			String uristr = pref.getString("PlayList_"+i, "");
			Uri uri = ActivitySelectMdxFile.getUriFromString(uristr);
			mPlayList[i] = uri;
		}
		mPlayNumber = pref.getInt("PlayNumber", 0);
		String uristr = pref.getString("LastSelectedFileName", "");
		mLastSelectedFileUri = ActivitySelectMdxFile.getUriFromString(uristr);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * リスナへのイベント転送処理
	 */
	private class MxdrvgListener implements MxdrvgEventListener {
		@Override
		public synchronized void endPlay() {
			if (T) Log.v(TAG, M()+"@in");

			for (int i=mListeners.size()-1; i>=0; i--) {
				mListeners.get(i).endPlay();
			}
			if (mAutoPlayNextMusic) {
				if (V) Log.v(TAG, "endPlay(): play next");
				if (setPlayNumberNext()) {
					setPlay(true);
				}
			}

			if (T) Log.v(TAG, M()+"@out");
		}

		@Override
		public synchronized void timerEvent(final int playAt) {
			// if (T) Log.v(TAG, M()+"@in: playAt="+playAt);

			for (int i=mListeners.size()-1; i>=0; i--) {
				mListeners.get(i).timerEvent(playAt);
			}

			// if (T) Log.v(TAG, M()+"@out");
		}
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public void addEventListener(final MusicPlayerEventListener l) {
		if (T) Log.v(TAG, M()+"@in: l="+l);

		if (l == null) {
			throw new IllegalArgumentException("Listener is null.");
		}
		mListeners.add(l);
		if (I) Log.i(TAG, M()+"add listener "+l+": size="+mListeners.size());

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * リスナ削除処理
	 * @param l リスナ
	 * @return true 正常に削除された
	 */
	public synchronized boolean removeEventListener(final MusicPlayerEventListener l) {
		if (T) Log.v(TAG, M()+"@in: l="+l);

		if (l == null) {
			Log.e(TAG, M()+"failed: l == null");
			return false;
		}

		for (int i=mListeners.size()-1; i>=0; i--) {
			if (mListeners.get(i).equals(l)) {
				mListeners.remove(i);
				if (I) Log.i(TAG, M()+"remove listener "+l+": size="+mListeners.size());
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}
		}

		Log.e(TAG, M()+"failed: not found "+l);
		return false;
	}

	/**
	 * ファイルリストを与える
	 * @param playList MDXファイルのUri文字列のリスト
	 * @return 成功なら1
	 */
	public boolean setPlayList(final String[] playList) {
		if (T) Log.v(TAG, M()+"@in: playList="+playList);

		setPlay(false);
		mPlayList = new Uri[playList.length];
		for (int i=0; i<playList.length; i++) {
			Uri uri = ActivitySelectMdxFile.getUriFromString(playList[i]);
			mPlayList[i] = uri;
		}
		boolean ret = setPlayNumber(0);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * ファイルリストを返す
	 * @return MDXファイルのリスト
	 */
	public String[] getPlayList() {
		if (T) Log.v(TAG, M()+"@in");

		String[] playList = new String[mPlayList.length];
		for (int i=0; i<mPlayList.length; i++) {
			String uristr = ActivitySelectMdxFile.getStringFromUri(mPlayList[i]);
			playList[i] = uristr;
		}

		if (T) Log.v(TAG, M()+"@out: playList="+playList);
		return playList;
	}

	/**
	 * プレイリスト番号を与える
	 * @param n プレイリスト番号（0～リスト数-1）
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumber(final int n) {
		if (T) Log.v(TAG, M()+"@in: n="+n);

		if (mPlayList.length == 0) {
			if (T) Log.v(TAG, M()+"@out: false");
			return false;
		}
		if ((n < 0) && (n >= mPlayList.length)) {
			if (T) Log.v(TAG, M()+"@out: false");
			return false;
		}
		mPlayNumber = n;
		Uri uri = mPlayList[mPlayNumber];
		if (uri != null) {
			final boolean f = mMxdrvg.loadMdxFile(uri, false);
			if (f) {
				for (int i=mListeners.size()-1; i>=0; i--) {
					mListeners.get(i).acceptMusicFile();
				}
				if (T) Log.v(TAG, M()+"@out: true");
			return true;
			}
		}

		if (T) Log.v(TAG, M()+"@out: false");
		return false;
	}

	/**
	 * プレイリスト番号を得る
	 * @return プレイリスト番号（0～リスト数-1）
	 */
	public int getPlayNumber() {
		if (T) Log.v(TAG, M()+"@out: mPlayNumber="+mPlayNumber);
		return mPlayNumber;
	}

	/**
	 * 次のプレイリスト番号へ移る
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumberNext() {
		if (T) Log.v(TAG, M()+"@in");

		boolean ret = false;
		if (mPlayNumber+1 < mPlayList.length) {
			ret = setPlayNumber(mPlayNumber+1);
		}

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 前のプレイリスト番号へ移る
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumberPrev() {
		if (T) Log.v(TAG, M()+"@in");

		boolean ret = false;
		if (mPlayNumber-1 >= 0) {
			ret = setPlayNumber(mPlayNumber-1);
		}

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return false;
	}

	/**
	 * 再生開始/終了する
	 * @param f trueで再生開始、falseで再生終了
	 */
	public void setPlay(final boolean f) {
		if (T) Log.v(TAG, M()+"@in: f="+f);

		mAutoPlayNextMusic = f;
		mMxdrvg.setPlay(f);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 位置などを与えて再生開始する
	 * @param playat 再生位置
	 * @param loop ループ回数
	 * @param fadeout ループ後のフェードアウト
	 */
	public void setPlayAt(final int playat, final int loop, final int fadeout) {
		if (T) Log.v(TAG, M()+"@in: playat="+playat+", loop="+loop+", fadeout="+fadeout);

		mMxdrvg.setPlayAt(playat, loop, fadeout);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 再生位置を得る
	 * @return 再生位置
	 */
	public int getPlayAt() {
		int ret = mMxdrvg.getPlayAt();
		// if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 再生中かどうかを得る
	 * @return trueで再生中、falseで非再生中
	 */
	public boolean getPlay() {
		boolean ret = mMxdrvg.getPlay();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 演奏中の曲のタイトル名を得る
	 * @return タイトル名
	 */
	public String getCurrentTitle() {
		String ret = mMxdrvg.getTitle();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 演奏中の曲の長さを得る
	 * @return 曲の長さ
	 */
	public int getCurrentDuration() {
		int ret = mMxdrvg.getDuration();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return mMxdrvg.getDuration();
	}

	/**
	 * 再生が終了しているかどうかを得る
	 * @return 再生終了していたらtrue
	 */
	public boolean getTerminated() {
		boolean ret = mMxdrvg.getTerminated();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 演奏中の曲のファイルタイプを得る
	 * @return ファイルタイプ
	 */
	public String getCurrentFileType() {
		String ret = mMxdrvg.getFileType();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 演奏中の曲のファイル名を得る
	 * @param id ファイル名ID
	 * @return ファイル名
	 */
	public String getCurrentFileName(final int id) {
		if (T) Log.v(TAG, M()+"@in: id="+id);

		String ret = mMxdrvg.getFileName(id);
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 最後に選択したファイルのURIを設定する
	 * @param uri URI
	 */
	public void setLastSelectedFileUri(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		mLastSelectedFileUri = uri;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 最後に選択したファイルのURIを得る
	 * @return URI
	 */
	public Uri getLastSelectedFileUri() {
		Uri ret = mLastSelectedFileUri;
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

}
