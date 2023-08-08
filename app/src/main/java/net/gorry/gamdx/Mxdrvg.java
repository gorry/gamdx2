/**
 *
 */
package net.gorry.gamdx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import net.gorry.ndk.LibraryLoader;
import net.gorry.ndk.Natives;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.util.Log;

/**
 * @author gorry
 *
 */
public class Mxdrvg implements Natives.EventListener {
	private static final String TAG = "Mxdrvg";
	private static final boolean V = false;
	private static final boolean D = false;

	private MxdrvgEventListener[] mListeners = new MxdrvgEventListener[0];

	private String mMdxFileName;
	private String mPdxFileName;
	private String mMdxTitle;
	private String mPdxFolderName;
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

	/**
	 * コンストラクタ
	 * @param samprate サンプリングレート
	 * @param fastmode 高速モードの選択
	 * @param mdxbufsize MDXバッファサイズ
	 * @param pdxbufsize PDXバッファサイズ
	 */
	public Mxdrvg(final int samprate, final int fastmode, final int mdxbufsize, final int pdxbufsize) {
		if (V) Log.v(TAG, "Mxdrvg()");
		if (!loadLibrary()) {
			return;
		}
		mPreferredAudioSamprate = samprate;
		Natives.ndkEntry(null);
		final int useIRQ = 1; //3; // bit0=onTerminatePlayFunc, bit1=onOPMIntFunc
		Natives.mxdrvgStart(samprate, fastmode, mdxbufsize, pdxbufsize, useIRQ);
		Natives.mxdrvgPCM8Enable(1);
	}

	/**
	 * 破棄
	 */
	public void dispose() {
		if (V) Log.v(TAG, "dispose()");
		Natives.mxdrvgEnd();
	}

	/**
	 * リスナ登録処理
	 * @param l リスナ
	 */
	public void addEventListener(final MxdrvgEventListener l) {
		if (V) Log.v(TAG, "addEventListener()");
		if (l == null) {
			throw new IllegalArgumentException("Listener is null.");
		}
		final int len = mListeners.length;
		final MxdrvgEventListener[] oldListeners = mListeners;
		mListeners = new MxdrvgEventListener[len + 1];
		System.arraycopy(oldListeners, 0, mListeners, 0, len);
		mListeners[len] = l;
	}

	/**
	 * リスナ削除処理
	 * @param l リスナ
	 * @return true 正常に削除された
	 */
	public synchronized boolean removeEventListener(final MxdrvgEventListener l) {
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
		final MxdrvgEventListener[] newListeners = new MxdrvgEventListener[len];
		for (int i=0, j=0; i<len; j++) {
			if (mListeners[j] != null) {
				newListeners[i++] = mListeners[j];
			}
		}
		mListeners = newListeners;
		return true;
	}

	/**
	 * PDXパス設定
	 * @param pdxPath PDXパス
	 */
	public void setPdxPath(final String pdxPath) {
		if (V) Log.v(TAG, "setPdxPath()");
		mPdxFolderName = pdxPath;
	}

	/**
	 * オーディオトラックの設置
	 */
	public synchronized void attachAudioTrack() {
		if (V) Log.v(TAG, "attachAudioTrack()");

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
			Log.e(TAG, "Mxdrvg(): AudioTrack() error");
			return;
		}

		mAudioThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (V) Log.v(TAG, "Mxdrvg(): run");
				Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
				mAudioBufferOfs = 0;
				mBufferRest = 0;

