/**
 *
 */
package net.gorry.gamdx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.documentfile.provider.DocumentFile;

/**
 * 
 * ファイルリストの取得
 * 
 * @author gorry
 *
 */
public class ActivitySelectMdxFile extends ListActivity {
	private static final boolean RELEASE = !BuildConfig.DEBUG;
	private static final String TAG = "ActivitySelectMdxFile";
	private static final boolean T = !RELEASE;
	private static final boolean V = !RELEASE;
	private static final boolean D = !RELEASE;
	private static final boolean I = true;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private final ArrayList<DocumentFile> mDirEntry = new ArrayList<DocumentFile>();
	private final ArrayList<DocumentFile> mDirs = new ArrayList<DocumentFile>();
	private final ArrayList<DocumentFile> mFiles = new ArrayList<DocumentFile>();
	private DocumentFile mCurrentDir;
	private SelectMdxFileAdapter mAdapter;
	private String mExtFilenameFilter;
	private Uri mCurrentDirUri;
	private Uri mCurrentFileUri;
	private Uri mLastDirUri;
	private Thread mThreadGetInfoTask = null;
	private boolean mHasParentFolder = false;

	@SuppressWarnings("unused")
	private boolean mSelected = false;
	@SuppressWarnings("unused")
	private int mCurPos = 0;

	private Uri mRootUri;

	private Context me;

	public static String getDisplayPath(Uri uri, Uri rootUri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri+", rootUri="+rootUri);

		if (uri == null) {
			if (D) Log.v(TAG, M()+"uri is null");
			if (T) Log.v(TAG, M()+"@out: uristr=");
			return "";
		}
		if (rootUri == null) {
			if (D) Log.v(TAG, M()+"rootUri is null");
			if (T) Log.v(TAG, M()+"@out: uristr=");
			return "";
		}
		String uristr = getStringFromUri(uri);
		String uristrRoot = getStringFromUri(rootUri);
		if (!uristr.startsWith(uristrRoot)) {
			if (D) Log.v(TAG, M()+"not in RootFolder");
			if (T) Log.v(TAG, M()+"@out: uristr=");
			return "";
		}
		uristr = uristr.substring(uristrRoot.length());
		uristr = uristr.replaceAll("%2F", "/");

