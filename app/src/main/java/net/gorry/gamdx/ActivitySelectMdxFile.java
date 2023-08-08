/**
 *
 */
package net.gorry.gamdx;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * 
 * ファイルリストの取得
 * 
 * @author gorry
 *
 */
public class ActivitySelectMdxFile extends ListActivity {
	private static final String TAG = "ActivitySelectMdxFile";
	private static final boolean V = false; // true;

	private String mCurrentFolderName = "";
	private File mCurDir;
	private File[] mDirEntry;
	private File[] mDirs;
	private File[] mFiles;
	private SelectMdxFileAdapter mAdapter;
	private String mExtFilenameFilter;
	private String mCurrentFileName;
	private String mLastFolderName = "";
	private Thread mThreadGetInfoTask = null;

	@SuppressWarnings("unused")
	private boolean mSelected = false;
	@SuppressWarnings("unused")
	private int mCurPos = 0;

	/* アプリの一時退避
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		if (V) Log.v(TAG, "onSaveInstanceState()");
		outState.putString("mCurrentFolderName", mCurrentFolderName);
		outState.putString("mCurrentFileName", mCurrentFileName);
		outState.putString("mLastFolderName", mLastFolderName);
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onRestoreInstanceState()");
		mCurrentFolderName = savedInstanceState.getString("mCurrentFolderName");
		mCurrentFileName = savedInstanceState.getString("mCurrentFileName");
		mLastFolderName = savedInstanceState.getString("mLastFolderName");
	}

	/**
	 * 拡張子フィルタ
	 * @param ext 拡張子
	 * @return 拡張子にマッチしたらtrue
	 */
	public FilenameFilter extNameFilter(final String ext) {
		mExtFilenameFilter = new String(ext.toLowerCase());
		return new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				final File file = new File(dir.getPath() + "/" + name);
				if (file.isDirectory()) {
					return false;
				}
				final String lname = name.toLowerCase();
				return lname.endsWith(mExtFilenameFilter);
			}
		};
	}

	/**
	 * ディレクトリフィルタ
	 * @return ディレクトリならtrue
	 */
	public static FileFilter dirEntryFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return !(file.isFile());
			}
		};
	}

	/**
	 * ソート用比較
	 */
	public class compareFileName implements Comparator<File> {
		@Override
		public int compare(final File f1, final File f2) {
			return (f1.getName().compareToIgnoreCase(f2.getName()));
		}

		/**
		 * @param f1 エントリ1
		 * @param f2 エントリ2
		 * @return 比較結果
		 */
		public boolean equals(final File f1, final File f2) {
			return (f1.getName().equalsIgnoreCase(f2.getName()));
		}
	}

	/**
	 * @param uri uri
	 */
	public void setFileList(final Uri uri) {
		// フォルダ一覧＋指定拡張子ファイル一覧
		final String path = uri.getPath();
		final int idx = path.lastIndexOf('/');
		final String folder;
		final String filename;
		if (idx >= 0) {
			folder = path.substring(0, idx+1);
			filename = path.substring(idx+1);
		} else {
			folder = "/";
			filename = "";
		}
		mCurrentFolderName = folder;
		mCurDir = new File(folder);
		mCurrentFileName = filename;
		final Comparator<File> c1 = new compareFileName();
		mDirs = mCurDir.listFiles(dirEntryFilter());
		if (mDirs == null) {
			mDirs = new File[0];
		}
		Arrays.sort(mDirs, c1);
		mFiles = mCurDir.listFiles(extNameFilter(".mdx"));
		if (mFiles == null) {
			mFiles = new File[0];
		}
		Arrays.sort(mFiles, c1);
		final File upDir = new File(folder + "..");
		mDirEntry = new File[1 + mDirs.length + mFiles.length];
		mDirEntry[0] = upDir;
		System.arraycopy(mDirs, 0, mDirEntry, 1, mDirs.length);
		System.arraycopy(mFiles, 0, mDirEntry, 1+mDirs.length, mFiles.length);

		int curpos = -1;
		if ((mLastFolderName != null) && (mLastFolderName.length() > 0+1)) {
			// ".."を選んだあと、今までいたフォルダを初期位置にする
			final String s1 = mLastFolderName.substring(0, mLastFolderName.length()-1);
			final int i1 = s1.lastIndexOf('/');
			if (i1 >= 0) {
				final String s2 = s1.substring(i1+1);
				if (s2.length() > 0) {
					for (int i=0; i<mDirEntry.length; i++) {
						if (mDirEntry[i].getName().equals(s2)) {
							curpos = i;
							break;
						}
					}
				}
			}
		}
		if (curpos < 0) {
			for (int i=0; i<mDirEntry.length; i++) {
				if (mDirEntry[i].getName().equals(mCurrentFileName)) {
					curpos = i;
					break;
				}
			}
		}
		mCurPos = curpos;
		mAdapter = new SelectMdxFileAdapter(this, mDirEntry, curpos);
		setListAdapter(mAdapter);

		if (curpos >= 0) {
			getListView().setSelection(curpos);
		}

		invokeThreadGetInfoTask();
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (V) Log.v(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
	}

	/*
	 * 再起動
	 * @see android.app.Activity#onRestart()
	 */
	@Override
	public void onRestart() {
		if (V) Log.v(TAG, "onRestart()");
		super.onRestart();

	}

	/*
	 * 開始
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		if (V) Log.v(TAG, "onStart()");
		super.onStart();
	}

	/*
	 * 再開
	 * @see android.app.Activity#onResume()
	 */
	@Override
	public void onResume() {
		if (V) Log.v(TAG, "onResume()");
		super.onResume();

		mSelected = false;
		Uri uri;
		final Intent intent = getIntent();
		uri = intent.getData();
		if ((mCurrentFolderName != null) && (mCurrentFolderName.length() > 0)) {
			if ((mCurrentFileName != null) && (mCurrentFileName.length() > 0)) {
				uri = Uri.parse("file://" + mCurrentFolderName + mCurrentFileName);
			} else {
				uri = Uri.parse("file://" + mCurrentFolderName);
			}
		}
		if (uri == null) {
			uri = Uri.parse("file://" + Setting.mdxRootPath);
		}
		setFileList(uri);

		final Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.getBoolean("listOnly")) {
				final Intent intent2 = new Intent();
				int selected = 0;
				final String findName = extras.getString("selectedFileName").toLowerCase();
				for (int i=0; i<mFiles.length; i++) {
					if (mFiles[i].getName().equalsIgnoreCase(findName)) {
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

		mySetTitle(uri);
	}

	/*
	 * 停止
	 * @see android.app.Activity#onPause()
	 */
	@Override
	public synchronized void onPause() {
		if (V) Log.v(TAG, "onPause()");
		super.onPause();
	}

	/*
	 * 中止
	 * @see android.app.Activity#onStop()
	 */
	@Override
	public void onStop() {
		if (V) Log.v(TAG, "onStop()");
		super.onStop();
	}

	/*
	 * 終了
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (V) Log.v(TAG, "onDestroy()");
		super.onDestroy();
		waitEndThreadGetInfoTask();
	}

	@Override
	public void onContentChanged() {
		if (V) Log.v(TAG, "onContentChanged");
		super.onContentChanged();
	}

	/*
	 * リストから選択
	 */
	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		if (V) Log.v(TAG, "onListItemClick");
		final File file = mDirEntry[position];
		if (V) Log.v(TAG, "Selected [" + file.getPath() + "]");
		waitEndThreadGetInfoTask();

		if (file.isDirectory() || (file.getName().equals(".."))) {
			Uri uri = Uri.parse("file://" + file.getPath() + "/");
			final String name = file.getName();
			mLastFolderName = "";
			if (name.equals("..")) {
				uri = Uri.parse("file://" + file.getParentFile().getParent() + "/");
				mLastFolderName = mCurrentFolderName;
			}
			setFileList(uri);
			mySetTitle(uri);
		} else {
			mLastFolderName = "";
			mSelected = true;
			final Intent intent = new Intent();
			makeIntentReturnFiles(intent, mFiles, position-mDirs.length-1);
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	/**
	 * 返却Intent用のファイル一覧を作成
	 * @param intent Intent
	 * @param files ファイル一覧
	 * @param selected 選択項目
	 */
	public void makeIntentReturnFiles(final Intent intent, final File[] files, final int selected) {
		intent.putExtra("path", files[selected].getPath());
		intent.putExtra("folder", mCurrentFolderName);
		intent.putExtra("nselect", selected);
		intent.putExtra("nfiles", files.length);
		for (int i=0; i<files.length; i++) {
			intent.putExtra("file"+i, files[i].getPath());
		}

	}

	/**
	 * Urlをタイトルに設定
	 * @param uri Uri
	 */
	public void mySetTitle(final Uri uri) {
		final String path = uri.getPath();
		final int idx = path.lastIndexOf('/');
		final String folder;
		if (idx >= 0) {
			folder = path.substring(0, idx+1);
		} else {
			folder = "/";
		}
		setTitle(folder + " " + getString(R.string.activityselectmdxfile_java_title));
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
			if (V) Log.v(TAG, "mGetInfoTask");
			for (int i=0; i<mDirEntry.length; i++) {
				if (V) Log.v(TAG, "mGetInfoTask: loop "+i);
				if (mGetInfoTaskInterrupt) {
					if (V) Log.v(TAG, "mGetInfoTask: interrupted");
					break;
				}
				final File file = mDirEntry[i];
				if (file.isDirectory()) {
					continue;
				}
				Mxdrvg mxdrvg = new Mxdrvg(22050, 0, 1024*64, 0);
				if (mxdrvg.loadMdxFile(file.getPath(), true)) {
					mAdapter.setDescs(i, mxdrvg.getTitle());
					mHandle.post(mListUpdater);
				}
				mxdrvg.dispose();
				mxdrvg = null;
			}
			if (V) Log.v(TAG, "mGetInfoTask: end");
		}
	};

	/**
	 * 非同期タスク終了待ち
	 */
	public void waitEndThreadGetInfoTask() {
		if (mThreadGetInfoTask != null) {
			try {
				mGetInfoTaskInterrupt = true;
				mThreadGetInfoTask.join();
			} catch (final InterruptedException e) {
				//
			}
		}
	}

	/**
	 * 非同期タスク起動
	 */
	public void invokeThreadGetInfoTask() {
		mGetInfoTaskInterrupt = false;
		mThreadGetInfoTask = new Thread(mGetInfoTask);
		mThreadGetInfoTask.start();
	}

}
