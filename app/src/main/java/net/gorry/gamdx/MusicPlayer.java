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
	private static final String TAG = "MusicPlayer";
	private static final boolean V = false;
	@SuppressWarnings("unused")
	private static final boolean D = false;

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
		if (V) Log.v(TAG, "MusicPlayer()");
		me = context;
		mMxdrvg = new Mxdrvg(Setting.sampleRate, (Setting.analogFilter ? 3 : 1), 1024*64, 1024*1024);
		mMxdrvg.addEventListener(new MxdrvgListener());
		mMxdrvg.setPdxPath(Setting.mdxRootPath + "pdx/");
		mMxdrvg.attachAudioTrack();
		mMxdrvg.attachMonitorTimer();

	}

	/**
	 * 破棄
	 */
	public void dispose() {
		mMxdrvg.detachMonitorTimer();
		mMxdrvg.detachAudioTrack();
		mMxdrvg.dispose();
		mMxdrvg = null;
	}

	/**
	 * 状態のセーブ
	 */
	public void save() {
		if (V) Log.v(TAG, "save()");
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
	}

	/**
	 * 状態のロード
	 */
	public void load() {
		if (V) Log.v(TAG, "load()");
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
	}

	/**
	 * リスナへのイベント転送処理
	 */
	private class MxdrvgListener implements MxdrvgEventListener {
		@Override
		public synchronized void endPlay() {
			if (V) Log.v(TAG, "endPlay()");
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].endPlay();
			}
			if (mAutoPlayNextMusic) {
				if (V) Log.v(TAG, "endPlay(): play next");
				if (setPlayNumberNext()) {
					setPlay(true);
				}
			}
		}

		@Override
		public synchronized void timerEvent(final int playAt) {
			// if (V) Log.v(TAG, "timerEvent()");
			for (int i=mListeners.length-1; i>=0; i--) {
				mListeners[i].timerEvent(playAt);
			}
		}
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public void addEventListener(final MusicPlayerEventListener l) {
		if (V) Log.v(TAG, "addEventListener()");
		if (l == null) {
			throw new IllegalArgumentException("Listener is null.");
		}
		final int len = mListeners.length;
		final MusicPlayerEventListener[] oldListeners = mListeners;
		mListeners = new MusicPlayerEventListener[len + 1];
		System.arraycopy(oldListeners, 0, mListeners, 0, len);
		mListeners[len] = l;
	}

	/**
	 * リスナ削除処理
	 * @param l リスナ
	 * @return true 正常に削除された
	 */
	public synchronized boolean removeEventListener(final MusicPlayerEventListener l) {
		if (V) Log.v(TAG, "removeEventListener()");
		if (l == null) {
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
		return true;
	}

	/**
	 * ファイルリストを与える
	 * @param playList MDXファイルのリスト
	 * @return 成功なら1
	 */
	public boolean setPlayList(final String[] playList) {
		if (V) Log.v(TAG, "setPlayList()");

		setPlay(false);
		mPlayList = playList;
		return (setPlayNumber(0));
	}

	/**
	 * ファイルリストを返す
	 * @return MDXファイルのリスト
	 */
	public String[] getPlayList() {
		if (V) Log.v(TAG, "getPlayList()");

		return mPlayList;
	}

	/**
	 * プレイリスト番号を与える
	 * @param n プレイリスト番号（0～リスト数-1）
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumber(final int n) {
		if (V) Log.v(TAG, "setPlayNumber()");
		if (V) Log.v(TAG, "setPlayNumber(): n="+n);
		if ((mPlayList == null) || (mPlayList.length == 0)) {
			return false;
		}
		if ((n < 0) && (n >= mPlayList.length)) {
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
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * プレイリスト番号を得る
	 * @return プレイリスト番号（0～リスト数-1）
	 */
	public int getPlayNumber() {
		if (V) Log.v(TAG, "getPlayNumber()");
		return mPlayNumber;
	}

	/**
	 * 次のプレイリスト番号へ移る
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumberNext() {
		if (V) Log.v(TAG, "setPlayNumberNext()");
		if (mPlayNumber+1 < mPlayList.length) {
			return setPlayNumber(mPlayNumber+1);
		}
		return false;
	}

	/**
	 * 前のプレイリスト番号へ移る
	 * @return 成功ならtrue
	 */
	public boolean setPlayNumberPrev() {
		if (V) Log.v(TAG, "setPlayNumberPrev()");
		if (mPlayNumber-1 >= 0) {
			return setPlayNumber(mPlayNumber-1);
		}
		return false;
	}

	/**
	 * 再生開始/終了する
	 * @param f trueで再生開始、falseで再生終了
	 */
	public void setPlay(final boolean f) {
		if (V) Log.v(TAG, "setPlay()");
		mAutoPlayNextMusic = f;
		mMxdrvg.setPlay(f);
	}

	/**
	 * 位置などを与えて再生開始する
	 * @param playat 再生位置
	 * @param loop ループ回数
	 * @param fadeout ループ後のフェードアウト
	 */
	public void setPlayAt(final int playat, final int loop, final int fadeout) {
		if (V) Log.v(TAG, "setPlayAt()");
		mMxdrvg.setPlayAt(playat, loop, fadeout);
	}

	/**
	 * 再生位置を得る
	 * @return 再生位置
	 */
	public int getPlayAt() {
		if (V) Log.v(TAG, "getPlayAt()");
		return mMxdrvg.getPlayAt();
	}

	/**
	 * 再生中かどうかを得る
	 * @return trueで再生中、falseで非再生中
	 */
	public boolean getPlay() {
		if (V) Log.v(TAG, "getPlay()");
		return mMxdrvg.getPlay();
	}

	/**
	 * 演奏中の曲のタイトル名を得る
	 * @return タイトル名
	 */
	public String getCurrentTitle() {
		if (V) Log.v(TAG, "getCurrentTitle()");
		return mMxdrvg.getTitle();
	}

	/**
	 * 演奏中の曲の長さを得る
	 * @return 曲の長さ
	 */
	public int getCurrentDuration() {
		if (V) Log.v(TAG, "getCurrentDuration()");
		return mMxdrvg.getDuration();
	}

	/**
	 * 再生が終了しているかどうかを得る
	 * @return 再生終了していたらtrue
	 */
	public boolean getTerminated() {
		if (V) Log.v(TAG, "getTerminated()");
		return mMxdrvg.getTerminated();
	}

	/**
	 * 演奏中の曲のファイルタイプを得る
	 * @return ファイルタイプ
	 */
	public String getCurrentFileType() {
		if (V) Log.v(TAG, "getCurrentFileType()");
		return mMxdrvg.getFileType();
	}

	/**
	 * 演奏中の曲のファイル名を得る
	 * @param id ファイル名ID
	 * @return ファイル名
	 */
	public String getCurrentFileName(final int id) {
		if (V) Log.v(TAG, "getCurrentFileName()");
		return mMxdrvg.getFileName(id);
	}

	/**
	 * 最後に選択したファイルのファイル名を設定する
	 * @param filename ファイル名
	 */
	public void setLastSelectedFileName(final String filename) {
		if (V) Log.v(TAG, "setLastSelectedFileName()");
		mLastSelectedFileName = filename;
	}

	/**
	 * 最後に選択したファイルのファイル名を得る
	 * @return ファイル名
	 */
	public String getLastSelectedFileName() {
		if (V) Log.v(TAG, "getLastSelectedFileName()");
		return mLastSelectedFileName;
	}

}
