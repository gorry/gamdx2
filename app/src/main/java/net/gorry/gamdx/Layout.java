/**
 *
 */
package net.gorry.gamdx;

import java.text.DecimalFormat;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author gorry
 *
 */
public class Layout {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "Layout";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private static final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private static final int FP = ViewGroup.LayoutParams.FILL_PARENT;
	private final ActivityMain main;

	@SuppressWarnings("unused")
	private int mBaseLayoutWidth;
	@SuppressWarnings("unused")
	private int mBaseLayoutHeight;

	/** */
	public LinearLayout mBaseLayout;

	@SuppressWarnings("unused")
	private Boolean mNotRestore = true;


	/**
	 * コンストラクタ
	 * @param a アクティビティインスタンス
	 */
	Layout(final ActivityMain a) {
		if (T) Log.v(TAG, M()+"@in: a="+a);

		main = a;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 *  アプリの一時退避
	 * @param outState 退避先
	 */
	public void saveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * アプリの一時退避復元
	 * @param savedInstanceState 復元元
	 */
	public void restoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		mNotRestore = false;

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 回転方向の設定
	 * @param isFirst 初期化のときtrue
	 * @return 回転方向が変わったらtrue
	 */
	public boolean setOrientation(final boolean isFirst) {
		if (T) Log.v(TAG, M()+"@in: isFirst="+isFirst);

		final boolean lastOrientation = Setting.getOrientation();
		final boolean isLandscape = (main.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
		switch (Setting.rotateMode) {
			case 0:  // 常に画面の向きに合わせる
				Setting.setOrientation(isLandscape);
				break;
			case 1:  // スタート時の画面の向きで固定
				if (isFirst) {
					Setting.setOrientation(isLandscape);
				}
				break;
			case 2:  // 縦向き表示で固定
				Setting.setOrientation(false);
				break;
			case 3:  // 横向き表示で固定
				Setting.setOrientation(true);
				break;
		}
		final boolean nowOrientation = Setting.getOrientation();
		final boolean ret = (lastOrientation != nowOrientation);

		if (T) Log.v(TAG, M()+"@out: ret="+ret);
		return ret;
	}

	/**
	 * 回転モードの設定
	 */
	public void setRotateMode() {
		if (T) Log.v(TAG, M()+"@in");

		final boolean isLandscape = Setting.getOrientation();
		switch (Setting.rotateMode) {
			default:
			case 0:
				main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				break;
			case 1:
				if (isLandscape) {
					main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
				break;
			case 2:
				main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			case 3:
				main.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ベースレイアウトのアップデート
	 */
	public void updateBaseLayout() {
		if (T) Log.v(TAG, M()+"@in");

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * ベースレイアウト作成
	 * @param isFirst 最初ならtrue
	 */
	public synchronized void baseLayout_Create(final boolean isFirst) {
		if (T) Log.v(TAG, M()+"@in: isFirst="+isFirst);

		if (mBaseLayout != null) {
			mBaseLayout.removeAllViews();
		} else {
			mBaseLayout = new LinearLayout(main) {
				@Override
				protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
					if (V) Log.v(TAG, "onSizeChanged() baseLayout");
					mBaseLayoutWidth = w;
					mBaseLayoutHeight = h;
				}
			};
			mBaseLayout.setOrientation(LinearLayout.VERTICAL);
			mBaseLayout.setLayoutParams(new LinearLayout.LayoutParams(FP, FP));
		}
		musicInfoLayout_Create(mBaseLayout, isFirst);
		musicPlayerUILayout_Create(mBaseLayout, isFirst);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * レイアウトのコンフィギュレーション変更
	 * @param newConfig 新内容
	 */
	public synchronized void changeConfiguration(final Configuration newConfig) {
		if (T) Log.v(TAG, M()+"@in: newConfig="+newConfig);

		final boolean isChanged = setOrientation(false);
		if (isChanged) {
			setRotateMode();
			baseLayout_Create(false);
			musicInfoLayout_Update();
		}
		mBaseLayout.requestLayout();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * レイアウトの再作成処理
	 */
	public synchronized void rebootLayout() {
		if (T) Log.v(TAG, M()+"@in");

		final boolean isChanged = setOrientation(true);
		if (isChanged || (Setting.rotateMode == 0)) {
			setRotateMode();
			baseLayout_Create(false);
		}

		if (T) Log.v(TAG, M()+"@out");
	}



	private LinearLayout mMusicInfoLayout;
	private TextView mTitleText;
	private TextView mDurationText;
	private TextView mPlayTimeText;
	private TextView mFileTypeText;
	private TextView mMainFileText;
	private TextView mSubFileText;

	/**
	 * 曲情報レイアウト作成
	 * @param baseLayout ベース
	 * @param isFirst 最初ならtrue
	 */
	public void musicInfoLayout_Create(final LinearLayout baseLayout, final boolean isFirst) {
		if (T) Log.v(TAG, M()+"@in: baseLayout="+baseLayout+", isFirst="+isFirst);

		mMusicInfoLayout = new LinearLayout(main);
		mMusicInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
		mMusicInfoLayout.setOrientation(LinearLayout.VERTICAL);
		baseLayout.addView(mMusicInfoLayout);

		mTitleText = new TextView(main);
		mTitleText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mTitleText);

		mDurationText = new TextView(main);
		mDurationText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mDurationText);

		mPlayTimeText = new TextView(main);
		mPlayTimeText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mPlayTimeText);

		mFileTypeText = new TextView(main);
		mFileTypeText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mFileTypeText);

		mMainFileText = new TextView(main);
		mMainFileText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mMainFileText);

		mSubFileText = new TextView(main);
		mSubFileText.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mMusicInfoLayout.addView(mSubFileText);

		if (T) Log.v(TAG, M()+"@out");
	}

	private LinearLayout mMusicPlayerUILayout;
	private LinearLayout mButtons1Layout;
	private LinearLayout mButtons2Layout;
	private Button mPlayButton;
	private Button mStopButton;
	private Button mNextButton;
	private Button mPrevButton;
	private Button mOpenButton;
	private Button mShutdownButton;

	/**
	 * プレイヤー操作レイアウト作成
	 * @param baseLayout ベース
	 * @param isFirst 最初ならtrue
	 */
	public void musicPlayerUILayout_Create(final LinearLayout baseLayout, final boolean isFirst) {
		if (T) Log.v(TAG, M()+"@in: baseLayout="+baseLayout+", isFirst="+isFirst);

		mMusicPlayerUILayout = new LinearLayout(main);
		mMusicPlayerUILayout.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
		mMusicPlayerUILayout.setOrientation(LinearLayout.VERTICAL);
		baseLayout.addView(mMusicPlayerUILayout);

		mButtons1Layout = new LinearLayout(main);
		mButtons1Layout.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
		mButtons1Layout.setOrientation(LinearLayout.HORIZONTAL);
		mMusicPlayerUILayout.addView(mButtons1Layout);

		mOpenButton = new Button(main);
		mOpenButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mOpenButton.setText(R.string.layout_java_openbutton_text);
		mOpenButton.setOnClickListener(OnClickOpenButton);
		mButtons1Layout.addView(mOpenButton);

		mShutdownButton = new Button(main);
		mShutdownButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mShutdownButton.setText(R.string.layout_java_shutdownbutton_text);
		mShutdownButton.setOnClickListener(OnClickShutdownButton);
		mButtons1Layout.addView(mShutdownButton);

		mButtons2Layout = new LinearLayout(main);
		mButtons2Layout.setLayoutParams(new LinearLayout.LayoutParams(WC, WC));
		mButtons2Layout.setOrientation(LinearLayout.HORIZONTAL);
		mMusicPlayerUILayout.addView(mButtons2Layout);

		mPlayButton = new Button(main);
		mPlayButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mPlayButton.setText(R.string.layout_java_playbutton_text);
		mPlayButton.setOnClickListener(OnClickPlayButton);
		mButtons2Layout.addView(mPlayButton);

		mStopButton = new Button(main);
		mStopButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mStopButton.setText(R.string.layout_java_stopbutton_text);
		mStopButton.setOnClickListener(OnClickStopButton);
		mButtons2Layout.addView(mStopButton);

		mPrevButton = new Button(main);
		mPrevButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mPrevButton.setText(R.string.layout_java_prevbutton_text);
		mPrevButton.setOnClickListener(OnClickPrevButton);
		mButtons2Layout.addView(mPrevButton);

		mNextButton = new Button(main);
		mNextButton.setLayoutParams(new LinearLayout.LayoutParams(FP, WC));
		mNextButton.setText(R.string.layout_java_nextbutton_text);
		mNextButton.setOnClickListener(OnClickNextButton);
		mButtons2Layout.addView(mNextButton);

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * Openボタン
	 */
	public Button.OnClickListener OnClickOpenButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doSelectMdxFile();
		}
	};

	/**
	 * Shutdownボタン
	 */
	public Button.OnClickListener OnClickShutdownButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doQuit();
		}
	};

	/**
	 * Playボタン
	 */
	public Button.OnClickListener OnClickPlayButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doPlayMusicButton();
		}
	};

	/**
	 * Stopボタン
	 */
	public Button.OnClickListener OnClickStopButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doStopMusic();
		}
	};

	/**
	 * Nextボタン
	 */
	public Button.OnClickListener OnClickNextButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doNextMusic();
		}
	};

	/**
	 * Prevボタン
	 */
	public Button.OnClickListener OnClickPrevButton = new Button.OnClickListener() {
		@Override
		public void onClick(final View v) {
			ActivityMain.doMain.doPrevMusic();
		}
	};

	/**
	 * 曲情報の更新
	 */
	public void musicInfoLayout_Update() {
		if (T) Log.v(TAG, M()+"@in");

		IMusicPlayerService sv = main.getMusicPlayerService();
		if (sv == null) {
			if (T) Log.v(TAG, M()+"@out: iMusicPlayerService == null");
			return;
		}

		{
			String s = "";
			try {
				s = sv.getCurrentTitle();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			mTitleText.setText("Title: "+s);
		}

		{
			int n = 0;
			try {
				n = sv.getCurrentDuration();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			final String s = "Duration: " +
			new DecimalFormat("0").format(n/1000/60) +
			":" +
			new DecimalFormat("00").format((n/1000)%60); // +
			// "'" +
			// new DecimalFormat("000").format(duration%1000);
			mDurationText.setText(s);
		}

		{
			int n = 0;
			try {
				n = sv.getPlayAt();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			final String s = "PlayTime: " +
			new DecimalFormat("0").format(n/1000/60) +
			":" +
			new DecimalFormat("00").format((n/1000)%60); // +
			// "'" +
			// new DecimalFormat("000").format(playtime%1000);
			mPlayTimeText.setText(s);
		}

		{
			String s = "";
			try {
				s = sv.getCurrentFileType();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			mFileTypeText.setText("File Type: "+s);
		}

		{
			String s = "";
			try {
				s = sv.getCurrentFileName(0);
				Uri uri = ActivitySelectMdxFile.getUriFromString(s);
				s = ActivitySelectMdxFile.getDisplayPath(uri, Setting.mdxRootUri);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			mMainFileText.setText("MDX File: "+s);
		}

		{
			String s = "";
			try {
				s = sv.getCurrentFileName(1);
				Uri uri = ActivitySelectMdxFile.getUriFromString(s);
				s = ActivitySelectMdxFile.getDisplayPath(uri, Setting.mdxRootUri);
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
			mSubFileText.setText("PDX File: "+s);
		}

		if (T) Log.v(TAG, M()+"@out");
	}


	/**
	 * 曲情報の更新
	 */
	public void musicInfoLayout_UpdateTimer() {
		// if (T) Log.v(TAG, M()+"@in");

		IMusicPlayerService sv = main.getMusicPlayerService();
		if (sv == null) {
			return;
		}

		{
			int n = 0;
			try {
				n = sv.getPlayAt();
			} catch (final RemoteException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			final String s = "PlayTime: " +
			new DecimalFormat("0").format(n/1000/60) +
			":" +
			new DecimalFormat("00").format((n/1000)%60); // +
			// "'" +
			// new DecimalFormat("000").format(playtime%1000);
			mPlayTimeText.setText(s);
		}

		// if (T) Log.v(TAG, M()+"@out");
	}
}

// [EOF]
