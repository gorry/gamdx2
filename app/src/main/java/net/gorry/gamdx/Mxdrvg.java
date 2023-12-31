/**
 *
 */
package net.gorry.gamdx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import net.gorry.ndk.LibraryLoader;
import net.gorry.ndk.Natives;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Process;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

/**
 * @author gorry
 *
 */
public class Mxdrvg implements Natives.EventListener {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "Mxdrvg";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private ArrayList<MxdrvgEventListener> mListeners = new ArrayList<MxdrvgEventListener>();

	private Uri mMdxFileUri;
	private Uri mPdxFileUri;
	private String mMdxTitle;
	private Uri mPdxFolderUri;
	private int mMasterVolume = 256;

	private int mMxdrvgWaveDeltaSize = 256*4;
	private short[] mWaveBuffer = null;
	private int mMinAudioBufferSize = 0;
	private int mAudioBufferDeltaSize = 0;
	private int mAudioBufferOfs = 0;
	private boolean mStopMxdrvg = true;
	private boolean mPauseMxdrvg = true;
	private boolean mMusicTerminatedMxdrvg = false;
	private IntervalTimer mTimer = null;
	private int mBufferSize = 0;
	private int mBufferRest = 0;

	private AudioTrack mAudioTrack = null;
	private int mAudioTrackBufferSize = 0;
	
	private int mDuration = 0;

	private int mPlayAt = 0;
	private int mLoop = 2;
	private int mFadeout = 1;

	private int mAudioSamprate = 0;
	private int mPreferredAudioSamprate = 0;

	private Thread mAudioThread = null;
	private boolean mLibraryLoaded = false;

	private Context mContext;

