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
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "ActivitySelectMdxFile";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

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
		if (T) Log.v(TAG, M()+"@in: outState="+outState);

		outState.putString("mCurrentFolderName", mCurrentFolderName);
		outState.putString("mCurrentFileName", mCurrentFileName);
		outState.putString("mLastFolderName", mLastFolderName);

		if (T) Log.v(TAG, M()+"@out");
	}

	/*
	 * アプリの一時退避復元
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		mCurrentFolderName = savedInstanceState.getString("mCurrentFolderName");
		mCurrentFileName = savedInstanceState.getString("mCurrentFileName");
		mLastFolderName = savedInstanceState.getString("mLastFolderName");

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 拡張子フィルタ
	 * @param ext 拡張子
	 * @return 拡張子にマッチしたらtrue
	 */
	public FilenameFilter extNameFilter(final String ext) {
		if (T) Log.v(TAG, M()+"@in: ext="+ext);

		mExtFilenameFilter = new String(ext.toLowerCase());
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(final File dir, final String name) {
				if (T) Log.v(TAG, M()+"@in: dir="+dir+", name="+name);

				final File file = new File(dir.getPath() + "/" + name);
				if (file.isDirectory()) {
					return false;
				}
				final String lname = name.toLowerCase();
				boolean ret = lname.endsWith(mExtFilenameFilter);

				if (T) Log.v(TAG, M()+"@out: ret="+ret);
				return ret;
			}
		};

		if (T) Log.v(TAG, M()+"@out: filter="+filter);
		return filter;
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
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

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

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		super.onCreate(savedInstanceState);

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
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		if (T) Log.v(TAG, M()+"@in: l="+l+", v="+v+", position="+position+", id="+id);

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

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * 返却Intent用のファイル一覧を作成
	 * @param intent Intent
	 * @param files ファイル一覧
	 * @param selected 選択項目
	 */
	public void makeIntentReturnFiles(final Intent intent, final File[] files, final int selected) {
		if (T) Log.v(TAG, M()+"@in: intent="+intent+", files="+files+", selected="+selected);

		intent.putExtra("path", files[selected].getPath());
		intent.putExtra("folder", mCurrentFolderName);
		intent.putExtra("nselect", selected);
		intent.putExtra("nfiles", files.length);
		for (int i=0; i<files.length; i++) {
			intent.putExtra("file"+i, files[i].getPath());
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	/**
	 * Urlをタイトルに設定
	 * @param uri Uri
	 */
	public void mySetTitle(final Uri uri) {
		if (T) Log.v(TAG, M()+"@in: uri="+uri);

		final String path = uri.getPath();
		final int idx = path.lastIndexOf('/');
		final String folder;
		if (idx >= 0) {
			folder = path.substring(0, idx+1);
		} else {
			folder = "/";
		}
		setTitle(folder + " " + getString(R.string.activityselectmdxfile_java_title));

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

			for (int i=0; i<mDirEntry.length; i++) {
				if (T) Log.v(TAG, M()+"i="+i);
				if (mGetInfoTaskInterrupt) {
					if (T) Log.v(TAG, M()+"interrupted");
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
