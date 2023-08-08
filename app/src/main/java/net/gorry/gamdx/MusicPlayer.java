/**
 *
 */
package net.gorry.gamdx;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

	private final Context me;

	private MusicPlayerEventListener[] mListeners = new MusicPlayerEventListener[0];

	private String mLastSelectedFileName = "";
	private String[] mPlayList = null;
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
		mMxdrvg = new Mxdrvg(Setting.sampleRate, (Setting.analogFilter ? 3 : 1), 1024*64, 1024*1024);
		mMxdrvg.addEventListener(new MxdrvgListener());
		mMxdrvg.setPdxPath(Setting.mdxRootPath + "pdx/");
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
		if (mPlayList == null) {
			editor.putInt("PlayListSize", 0);
		} else {
			editor.putInt("PlayListSize", mPlayList.length);
			for (int i=0; i<mPlayList.length; i++) {
				editor.putString("PlayList_"+i, mPlayList[i]);
			}
		}
		editor.putInt("PlayNumber", mPlayNumber);
		editor.putString("LastSelectedFileName", mLastSelectedFileName);

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
		if (playListSize == 0) {
			mPlayList = null;
		} else {
			mPlayList = new String[playListSize];
			for (int i=0; i<playListSize; i++) {
				mPlayList[i] = pref.getString("PlayList_"+i, "");
			}
		}
		mPlayNumber = pref.getInt("PlayNumber", 0);
		mLastSelectedFileName = pref.getString("LastSelectedFileName", "");

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * リスナへのイベント転送処理
	 */
	private class MxdrvgListener implements MxdrvgEventListener {
		@Override
		public synchronized void endPlay() {
			if (T) Log.v(TAG, M()+"@in");

			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].endPlay();
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

			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].timerEvent(playAt);
			}

			if (T) Log.v(TAG, M()+"@out");
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
		final int len = mListeners.length;
		final MusicPlayerEventListener[] oldListeners = mListeners;
		mListeners = new MusicPlayerEventListener[len + 1];
		System.arraycopy(oldListeners, 0, mListeners, 0, len);
		mListeners[len] = l;

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

		int index = -1;
		for (int i=0; i<mListeners.length; i++) {
			if (mListeners[i].equals(l)) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			Log.e(TAG, M()+"failed: index == -1");
			return false;
		}

		mListeners[index] = null;
		final int len = mListeners.length - 1;
		final MusicPlayerEventListener[] newListeners = new MusicPlayerEventListener[len];
		for (int i=0, j=0; i<len; j++) {
			if (mListeners[j] != null) {
				newListeners[i++] = mListeners[j];
			}
		}
		mListeners = newListeners;

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	/**
	 * ファイルリストを与える
	 * @param playList MDXファイルのリスト
	 * @return 成功なら1
	 */
	public boolean setPlayList(final String[] playList) {
		if (T) Log.v(TAG, M()+"@in: playList="+playList);

		setPlay(false);
		mPlayList = playList;
		boolean ret = setPlayNumber(0);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * ファイルリストを返す
	 * @return MDXファイルのリスト
	 */
	public String[] getPlayList() {
		if (T) Log.v(TAG, M()+"@out: mPlayList="+mPlayList);
		return mPlayList;
	}

	/**
	 * プレイリスト番号を与える
	 * @param n プレイリスト番号（0～リスト数-1）
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumber(final int n) {
		if (T) Log.v(TAG, M()+"@in: n="+n);

		if ((mPlayList == null) || (mPlayList.length == 0)) {
			if (T) Log.v(TAG, M()+"@out: false");
			return false;
		}
		if ((n < 0) && (n >= mPlayList.length)) {
			if (T) Log.v(TAG, M()+"@out: false");
			return false;
		}
		mPlayNumber = n;
		if (mPlayList != null) {
			if (!mPlayList[mPlayNumber].equals("")) {
				final boolean f = mMxdrvg.loadMdxFile(mPlayList[mPlayNumber], false);
				if (f) {
					for (int i=mListeners.length-1; i>=0; i--) {
						mListeners[i].acceptMusicFile();
					}
					if (T) Log.v(TAG, M()+"@out: true");
					return true;
				}
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
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
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
	 * 最後に選択したファイルのファイル名を設定する
	 * @param filename ファイル名
	 */
	public void setLastSelectedFileName(final String filename) {
		if (T) Log.v(TAG, M()+"@in: filename="+filename);

		mLastSelectedFileName = filename;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 最後に選択したファイルのファイル名を得る
	 * @return ファイル名
	 */
	public String getLastSelectedFileName() {
		String ret = mLastSelectedFileName;
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

}