				mAudioTrack.setPlaybackPositionUpdateListener(
						new AudioTrack.OnPlaybackPositionUpdateListener() {
							@Override
							public void onMarkerReached(final AudioTrack track) {
								/*
								// Galaxt TabではTHREAD_PRIORITY_URGENT_AUDIOにしても音が切れる・・・
								// Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

								if (mAudioTrack == null) return;
								// Log.d(TAG, "Mxdrvg(): run(): onMarkerReached(): store " + mAudioTrackBufferDeltaSize + " samples");
								fillAudioTrack();
								mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
								// mAudioTrack.setNotificationMarkerPosition(1);
								*/
							}

							@Override
							public void onPeriodicNotification(final AudioTrack track) {
								// if (D) Log.d(TAG, "Mxdrvg(): run(): onPeriodicNotification(): mBufferRest="+mBufferRest);
								Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

								if (mAudioTrack == null) return;

								mBufferRest -= 1024;
								if (mBufferRest < mAudioBufferDeltaSize) {
									fillAudioTrack();
									mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
								}
							}
						}
				);

				mAudioTrack.play();

				fillAudioTrack();
				mBufferRest = 0;
				mAudioTrack.write(mWaveBuffer, 0, mAudioBufferDeltaSize*2);
				// mAudioTrack.setNotificationMarkerPosition(1);
				mAudioTrack.setPositionNotificationPeriod(1024);
			}
		});
		mAudioThread.start();
	}

	/**
	 * オーディオトラックの解除
	 */
	public synchronized void detachAudioTrack() {
		if (V) Log.v(TAG, "detachAudioTrack()");
		if (mAudioTrack != null) {
			mStopMxdrvg = true;
			mPauseMxdrvg = true;
			mAudioTrack.stop();
			mAudioTrack.flush();
			mAudioTrack.release();
			mAudioTrack = null;
			mWaveBuffer = null;
		}
	}

	/**
	 * バッファにオーディオデータを注入
	 */
	public void fillAudioTrack() {
		if (V) Log.v(TAG, "fillAudioTrack(): mAudioBufferOfs="+mAudioBufferOfs+", mAudioBufferDeltaSize="+mAudioBufferDeltaSize);
		while (mAudioBufferOfs < mAudioBufferDeltaSize) {
			int delta = mAudioBufferDeltaSize - mAudioBufferOfs;
			if (delta > mMxdrvgWaveDeltaSize) {
				delta = mMxdrvgWaveDeltaSize;
			}
			if (mPauseMxdrvg || mStopMxdrvg || mMusicTerminatedMxdrvg) {
				Arrays.fill(mWaveBuffer, mAudioBufferOfs*2, (mAudioBufferOfs+delta)*2, (short)0);
			} else {
				Natives.mxdrvgGetPCM(mWaveBuffer, mAudioBufferOfs, delta);
				// if (V) Log.v(TAG, "fillAudioTrack(): mWaveBuffer[0]="+mWaveBuffer[0]);
				if (Natives.mxdrvgGetPlayAt() >= mDuration) {
					mMusicTerminatedMxdrvg = true;
				}
			}
			mAudioBufferOfs += delta;
			mBufferRest += delta;
		}
		mAudioBufferOfs -= mAudioBufferDeltaSize;
	}

	/**
	 * オーディオトラックの調査
	 */
	public boolean configAudioTrack() {
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
		if (V) Log.v(TAG, "Mxdrvg(): mAudioSamprate=" + mAudioSamprate);
		if (V) Log.v(TAG, "Mxdrvg(): mMinAudioTrackBufferSize=" + mMinAudioBufferSize);
		if (mMinAudioBufferSize == 0) {
			Log.e(TAG, "Mxdrvg(): AudioTrack.getMinBufferSize() error");
			return false;
		}

		return true;
	}
	
	public void attachMonitorTimer() {
		mTimer = new IntervalTimer() {
			private int lastPlayAt = 0;

			@Override
			public void onTimer(final long timerCount) {
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
						for (int i=mListeners.length-1; i>=0; i--) {
							mListeners[i].timerEvent(playAt);
						}
					}
				} else {
					setPlay(false);
					mTimer.stopTimer();
					for (int i=mListeners.length-1; i>=0; i--) {
						mListeners[i].endPlay();
					}
				}
			}
		};
	}

	public void detachMonitorTimer() {
		mTimer.stopTimer();
		mTimer = null;
	}
	
	@Override
	protected void finalize() {
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
	}

	/**
	 * NDKライブラリの読み込み
	 * @return 常にtrue
	 */
	public boolean loadLibrary() {
		if (V) Log.v(TAG, "loadLibrary()");
		if (mLibraryLoaded) {
			return true;
		}
		LibraryLoader.load("mxdrvg");
		Natives.setListener(this);
		mLibraryLoaded = true;
		return true;
	}

	@Override
	public void onOPMIntFunc(final int ch, final int workSize, final byte[] mdxChannelWork, final byte[] mdxGlobalWork) {
		if (V) Log.v(TAG, "onOPMIntFunc()");
		//
	}

	@Override
	public void onTerminatePlayFunc(final int i) {
		if (V) Log.v(TAG, "onTerminatePlayFunc()");
		//
	}

	/**
	 * ShiftJIS文字列の抽出
	 * @param b バイト配列
	 * @param from 先頭位置
	 * @param count バイト数
	 * @return UTF-8文字列
	 */
	public String getShiftJisStringFromByteArray(final byte[] b, final int from, final int count) {
		String s = null;
		final byte[] a = new byte[count];
		System.arraycopy(b, from, a, 0, count);
		try {
			s = new String(a, "Shift_JIS");
		} catch (final UnsupportedEncodingException e) {
			s = "";
		}
		return s;
	}

	/**
	 * MDXファイルの読み込み
	 * @param mdxPath MDXファイル名
	 * @param infoonly 情報取得のみを行うときはtrue
	 * @return 成功ならtrue
	 */
	public boolean loadMdxFile(final String mdxPath, final boolean infoonly) {
		if (V) Log.v(TAG, "loadMdxFile()");

		byte[] mdxFile = null;
		byte[] pdxFile = null;
		int mdxFileSize = 0;
		int pdxFileSize = 0;
		int pos;

		mMdxFileName = null;
		mPdxFileName = null;

		try {
			final FileInputStream fin = new FileInputStream(mdxPath);
			mdxFileSize = fin.available();
			mdxFile = new byte[mdxFileSize];
			fin.read(mdxFile);
			fin.close();
		} catch (final FileNotFoundException e) {
			if (V) Log.v(TAG, "MDX File [" + mdxPath + "] not found");
			return false;
		} catch (final IOException e) {
			if (V) Log.v(TAG, "MDX File [" + mdxPath + "] load error");
			return false;
		}
		if (mdxFileSize == 0) {
			if (V) Log.v(TAG, "MDX File [" + mdxPath + "] not found");
			return false;
		}

		try {
			pos = 0;
			while (pos < mdxFileSize) {
				if (mdxFile[pos] == 0x0d) break;
				if (mdxFile[pos] == 0x0a) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan Title");
			final int titleEndPos = pos;

			while (pos < mdxFileSize) {
				if (mdxFile[pos] == 0x1a) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan After Title");
			int pdxStartPos = pos;

			while (pos < mdxFileSize) {
				if (mdxFile[pos] == 0x00) break;
				pos++;
			}
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan PDX Name");
			pdxStartPos++;
			final int pdxEndPos = pos;

			pos++;
			if (pos >= mdxFileSize) throw new IndexOutOfBoundsException("Scan After PDX Name");
			final int mdxBodyStartPos = pos;
			final int mdxBodySize = mdxFileSize - mdxBodyStartPos;

			mMdxTitle = getShiftJisStringFromByteArray(mdxFile, 0, titleEndPos);

			final String mdxFolder = mdxPath.substring(0, mdxPath.lastIndexOf('/')) + "/";
			String pdxName = "";
			String pdxFileName;
			if (pdxEndPos-pdxStartPos > 0) {
				pdxName = getShiftJisStringFromByteArray(mdxFile, pdxStartPos, pdxEndPos-pdxStartPos);
			}

			File f;
			boolean havePdx = true;
			if ((pdxName == null)||(pdxName.length() == 0)) {
				havePdx = false;
				pdxFileName = null;
			} else {
				pdxFileName = mdxFolder + pdxName;
				f = new File(pdxFileName);
				if (!f.exists()) {
					pdxFileName = mdxFolder + pdxName.toLowerCase();
					f = new File(pdxFileName);
					if (!f.exists()) {
						pdxFileName = mdxFolder + pdxName + ".pdx";
						f = new File(pdxFileName);
						if (!f.exists()) {
							pdxFileName = mdxFolder + pdxName.toLowerCase() + ".pdx";
							f = new File(pdxFileName);
							if (!f.exists()) {
								pdxFileName = mPdxFolderName + pdxName;
								f = new File(pdxFileName);
								if (!f.exists()) {
									pdxFileName = mPdxFolderName + pdxName.toLowerCase();
									f = new File(pdxFileName);
									if (!f.exists()) {
										pdxFileName = mPdxFolderName + pdxName + ".pdx";
										f = new File(pdxFileName);
										if (!f.exists()) {
											pdxFileName = mPdxFolderName + pdxName.toLowerCase() + ".pdx";
											f = new File(pdxFileName);
											if (!f.exists()) {
												pdxFileName = null;
												havePdx = false;
											}
										}
									}
								}
							}
						}
					}
				}
			}

			int pdxDataSize = 0;
			byte[] pdxData = null;
			if (havePdx) {
				try {
					final FileInputStream fin = new FileInputStream(pdxFileName);
					pdxFileSize = fin.available();
					if (pdxFileSize == 0) {
						if (V) Log.v(TAG, "PDX File [" + pdxFileName + "] not found");
						havePdx = false;
					} else {
						if (!infoonly) {
							pdxFile = new byte[pdxFileSize];
							fin.read(pdxFile);
						}
					}
					fin.close();
				} catch (final FileNotFoundException e) {
					if (V) Log.v(TAG, "PDX File [" + pdxFileName + "] not found");
					havePdx = false;
				} catch (final IOException e) {
					if (V) Log.v(TAG, "PDX File [" + pdxFileName + "] load error");
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
				System.arraycopy(pdxFile, 0, pdxData, 10, pdxFileSize);
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
			System.arraycopy(mdxFile, mdxBodyStartPos, mdxData, 10, mdxBodySize);

			mMdxFileName = mdxPath;
			mPdxFileName = pdxFileName;

			mPauseMxdrvg = true;
			mStopMxdrvg = true;
			mMusicTerminatedMxdrvg = false;

			setTotalVolume(mMasterVolume);
			if (havePdx && (pdxData != null) && (pdxDataSize > 0)) {
				Natives.mxdrvgSetData(mdxData, mdxDataSize, pdxData, pdxDataSize);
			} else {
				Natives.mxdrvgSetData(mdxData, mdxDataSize, null, 0);
			}
			mdxData = null;
			pdxData = null;
			mDuration = Natives.mxdrvgMeasurePlayTime(2, 1);
		} catch (final IndexOutOfBoundsException e) {
			if (V) Log.v(TAG, e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * 音量の設定
	 * @param vol 音量
	 */
	public void setTotalVolume(final int vol) {
		if (V) Log.v(TAG, "setTotalVolume()");
		mMasterVolume = vol;
		Natives.mxdrvgTotalVolume(vol);
	}

	/**
	 * 音量の読み込み
	 * @return 音量
	 */
	public int getTotalVolume() {
		if (V) Log.v(TAG, "getTotalVolume()");
		return Natives.mxdrvgGetTotalVolume();
	}

	/**
	 * チャンネルマスクの設定
	 * @param mask マスク
	 */
	public void setChannelMask(final int mask) {
		if (V) Log.v(TAG, "setChannelMask()");
		Natives.mxdrvgChannelMask(mask);
	}

	/**
	 * チャンネルマスクの読み込み
	 * @return チャンネルマスク
	 */
	public int getChannelMask() {
		if (V) Log.v(TAG, "getChannelMask()");
		return Natives.mxdrvgGetChannelMask();
	}

	/**
	 * PCM8使用の設定
	 * @param sw 1で使用、0で非使用
	 */
	public void setPCM8Enable(final int sw) {
		if (V) Log.v(TAG, "setPCM8Enable()");
		Natives.mxdrvgPCM8Enable(sw);
	}

	/**
	 * PCM8使用の読み込み
	 * @return 1で使用、0で非使用
	 */
	public int getPCM8Enable() {
		if (V) Log.v(TAG, "getPCM8Enable()");
		return Natives.mxdrvgGetPCM8Enable();
	}

	/**
	 * 演奏の開始＆停止
	 * @param f trueで演奏、falseで停止
	 */
	public void setPlay(final boolean f) {
		if (V) Log.v(TAG, "setPlay()");
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
	}

	/**
	 * 演奏中かどうかを取得。Pauseかどうかは関与しない
	 * @return 演奏中ならtrue
	 */
	public boolean getPlay() {
		if (V) Log.v(TAG, "getPlay()");
		return mStopMxdrvg;
	}

	/**
	 * 演奏パラメータを指定して再生開始
	 * @param playat 再生位置
	 * @param loop ループ回数
	 * @param fadeout ループ後のフェードアウト
	 */
	public void setPlayAt(final int playat, final int loop, final int fadeout) {
		if (V) Log.v(TAG, "setPlayAt()");
		mPlayAt = playat;
		mLoop = loop;
		mFadeout = fadeout;
		Natives.mxdrvgPlayAt(playat, loop, fadeout);
	}

	/**
	 * 再生位置を取得
	 * @return 再生位置
	 */
	public int getPlayAt() {
		// Log.d(TAG, "getPlayAt()");
		return Natives.mxdrvgGetPlayAt();
	}

	/**
	 * 再生終了しているかどうかを取得
	 * @return 再生終了していたらtrue
	 */
	public boolean getTerminated() {
		if (V) Log.v(TAG, "getTerminated()");
		return (Natives.mxdrvgGetTerminated() != 0);
	}

	/**
	 * ポーズ＆ポーズ解除
	 * @param f trueでポーズ、falseでポーズ解除
	 */
	public void setPause(final boolean f) {
		if (V) Log.v(TAG, "setPause()");
		if (f) {
			mPauseMxdrvg = true;
		} else {
			mPauseMxdrvg = false;
		}
	}

	/**
	 * ポーズ中かどうかを取得
	 * @return 演奏中ならtrue
	 */
	public boolean getPause() {
		if (V) Log.v(TAG, "getPause()");
		return mPauseMxdrvg;
	}

	/**
	 * タイトルを取得
	 * @return タイトル名
	 */
	public String getTitle() {
		if (V) Log.v(TAG, "getTitle()");
		return mMdxTitle;
	}

	/**
	 * 曲の長さを取得
	 * @return 曲の長さ
	 */
	public int getDuration() {
		if (V) Log.v(TAG, "getDuration()");
		return mDuration;
	}

	/**
	 * ファイルタイプを取得
	 * @return ファイルタイプ
	 */
	public String getFileType() {
		if (V) Log.v(TAG, "getFileType()");
		return "MXDRV";
	}

	/**
	 * ファイル名を取得
	 * @param id ファイル名ID
	 * @return ファイル名
	 */
	public String getFileName(final int id) {
		if (V) Log.v(TAG, "getFileName()");
		switch (id) {
			case 0:
				return mMdxFileName;
			case 1:
				return mPdxFileName;
		}
		return null;
	}

}
