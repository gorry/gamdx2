package net.gorry.gamdx;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

/**
 * 
 * メインアクティビティ
 * 
 * @author gorry
 *
 */
public class ActivityMain extends AppCompatActivity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivityMain";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	/** */
	public static final int ACTIVITY_SETTING = 1;
	/** */
	public static final int ACTIVITY_SELECT_MDX = 2;
	/** */
	public static final int ACTION_OPEN_DOCUMENT_TREE = 3;

	/** */
	private static ActivityMain me;

	/** */
	public static Layout layout;
	/** */
	public static DoMain doMain;

	/** */
	public static boolean mShutdownServiceOnDestroy = false;


	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	/*
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		layout.saveInstanceState(outState);

		if (T) Log.v(TAG, M()+"@out");
	}
	*/

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	/*
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		layout.restoreInstanceState(savedInstanceState);

		if (T) Log.v(TAG, M()+"@out");
	}
	*/

	public static ActivityMain Instance() {
		return me;
	}

	public static IMusicPlayerService getMusicPlayerService() {
		// if (T) Log.v(TAG, M()+"@in");
		// if (T) Log.v(TAG, M()+"@out");
		return doMain.iMusicPlayerService;
	}

	private Uri getMdxRootUri() {
		if (T) Log.v(TAG, M()+"@in");

		final SharedPreferences pref = getSharedPreferences("setting", 0);
		final String uristr = pref.getString(Setting.name_mdxRootUri, "");
		Uri mdxRootUri = ActivitySelectMdxFile.getUriFromString(uristr);

		return mdxRootUri;
	}


	public void DialogSelectMxdrvDocumentTree() {
		if (T) Log.v(TAG, M()+"@in");

		Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		// String path = Environment.getExternalStorageDirectory().getPath() + "/mxdrv/";
		// mdxRootUri = ActivitySelectMdxFile.getUriFromString("file://"+path);
		// intent2.putExtra(DocumentsContract.EXTRA_INITIAL_URI, mdxRootUri);
		startActivityForResult(intent2, ACTION_OPEN_DOCUMENT_TREE);

		if (T) Log.v(TAG, M()+"@out");
	}

	private void DialogPreSelectMxdrvDocumentTree() {
		if (T) Log.v(TAG, M()+"@in");

		new AlertDialog.Builder(this)
			.setTitle(R.string.title_open_document_tree_mxdrv_folder)
			.setMessage(R.string.msg_open_document_tree_mxdrv_folder)
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (T) Log.v(TAG, M()+"@in: dialog="+dialog+", which="+which);

					DialogSelectMxdrvDocumentTree();

					if (T) Log.v(TAG, M()+"@out");
				}
			})
			.show();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		super.onCreate(savedInstanceState);
		me = this;
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		setTitle(R.string.activitymain_title);

		layout = new Layout(ActivityMain.this);
		doMain = new DoMain(ActivityMain.this);
		Setting.setContext(ActivityMain.this);
		layout.setOrientation(true);
		Setting.load();
		layout.setRotateMode();

		layout.baseLayout_Create(true);

		setContentView(layout.mBaseLayout);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.show();
		}

		final Intent intent = getIntent();
		if (intent != null) {
			doMain.setStartIntent(intent);
		}

		if (Setting.mdxRootUri == null) {
			DialogPreSelectMxdrvDocumentTree();
		}

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
	 * 通知アイコンからの起動でIntentが付いてるとき
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	public void onNewIntent(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		super.onNewIntent(intent);

		doMain.setStartIntent(intent);

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
	public void onResume() {
		if (T) Log.v(TAG, M()+"@in");

		super.onResume();

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

		doMain.doResumeMusicPlayerService();

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

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

		doMain.doSavePlayerService();

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

		if (Setting.mdxRootUri == null) {
			if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
			return;
		}

		if (mShutdownServiceOnDestroy) {
			mShutdownServiceOnDestroy = false;
			doMain.doUnbindPlayerService();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	//
	/*
	 * 破棄
	 * @see android.app.Activity#onDestroy()
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
	@Override
	public synchronized void onConfigurationChanged(final Configuration newConfig) {
		if (T) Log.v(TAG, M()+"@in: newConfig="+newConfig);

		super.onConfigurationChanged(newConfig);

		final ProgressDialog pd = new ProgressDialog(me);
		final Handler h = new Handler();
		final Runnable dismissProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.dismiss();

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		final Runnable doChangeConfiguration = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				layout.changeConfiguration(newConfig);
				h.post(dismissProgressDialog);

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		final Runnable showProgressDialog = new Runnable() {
			@Override
			public void run() {
				if (T) Log.v(TAG, M()+"@in");

				pd.setTitle(getString(R.string.activitymain_java_progress_changeconfiguration));
				pd.setIndeterminate(true);
				pd.setCancelable(false);
				pd.show();
				h.post(doChangeConfiguration);

				if (T) Log.v(TAG, M()+"@out");
			}
		};

		h.post(showProgressDialog);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * メインウィンドウにフォーカスが移るときの処理
	 * 子レイアウトを調整するためのエントリポイントとして使う
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public synchronized void onWindowFocusChanged(final boolean b) {
		if (T) Log.v(TAG, M()+"@in: b="+b);

		layout.updateBaseLayout();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * オプションメニュー作成
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (T) Log.v(TAG, M()+"@in: menu="+menu);

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activitymain, menu);

		if (T) Log.v(TAG, M()+"@out");
		return true;
	}

	/*
	 * オプションメニュー選択処理
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (T) Log.v(TAG, M()+"@in: item="+item);

		switch (item.getItemId()) {
			case R.id.help:
			{
				doMain.doShowHelp();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.version:
			{
				doMain.doShowVersion();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.quit:
			{
				doMain.doQuit();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

			case R.id.setting:
			{
				doMain.doSetting();
				if (T) Log.v(TAG, M()+"@out");
				return true;
			}

		}

		if (T) Log.v(TAG, M()+"@out");
		return false;
	}

	/*
	 * アクティビティの結果処理
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (T) Log.v(TAG, M()+"@in: requestCode="+requestCode+", resultCode="+resultCode+", data="+data);

		super.onActivityResult(requestCode, resultCode, data);

		Bundle extras = null;
		int intentResult = 0;
		if (data != null) {
			extras = data.getExtras();
			if (extras != null) {
				intentResult = extras.getInt("result");
			}
		}

		switch (requestCode) {
			case ACTIVITY_SETTING:
				if (V) Log.v(TAG, M()+"ACTIVITY_SETTING");
				if (intentResult >= 0) {
					Setting.load();
					final int rebootlevel = extras.getInt("rebootlevel");
					if ((rebootlevel & 2) == 2) {
						doMain.doRebootMusicPlayerService();
					}
				}
				break;

			case ACTIVITY_SELECT_MDX:
				if (V) Log.v(TAG, M()+"ACTIVITY_SELECT_MDX");
				if (extras != null) {
					doMain.onSelectMdxFile(extras);
				}
				break;

			case ACTION_OPEN_DOCUMENT_TREE:
				if (V) Log.v(TAG, M()+"ACTION_OPEN_DOCUMENT_TREE");
				if (resultCode != RESULT_OK) {
					if (Setting.mdxRootUri == null) {
						if (T) Log.v(TAG, M()+"@out: not selected mdxRootUri");
						finish();
						return;
					}
				}
				if (data.getData() == null) {
					if (T) Log.v(TAG, M()+"@out: not received treeUri");
					finish();
					return;
				}

				Uri treeUri = data.getData();
				DocumentFile file = DocumentFile.fromTreeUri(this, treeUri);
				getContentResolver().takePersistableUriPermission(
					treeUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION
				);
				Setting.mdxRootUri = file.getUri();
				Setting.save();

				break;
		}

		doMain.doResumeMusicPlayerService();

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 起動時のIntent処理
	 * @param intent Intent
	 */
	public void onMyNewIntent(final Intent intent) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent);

		final Bundle extras = intent.getExtras();
		if (extras != null) {  // 追加項目あり
			if (intent.getAction().equals(Intent.ACTION_VIEW)) {  // ACTION_VIEW
				final String uristr = String.valueOf(intent.getData());
				if (uristr.length() > 0) {  // URIデータがある
					final Uri uri = Uri.parse(uristr);
					if (uri != null) {  // uriがvalid
						final DocumentFile file = DocumentFile.fromSingleUri(this, uri);
						if (file.isFile()) {  // fileがファイル
							final String name = file.getName().toLowerCase();
							if (name.length() >= 5) {  // ファイル名が５文字以上
								if (name.endsWith(".mdx")) {  // 拡張子が".MDX"

									if (Setting.mdxRootUri == null) {
										return;
									}
									final Intent intent2 = new Intent(
											me,
											ActivitySelectMdxFile.class
									);
									intent2.setData(uri);
									intent2.putExtra("listOnly", true);
									intent2.putExtra("selectedFileName", name);
									intent2.putExtra("rootUri", ActivitySelectMdxFile.getStringFromUri(Setting.mdxRootUri));
									startActivityForResult(intent2, ActivityMain.ACTIVITY_SELECT_MDX);
								}
							}
						}
					}
				}
			}
		}

		if (T) Log.v(TAG, M()+"@out");
	}

}

// [EOF]