		if (T) Log.v(TAG, M()+"@out: uristr="+uristr);
		return uristr;
	}

	public static String getStringFromUri(Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		String uristr = "";
		if (uri != null) {
			uristr = uri.toString();
		}

		if (T) Log.v(TAG, M()+"@out: uristr="+uristr);
		return uristr;
	}

	public static Uri getUriFromString(String uristr) {
		if (T) Log.v(TAG, M()+"@in: uristr="+uristr);

		Uri uri = null;
		if ((uristr != null) && (uristr.length() > 0)) {
			uri = Uri.parse(uristr);
		}

		if (T) Log.v(TAG, M()+"@out: uri="+uri);
		return uri;
	}

	public static DocumentFile getParentFolder(Context context, DocumentFile file) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", file="+file);

		Uri rootFolderUri = Setting.mdxRootUri;
		if (rootFolderUri == null) {
			Log.e(TAG, M()+"failed: mdxRootUri is null");
			return null;
		}
		DocumentFile fileParent = DocumentFile.fromTreeUri(context, rootFolderUri);
		Uri uri = file.getUri();
		String uriStr = getStringFromUri(uri);

		String uriStrRoot = getStringFromUri(rootFolderUri);
		if (!uriStr.startsWith(uriStrRoot)) {
			if (D) Log.v(TAG, M()+"not in RootFolder");
			return fileParent;
		}
		String uriStrParent = uriStrRoot;
		int idx = uriStr.lastIndexOf("%2F");
		if (idx < uriStrRoot.length()) {
			if (D) Log.v(TAG, M()+"parent is RootFolder");
		} else {
			uriStrParent = uriStr.substring(0, idx);
		}

		uri = getUriFromString(uriStrParent);
		DocumentFile file2 = DocumentFile.fromTreeUri(context, uri);
		if (file2 == null) {
			if (D) Log.v(TAG, M()+"parent is null");
		} else {
			fileParent = file2;
		}

		if (T) Log.v(TAG, M()+"@out: parent="+fileParent);
		return fileParent;
	}

	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		outState.putString("mCurrentDirUri", getStringFromUri(mCurrentDirUri));
		outState.putString("mCurrentFileUri", getStringFromUri(mCurrentFileUri));
		outState.putString("mLastDirUri", getStringFromUri(mLastDirUri));

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		mCurrentDirUri = getUriFromString(savedInstanceState.getString("mCurrentDirUri"));
		mCurrentFileUri = getUriFromString(savedInstanceState.getString("mCurrentFileUri"));
		mLastDirUri = getUriFromString(savedInstanceState.getString("mLastDirUri"));

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * フォルダの一覧をとる
	 * @param uri フォルダを示すURI
	 */
	public void setFileList(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		// uriがフォルダのときの処理
		mCurrentDir = DocumentFile.fromTreeUri(this, uri);
		if (mCurrentDir != null) {
			if (mCurrentDir.isDirectory()) {
				mCurrentDirUri = uri;
				mCurrentFileUri = null;
			} else {
				mCurrentDir = null;
			}
		}

		// uriがフォルダでないときの処理
		if (mCurrentDir == null) {
			DocumentFile curfile = DocumentFile.fromSingleUri(this, uri);
			if (curfile == null) {
				Log.e(TAG, M()+"failed: uri is invalid: "+uri);
				return;
			}
			mCurrentDir = getParentFolder(me, curfile);
			mCurrentDirUri = mCurrentDir.getUri();
			mCurrentFileUri = uri;
		}

		// とりあえず一覧をとる
		mDirEntry.clear();
		mDirs.clear();
		mFiles.clear();
		List<DocumentFile> filesall = Arrays.asList(mCurrentDir.listFiles());

		// 親フォルダが自分と同じ（＝ルート）でなければ".."を最初にmDirEntryに追加する
		mHasParentFolder = false;
		DocumentFile parent = getParentFolder(me, mCurrentDir);
		if (!parent.getUri().equals(mCurrentDir.getUri())) {
			mDirEntry.add(parent);
			mHasParentFolder = true;
		}

		Comparator comparator = new Comparator<DocumentFile>() {
			@Override
			public int compare(DocumentFile a, DocumentFile b) {
				return a.getUri().compareTo(b.getUri());
			}
		};

		// フォルダをmDirEntryに追加する
		for (int i=0; i<filesall.size(); i++) {
			DocumentFile f = filesall.get(i);
			if (f.isDirectory()) {
				mDirs.add(f);
			}
		}
		Collections.sort(mDirs, comparator);
		mDirEntry.addAll(mDirs);

		// MDXファイルをmDirEntryに追加する
		mFiles.clear();
		for (int i=0; i<filesall.size(); i++) {
			DocumentFile f = filesall.get(i);
			if (!f.isDirectory()) {
				String name = f.getName().toLowerCase();
				if (name.endsWith(".mdx")) {
					mFiles.add(f);
				}
			}
		}
		Collections.sort(mFiles, comparator);
		mDirEntry.addAll(mFiles);

		// カーソルの位置を決定する
		int mCurPos = -1;
		if (mLastDirUri != null) {
			for (int i=0; i<mDirEntry.size(); i++) {
				DocumentFile f = mDirEntry.get(i);
				if (f.getUri().equals(mLastDirUri)) {
					mCurPos = i;
					break;
				}
			}
		}
		if (mCurrentFileUri != null) {
			for (int i=0; i<mDirEntry.size(); i++) {
				DocumentFile f = mDirEntry.get(i);
				if (f.getUri().equals(mCurrentFileUri)) {
					mCurPos = i;
					break;
				}
			}
		}
		if (mCurPos < 0) {
			mCurPos = 0;
		}

		// リストビューを作成する
		mAdapter = new SelectMdxFileAdapter(this, mDirEntry, mCurPos, mHasParentFolder);
		setListAdapter(mAdapter);
		getListView().setSelection(mCurPos);

		// リストビューの情報更新タスクを発行
		invokeThreadGetInfoTask();

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		super.onCreate(savedInstanceState);
		me = this;

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
	public void onResume() {
		if (T) Log.v(TAG, M()+"@in");

		super.onResume();

		mSelected = false;
		Uri uri;
		final Intent intent = getIntent();
		uri = intent.getData();

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			String uristrRoot = extras.getString("rootUri");
			if (uristrRoot != null) {
				mRootUri = getUriFromString(uristrRoot);
			}
		}

		// URIを決定してファイル一覧を得る
		if (mCurrentFileUri != null) {
			uri = mCurrentFileUri;
		}
		if (mCurrentDirUri != null) {
			uri = mCurrentDirUri;
		}
		if (uri == null) {
			uri = mRootUri;
		}
		setFileList(uri);

		// "listOnly"がONなら、そのファイルを選択したことにして終了する
		if (extras != null) {
			if (extras.getBoolean("listOnly")) {
				final Intent intent2 = new Intent();
				int selected = 0;
				final String findName = extras.getString("selectedFileName").toLowerCase();
				for (int i=0; i<mFiles.size(); i++) {
					DocumentFile file = mFiles.get(i);
					if (file.getName().equalsIgnoreCase(findName)) {
						selected = i;
						break;
					}
				}
				makeIntentReturnFiles(intent2, mFiles, selected);
				setResult(RESULT_OK, intent2);
				finish();
				return;
			}
		}

		mySetTitle(mCurrentDirUri);

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

	/*
	 * 終了
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (T) Log.v(TAG, M()+"@in");

		super.onDestroy();
		waitEndThreadGetInfoTask();

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onContentChanged() {
		if (T) Log.v(TAG, M()+"@in");

		super.onContentChanged();

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * リストから選択
	 */
	@Override
	public void onListItemClick(final ListView l, final View v, final int pos, final long id) {
		if (T) Log.v(TAG, M()+"@in: l="+l+", v="+v+", pos="+pos+", id="+id);

		final DocumentFile file = mDirEntry.get(pos);
		if (V) Log.v(TAG, "Selected [" + getStringFromUri(file.getUri()) + "]");
		waitEndThreadGetInfoTask();

		mLastDirUri = mCurrentDirUri;
		if (file.isDirectory()) {
			// フォルダを選択
			Uri uri = file.getUri();
			setFileList(uri);
			mySetTitle(uri);
		} else {
			// ファイルを選択
			mLastDirUri = null;
			mSelected = true;
			final Intent intent = new Intent();
			int selpos = pos - mDirs.size();
			if (mHasParentFolder) {
				selpos--;
			}
			makeIntentReturnFiles(intent, mFiles, selpos);
			setResult(RESULT_OK, intent);
			finish();
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 返却Intent用のファイル一覧を作成
	 * @param intent Intent
	 * @param files ファイル一覧
	 * @param selpos 選択項目
	 */
	public void makeIntentReturnFiles(final Intent intent, final ArrayList<DocumentFile> files, final int selpos) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent+", files="+files+", selpos="+selpos);

		intent.putExtra("uri", getStringFromUri(files.get(selpos).getUri()));
		intent.putExtra("folder", getStringFromUri(mCurrentDirUri));
		intent.putExtra("nselect", selpos);
		intent.putExtra("nuris", files.size());
		for (int i=0; i<files.size(); i++) {
			intent.putExtra("uri_"+i, getStringFromUri(files.get(i).getUri()));
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * Urlをタイトルに設定
	 * @param uri Uri
	 */
	public void mySetTitle(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		final String path = getDisplayPath(uri, mRootUri);
		setTitle(path + " " + getString(R.string.activityselectmdxfile_java_title));

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 非同期で曲名をリストに登録するタスク
	 */
	private final Runnable mListUpdater = new Runnable() {
		@Override
		public void run() {
			mAdapter.notifyDataSetChanged();
		}
	};
	private final Handler mHandle = new Handler();
	private boolean mGetInfoTaskInterrupt = false;
	private final Thread mGetInfoTask = new Thread() {
		@Override
		public void run() {
			if (T) Log.v(TAG, M()+"@in");

			for (int i=0; i<mDirEntry.size(); i++) {
				if (T) Log.v(TAG, M()+"i="+i);
				if (mGetInfoTaskInterrupt) {
					if (T) Log.v(TAG, M()+"interrupted");
					break;
				}
				final DocumentFile file = mDirEntry.get(i);
				if (file.isDirectory()) {
					continue;
				}
				Mxdrvg mxdrvg = new Mxdrvg(22050, 0, 1024*1024, 0);
				mxdrvg.setContext(me);
				if (mxdrvg.loadMdxFile(file.getUri(), true)) {
					mAdapter.setDescs(i, mxdrvg.getTitle());
					mHandle.post(mListUpdater);
				}
				mxdrvg.dispose();
				mxdrvg = null;
			}

			if (T) Log.v(TAG, M()+"@out");
		}
	};

	/**
	 * 非同期タスク終了待ち
	 */
	public void waitEndThreadGetInfoTask() {
		if (T) Log.v(TAG, M()+"@in");

		if (mThreadGetInfoTask != null) {
			try {
				mGetInfoTaskInterrupt = true;
				mThreadGetInfoTask.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 非同期タスク起動
	 */
	public void invokeThreadGetInfoTask() {
		if (T) Log.v(TAG, M()+"@in");

		mGetInfoTaskInterrupt = false;
		mThreadGetInfoTask = new Thread(mGetInfoTask);
		mThreadGetInfoTask.start();

		if (T) Log.v(TAG, M()+"@out");
	}

}

// [EOF]