	/**
	 * コンストラクタ
	 * @param samprate サンプリングレート
	 * @param fastmode 高速モードの選択
	 * @param mdxbufsize MDXバッファサイズ
	 * @param pdxbufsize PDXバッファサイズ
	 */
	public Mxdrvg(final int samprate, final int fastmode, final int mdxbufsize, final int pdxbufsize) {
		if (T) Log.v(TAG, M()+"@in: samprate="+samprate+", fastmode="+fastmode+", mdxbufsize="+mdxbufsize+", pdxbufsize="+pdxbufsize);

		if (!loadLibrary()) {
			Log.e(TAG, M()+"failed: loadLibrary()");
			return;
		}
		mPreferredAudioSamprate = samprate;
		Natives.ndkEntry(null);
		final int useIRQ = 1; //3; // bit0=onTerminatePlayFunc, bit1=onOPMIntFunc
		Natives.mxdrvgStart(samprate, fastmode, mdxbufsize, pdxbufsize, useIRQ);
		Natives.mxdrvgPCM8Enable(1);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 破棄
	 */
	public void dispose() {
		if (T) Log.v(TAG, M()+"@in");

		if (mTimer != null) {
			detachMonitorTimer();
		}
		if (mAudioTrack != null) {
			detachAudioTrack();
		}

		for (int i=mListeners.size()-1; i>=0; i--) {
			mListeners.remove(i);
		}
		mListeners.clear();

		Natives.mxdrvgEnd();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public void addEventListener(final MxdrvgEventListener l) {
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
	public synchronized boolean removeEventListener(final MxdrvgEventListener l) {
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
	 * コンテントリゾルバ設定
	 * @param c コンテントリゾルバ
	 */
	public void setContext(final Context c) {
		if (T) Log.v(TAG, M()+"@in: c="+c);

		mContext = c;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * PDXパス設定
	 * @param pdxUri PDXディレクトリのURI
	 */
	public void setPdxFolderUri(final Uri pdxUri) {
		if (T) Log.v(TAG, M()+"@in: pdxUri="+pdxUri);

		mPdxFolderUri = pdxUri;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * オーディオトラックの設置
	 */
	public synchronized void attachAudioTrack() {
		if (T) Log.v(TAG, M()+"@in");

		// 効果ないのでカット
		Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

		configAudioTrack();

		mBufferSize = Setting.bufferSize;
		if (mBufferSize < 1) mBufferSize = 1;
		mMinAudioBufferSize /= 1;
		mAudioTrackBufferSize = mMinAudioBufferSize * mBufferSize;
		if (V) Log.v(TAG, "Mxdrvg(): mAudioTrackBufferSize=" + mAudioTrackBufferSize);
		mAudioBufferDeltaSize = mAudioTrackBufferSize/4;
		if (V) Log.v(TAG, "Mxdrvg(): mAudioTrackBufferDeltaSize=" + mAudioBufferDeltaSize);
		if (V) Log.v(TAG, "Mxdrvg(): mMxdrvgWaveDeltaSize=" + mMxdrvgWaveDeltaSize);
		mWaveBuffer = new short[(mAudioTrackBufferSize+mMxdrvgWaveDeltaSize)*2];

		try {
			mAudioTrack = new AudioTrack(
					AudioManager.STREAM_MUSIC,
					mAudioSamprate,
					AudioFormat.CHANNEL_CONFIGURATION_STEREO,
					AudioFormat.ENCODING_PCM_16BIT,
					mAudioTrackBufferSize,
					AudioTrack.MODE_STREAM
			);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		if (mAudioTrack == null) {
			Log.e(TAG, M()+"failed: AudioTrack() error");
			return;
		}

		mAudioThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");
				Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
				mAudioBufferOfs = 0;
				mBufferRest = 0;

				mAudioTrack.setPlaybackPositionUpdateListener(
						new AudioTrack.OnPlaybackPositionUpdateListener() {
							@Override
							public void onMarkerReached(final AudioTrack track) {
								if (T) Log.v(TAG, M()+"@in: track="+track);
								/*
								// Galaxt TabではTHREAD_PRIORITY_URGENT_AUDIOにしても音が切れる・・・
								// Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

								if (mAudioTrack == null) return;
								// Log.d(TAG, "Mxdrvg(): run(): onMarkerReached(): store " + mAudioTrackBufferDeltaSize + " samples");
								fillAudioTrack();
								mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
								// mAudioTrack.setNotificationMarkerPosition(1);
								*/

								if (T) Log.v(TAG, M()+"@out");
							}

							@Override
							public void onPeriodicNotification(final AudioTrack track) {
								// if (T) Log.v(TAG, M()+"@in: track="+track);
								// if (D) Log.d(TAG, "Mxdrvg(): run(): onPeriodicNotification(): mBufferRest="+mBufferRest);
								Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

								if (mAudioTrack == null) return;

								mBufferRest -= 1024;
								if (mBufferRest < mAudioBufferDeltaSize) {
									fillAudioTrack();
									mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
								}

								// if (T) Log.v(TAG, M()+"@out");
							}
						}
				);

				mAudioTrack.play();

				fillAudioTrack();
				mBufferRest = 0;
				mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
				// mAudioTrack.setNotificationMarkerPosition(1);
				mAudioTrack.setPositionNotificationPeriod(1024);

				if (T) Log.v(TAG, M()+"@out");
			}
		});
		mAudioThread.start();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * オーディオトラックの解除
	 */
	public synchronized void detachAudioTrack() {
		if (T) Log.v(TAG, M()+"@in");

		if (mAudioTrack != null) {
			mStopMxdrvg = true;
			mPauseMxdrvg = true;
			mAudioTrack.stop();
			mAudioTrack.flush();
			mAudioTrack.release();
			mAudioTrack = null;
			mWaveBuffer = null;
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * バッファにオーディオデータを注入
	 */
	public void fillAudioTrack() {
		// if (T) Log.v(TAG, M()+"@in");

		// if (V) Log.v(TAG, M()+"mAudioBufferOfs="+mAudioBufferOfs+", mAudioBufferDeltaSize="+mAudioBufferDeltaSize);
		while (mAudioBufferOfs < mAudioBufferDeltaSize) {
			int delta = mAudioBufferDeltaSize - mAudioBufferOfs;
			if (delta > mMxdrvgWaveDeltaSize) {
				delta = mMxdrvgWaveDeltaSize;
			}
			if (mPauseMxdrvg || mStopMxdrvg || mMusicTerminatedMxdrvg) {
				Arrays.fill(mWaveBuffer, mAudioBufferOfs*2, (mAudioBufferOfs+delta)*2, (short)0);
			} else {
				Natives.mxdrvgGetPCM(mWaveBuffer, mAudioBufferOfs, delta);
				// if (V) Log.v(TAG, M()+"mWaveBuffer[0]="+mWaveBuffer[0]);
				if (Natives.mxdrvgGetPlayAt() >= mDuration) {
					mMusicTerminatedMxdrvg = true;
				}
			}
			mAudioBufferOfs += delta;
			mBufferRest += delta;
		}
		mAudioBufferOfs -= mAudioBufferDeltaSize;

		// if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * オーディオトラックの調査
	 */
	public boolean configAudioTrack() {
		if (T) Log.v(TAG, M()+"@in");

		// テスト用
		// mPreferredAudioSamprate = 48000;

		final int stereo = (/*Build.VERSION.SDK_INT>=5 ? AudioFormat.CHANNEL_OUT_STEREO :*/ AudioFormat.CHANNEL_CONFIGURATION_STEREO);
		mAudioSamprate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
		if (V) Log.v(TAG, "Mxdrvg(): AudioTrack.getNativeOutputSampleRate=" + mAudioSamprate);
		switch (mAudioSamprate) {
			case 44100:
			case 48000:
				break;
			default:
				mAudioSamprate = 48000;
				break;
		}
		mMinAudioBufferSize = AudioTrack.getMinBufferSize(
				mPreferredAudioSamprate,
				stereo,
				AudioFormat.ENCODING_PCM_16BIT
		);
		if (mMinAudioBufferSize == 0) {
			mMinAudioBufferSize = AudioTrack.getMinBufferSize(
					mAudioSamprate,
					stereo,
					AudioFormat.ENCODING_PCM_16BIT
			);
		} else {
			mAudioSamprate = mPreferredAudioSamprate;
		}
		if (V) Log.v(TAG, M()+"mAudioSamprate=" + mAudioSamprate+", mMinAudioTrackBufferSize=" + mMinAudioBufferSize);
		if (mMinAudioBufferSize == 0) {
			Log.e(TAG, M()+"AudioTrack.getMinBufferSize() error");
			return false;
		}

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	public void attachMonitorTimer() {
		if (T) Log.v(TAG, M()+"@in");

		mTimer = new IntervalTimer() {
			private int lastPlayAt = 0;

			@Override
			public void onTimer(final long timerCount) {
				// if (T) Log.v(TAG, M()+"@in: timerCount="+timerCount);

				/*
				// こちらだと曲終了後のウェイトが反映されないのでナシ
				if (Natives.mxdrvgGetTerminated() != 0) {
					setPlay(false);
					mTimer.stopTimer();
					for (int i=mListeners.length-1; i>=0; i--) {
						mListeners[i].endPlay();
					}
				}
				 */
				final int playAt = Natives.mxdrvgGetPlayAt();
				if (playAt < mDuration) {
					if (lastPlayAt/1000 != playAt/1000) {
						lastPlayAt = playAt;
						for (int i=mListeners.size()-1; i>=0; i--) {
							mListeners.get(i).timerEvent(playAt);
						}
					}
				} else if (playAt > 0) {
					setPlay(false);
					mTimer.stopTimer();
					for (int i=mListeners.size()-1; i>=0; i--) {
						mListeners.get(i).endPlay();
					}
				}

				// if (T) Log.v(TAG, M()+"@out");
			}
		};
	}

	public void detachMonitorTimer() {
		if (T) Log.v(TAG, M()+"@in");

		mTimer.stopTimer();
		mTimer = null;

		if (T) Log.v(TAG, M()+"@out");
	}
	
	@Override
	protected void finalize() {
		if (T) Log.v(TAG, M()+"@in");

		try {
			super.finalize();
		} catch (final Throwable e) {
			e.printStackTrace();
		}

		// Natives.mxdrvgEnd(null);

		/*
		if (mAudioTrack != null) {
			mAudioTrack.stop();
			mAudioTrack.release();
		}
		*/

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * NDKライブラリの読み込み
	 * @return 常にtrue
	 */
	public boolean loadLibrary() {
		if (T) Log.v(TAG, M()+"@in");

		if (!mLibraryLoaded) {
			LibraryLoader.load("mxdrvg");
			Natives.setListener(this);
			mLibraryLoaded = true;
		}

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	@Override
	public void onOPMIntFunc(final int ch, final int workSize, final byte[] mdxChannelWork, final byte[] mdxGlobalWork) {
		if (T) Log.v(TAG, M()+"@in: ch="+ch+", workSize="+workSize+", mdxChannelWork="+mdxChannelWork+", mdxGlobalWork="+mdxGlobalWork);

		//

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onTerminatePlayFunc(final int i) {
		if (T) Log.v(TAG, M()+"@in: i="+i);
		//
		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ShiftJIS文字列の抽出
	 * @param b バイト配列
	 * @param from 先頭位置
	 * @param count バイト数
	 * @return UTF-8文字列
	 */
	public String getShiftJisStringFromByteArray(final byte[] b, final int from, final int count) {
		if (T) Log.v(TAG, M()+"@in: b="+b+", from="+from+", count="+count);

		String s = null;
		final byte[] a = new byte[count];
		System.arraycopy(b, from, a, 0, count);
		try {
			s = new String(a, "Shift_JIS");
		} catch (final UnsupportedEncodingException e) {
			s = "";
		}

		if (T) Log.v(TAG, M()+"@out: s="+s);
		return s;
	}

	/**
	 * MDXファイルの読み込み
	 * @param mdxUri MDXファイルのURI
	 * @param infoonly 情報取得のみを行うときはtrue
	 * @return 成功ならtrue
	 */
	public boolean loadMdxFile(final Uri mdxUri, final boolean infoonly) {
		if (T) Log.v(TAG, M()+"@in: mdxUri="+mdxUri+", infoonly="+infoonly);

		byte[] mdxFileBin = null;
		byte[] pdxFileBin = null;
		int mdxFileSize = 0;
		int pdxFileSize = 0;
		int pos;

		mMdxFileUri = null;
		mPdxFileUri = null;
		ContentResolver contentResolver = mContext.getContentResolver();

		try {
			final InputStream fin = contentResolver.openInputStream(mdxUri);
			mdxFileSize = fin.available();
			mdxFileBin = new byte[mdxFileSize];
			fin.read(mdxFileBin);
			fin.close();
		} catch (final FileNotFoundException e) {
			Log.e(TAG, M()+"MDX File [" + ActivitySelectMdxFile.getStringFromUri(mMdxFileUri) + "] not found");
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		if (mdxFileSize == 0) {
			Log.e(TAG, M()+"MDX File [" + ActivitySelectMdxFile.getStringFromUri(mMdxFileUri) + "] size error");
			return false;
		}

		try {
			pos = 0;
			while (pos < mdxFileSize) {
				if (mdxFileBin[pos] == 0x0d) break;
				if (mdxFileBin[pos] == 0x0a) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan Title");
			final int titleEndPos = pos;

			while (pos < mdxFileSize) {
				if (mdxFileBin[pos] == 0x1a) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan After Title");
			int pdxStartPos = pos;

			while (pos < mdxFileSize) {
				if (mdxFileBin[pos] == 0x00) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan PDX Name");
			pdxStartPos++;
			final int pdxEndPos = pos;

			pos++;
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan After PDX Name");
			final int mdxBodyStartPos = pos;
			final int mdxBodySize = mdxFileSize - mdxBodyStartPos;

			mMdxTitle = getShiftJisStringFromByteArray(mdxFileBin, 0, titleEndPos);

			DocumentFile mdxfile = DocumentFile.fromSingleUri(mContext, mdxUri);
			DocumentFile mdxdir = ActivitySelectMdxFile.getParentFolder(mContext, mdxfile);
			Uri pdxUri = null;
			String pdxName = "";
			DocumentFile pdxFile = null;
			if (pdxEndPos-pdxStartPos > 0) {
				pdxName = getShiftJisStringFromByteArray(mdxFileBin, pdxStartPos, pdxEndPos-pdxStartPos);
			}

			boolean havePdx = true;
			if (!infoonly) {
				if ((pdxName != null) && (pdxName.length() > 0)) {
					// MDXフォルダ内のPDXファイルを探す
					pdxFile = findPdxFile(pdxName, mdxdir);
					if (pdxFile == null) {
						if (mPdxFolderUri != null) {
							// PDXフォルダ内のPDXファイルを探す
							DocumentFile pdxdir = DocumentFile.fromTreeUri(mContext, mPdxFolderUri);
							if (pdxdir != null) {
								pdxFile = findPdxFile(pdxName, pdxdir);
							}
						}
					}
				}
			}
			if (pdxFile == null) {
				havePdx = false;
			}

			int pdxDataSize = 0;
			byte[] pdxData = null;
			if (havePdx) {
				try {
					pdxUri = pdxFile.getUri();
					final InputStream fin = contentResolver.openInputStream(pdxUri);
					pdxFileSize = fin.available();
					if (pdxFileSize == 0) {
						if (V) Log.v(TAG, "PDX File [" + pdxUri + "] not found");
						havePdx = false;
					} else {
						if (!infoonly) {
							pdxFileBin = new byte[pdxFileSize];
							fin.read(pdxFileBin);
						}
					}
					fin.close();
				} catch (final FileNotFoundException e) {
					if (V) Log.v(TAG, "PDX File [" + pdxUri + "] not found");
					havePdx = false;
				} catch (final IOException e) {
					if (V) Log.v(TAG, "PDX File [" + pdxUri + "] load error");
					havePdx = false;
				}
			}
			if (havePdx && !infoonly) {
				pdxDataSize = pdxFileSize + 8 + 2;
				pdxData = new byte[pdxDataSize];
				pdxData[0] = 0x00;
				pdxData[1] = 0x00;
				pdxData[2] = 0x00;
				pdxData[3] = 0x00;
				pdxData[4] = 0x00;
				pdxData[5] = 0x0a;
				pdxData[6] = 0x00;
				pdxData[7] = 0x02;
				pdxData[8] = 0x00;
				pdxData[9] = 0x00;
				System.arraycopy(pdxFileBin, 0, pdxData, 10, pdxFileSize);
			}

			final int mdxDataSize = mdxBodySize + 8 + 2;
			byte[] mdxData = new byte[mdxDataSize];
			mdxData[0] = 0x00;
			mdxData[1] = 0x00;
			mdxData[2] = (byte)(havePdx ? 0 : 0xff);
			mdxData[3] = (byte)(havePdx ? 0 : 0xff);
			mdxData[4] = 0x00;
			mdxData[5] = 0x0a;
			mdxData[6] = 0x00;
			mdxData[7] = 0x08;
			mdxData[8] = 0x00;
			mdxData[9] = 0x00;
			System.arraycopy(mdxFileBin, mdxBodyStartPos, mdxData, 10, mdxBodySize);

			mMdxFileUri = mdxUri;
			mPdxFileUri = pdxUri;

			mPauseMxdrvg = true;
			mStopMxdrvg = true;
			mMusicTerminatedMxdrvg = false;

			setTotalVolume(mMasterVolume);
			int ret = 0;
			if (havePdx && (pdxData != null) && (pdxDataSize > 0)) {
				ret = Natives.mxdrvgSetData(mdxData, mdxDataSize, pdxData, pdxDataSize);
			} else {
				ret = Natives.mxdrvgSetData(mdxData, mdxDataSize, null, 0);
			}
			if (ret != 0) {
				Log.e(TAG, M()+"failed: memory error");
				return false;
			}
			mdxData = null;
			pdxData = null;
			mDuration = Natives.mxdrvgMeasurePlayTime(2, 1);
		} catch (final IndexOutOfBoundsException e) {
			Log.e(TAG, M()+"failed: MDX File [" + ActivitySelectMdxFile.getStringFromUri(mdxUri) + "] "+e.getMessage());
			return false;
		}

		if (T) Log.v(TAG, M()+"@out");
		return true;

	}

	/**
	 * 音量の設定
	 * @param vol 音量
	 */
	public void setTotalVolume(final int vol) {
		if (T) Log.v(TAG, M()+"@in: vol="+vol);

		mMasterVolume = vol;
		Natives.mxdrvgTotalVolume(vol);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 音量の読み込み
	 * @return 音量
	 */
	public int getTotalVolume() {
		int ret = Natives.mxdrvgGetTotalVolume();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * チャンネルマスクの設定
	 * @param mask マスク
	 */
	public void setChannelMask(final int mask) {
		if (T) Log.v(TAG, M()+"@in: mask="+mask);

		Natives.mxdrvgChannelMask(mask);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * チャンネルマスクの読み込み
	 * @return チャンネルマスク
	 */
	public int getChannelMask() {
		int ret = Natives.mxdrvgGetChannelMask();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * PCM8使用の設定
	 * @param sw 1で使用、0で非使用
	 */
	public void setPCM8Enable(final int sw) {
		if (T) Log.v(TAG, M()+"@in: sw="+sw);

		Natives.mxdrvgPCM8Enable(sw);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * PCM8使用の読み込み
	 * @return 1で使用、0で非使用
	 */
	public int getPCM8Enable() {
		int ret = Natives.mxdrvgGetPCM8Enable();
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 演奏の開始＆停止
	 * @param f trueで演奏、falseで停止
	 */
	public void setPlay(final boolean f) {
		if (T) Log.v(TAG, M()+"@in");

		if (f) {
			Natives.mxdrvgPlay();
			Natives.mxdrvgPlayAt(mPlayAt, mLoop, mFadeout);
			mPauseMxdrvg = false;
			mStopMxdrvg = false;
			mMusicTerminatedMxdrvg = false;
			mTimer.startTimer();
		} else {
			mPauseMxdrvg = true;
			mStopMxdrvg = true;
			mTimer.stopTimer();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 演奏中かどうかを取得。Pauseかどうかは関与しない
	 * @return 演奏中ならtrue
	 */
	public boolean getPlay() {
		if (T) Log.v(TAG, M()+"@out: ret="+mStopMxdrvg);
		return !mStopMxdrvg;
	}

	/**
	 * 演奏パラメータを指定して再生開始
	 * @param playat 再生位置
	 * @param loop ループ回数
	 * @param fadeout ループ後のフェードアウト
	 */
	public void setPlayAt(final int playat, final int loop, final int fadeout) {
		if (T) Log.v(TAG, M()+"@in: playat="+playat+", loop="+loop+", fadeout="+fadeout);

		mPlayAt = playat;
		mLoop = loop;
		mFadeout = fadeout;
		Natives.mxdrvgPlayAt(playat, loop, fadeout);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 再生位置を取得
	 * @return 再生位置
	 */
	public int getPlayAt() {
		int ret = Natives.mxdrvgGetPlayAt();
		// if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 再生終了しているかどうかを取得
	 * @return 再生終了していたらtrue
	 */
	public boolean getTerminated() {
		boolean ret = (Natives.mxdrvgGetTerminated() != 0);
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * ポーズ＆ポーズ解除
	 * @param f trueでポーズ、falseでポーズ解除
	 */
	public void setPause(final boolean f) {
		if (T) Log.v(TAG, M()+"@in: f="+f);

		mPauseMxdrvg = f;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ポーズ中かどうかを取得
	 * @return 演奏中ならtrue
	 */
	public boolean getPause() {
		if (T) Log.v(TAG, M()+"@out: ret="+mPauseMxdrvg);
		return mPauseMxdrvg;
	}

	/**
	 * タイトルを取得
	 * @return タイトル名
	 */
	public String getTitle() {
		if (T) Log.v(TAG, M()+"@out: ret="+mMdxTitle);
		return mMdxTitle;
	}

	/**
	 * 曲の長さを取得
	 * @return 曲の長さ
	 */
	public int getDuration() {
		if (T) Log.v(TAG, M()+"@out: ret="+mDuration);
		return mDuration;
	}

	/**
	 * ファイルタイプを取得
	 * @return ファイルタイプ
	 */
	public String getFileType() {
		String ret = "MXDRV";
		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * ファイル名を取得
	 * @param id ファイル名ID
	 * @return ファイル名
	 */
	public String getFileName(final int id) {
		if (T) Log.v(TAG, M()+"@in: id="+id);

		String ret = null;
		switch (id) {
			case 0:
				ret = ActivitySelectMdxFile.getStringFromUri(mMdxFileUri);
				break;
			case 1:
				ret = ActivitySelectMdxFile.getStringFromUri(mPdxFileUri);
				break;
		}

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	private DocumentFile findPdxFile(String pdxName, DocumentFile dir) {
		if (T) Log.v(TAG, M()+"@in: pdxName="+pdxName+", dir="+dir);

		DocumentFile pdxFile = null;
		pdxFile = dir.findFile(pdxName);
		if ((pdxFile == null) || (!pdxFile.exists())) {
			final int extpos = pdxName.lastIndexOf('.');
			if (extpos >= 0) {
				pdxName = pdxName.substring(0, extpos);
			}
			pdxFile = dir.findFile(pdxName+".PDX");
			if ((pdxFile == null) || (!pdxFile.exists())) {
				pdxFile = dir.findFile(pdxName+".pdx");
				if ((pdxFile == null) || (!pdxFile.exists())) {
					pdxFile = dir.findFile(pdxName.toUpperCase()+".PDX");
					if ((pdxFile == null) || (!pdxFile.exists())) {
						pdxFile = dir.findFile(pdxName.toUpperCase()+".pdx");
						if ((pdxFile == null) || (!pdxFile.exists())) {
							pdxFile = dir.findFile(pdxName.toLowerCase()+".PDX");
							if ((pdxFile == null) || (!pdxFile.exists())) {
								pdxFile = dir.findFile(pdxName.toLowerCase()+".pdx");
								if ((pdxFile == null) || (!pdxFile.exists())) {
									pdxFile = null;
								}
							}
						}
					}
				}
			}
		}

		if (T) Log.v(TAG, M()+"@out: pdxFile="+pdxFile);
		return pdxFile;
	}

}

// [EOF]
