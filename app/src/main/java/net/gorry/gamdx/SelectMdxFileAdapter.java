package net.gorry.gamdx;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

/**
 * @author gorry
 *
 */
public class SelectMdxFileAdapter extends BaseAdapter {
	private static final boolean RELEASE = false;//true;
	private static final String TAG = "SelectMdxFileAdapter";
	private static final boolean T = true; //false;
	private static final boolean V = false;
	private static final boolean D = false;
	private static final boolean I = !RELEASE;

	private static String M() {
		StackTraceElement[] es = new Exception().getStackTrace();
		int count = 1; while (es[count].getMethodName().contains("$")) count++;
		return es[count].getFileName()+"("+es[count].getLineNumber()+"): "+es[count].getMethodName()+"(): ";
	}

	private final LayoutInflater mInflater;
	private ArrayList<DocumentFile> mFiles = new ArrayList<DocumentFile>();
	private ArrayList<String> mTypes = new ArrayList<String>();
	private ArrayList<String> mDescs = new ArrayList<String>();
	private final HashMap<String, String> typeTable = new HashMap<String, String>();

	private final Bitmap mFolderIcon;
	private final Bitmap mMdxIcon;
	private final int mHighlightPos;
	private boolean mHasParentFolder;


	/**
	 * @param context context
	 * @param files files
	 * @param pos pos
	 */
	public SelectMdxFileAdapter(final Context context, ArrayList<DocumentFile> files, final int pos, boolean hasParentFolder) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", files="+files+", pos="+pos+", hasParentFolder="+hasParentFolder);

		mHasParentFolder = hasParentFolder;
		mHighlightPos = ((pos < 0) ? 0 : pos);
		mInflater = LayoutInflater.from(context);

		mFiles = files;
		mTypes.clear();
		mDescs.clear();

		typeTable.put("mdx", "audio/x-mdx");

		for (int i=0; i<files.size(); i++) {
			final DocumentFile file = files.get(i);

			if (file.isDirectory()) {
				mTypes.add("text/directory");
				mDescs.add("");
				continue;
			}

			final String filename = file.getName();
			final int extpos = filename.lastIndexOf('.');
			if (extpos >= 0) {
				final String ext = filename.substring(extpos + 1);
				mTypes.add(typeTable.get(ext));
				mDescs.add("");
			} else {
				mTypes.add(null);
				mDescs.add("");
			}
		}

		mFolderIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_folder);
		mMdxIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_mdx);

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public int getCount() {
		return mFiles.size();
	}

	@Override
	public Object getItem(final int position) {
		return position;
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	static class ViewHolder {
		TextView name;
		TextView desc;
		ImageView icon;
	}

	/**
	 * @param pos position
	 * @return type
	 */
	public String getType(final int pos) {
		if ((pos < 0) || (pos >= mTypes.size())) {
			return null;
		}
		return mTypes.get(pos);
	}

	/**
	 * @param pos position
	 * @return desc
	 */
	public String getDescs(final int pos) {
		if ((pos < 0) || (pos >= mDescs.size())) {
			return "";
		}
		return mDescs.get(pos);
	}

	/**
	 * @param pos position
	 * @param desc desc
	 */
	public void setDescs(final int pos, final String desc) {
		if (T) Log.v(TAG, M()+"@in: pos="+pos+", desc="+desc);
		if ((pos >= 0) && (pos < mDescs.size())) {
			mDescs.set(pos, desc);
		}

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public View getView(final int pos, final View convertView, final ViewGroup parent) {
		if (T) Log.v(TAG, M()+"in: pos="+pos+", convertView="+convertView+", parent="+parent);

		ViewHolder holder;
		View v;
		final DocumentFile file = mFiles.get(pos);
		Bitmap b;

		if (convertView == null) {
			v = mInflater.inflate(R.layout.selectmdxfileadapter, null);

			holder = new ViewHolder();
			holder.name = (TextView)v.findViewById(R.id.firstLine);
			holder.desc = (TextView)v.findViewById(R.id.secondLine);
			holder.icon = (ImageView)v.findViewById(R.id.icon);

			v.setTag(holder);
		} else {
			v = convertView;
			holder = (ViewHolder) convertView.getTag();
		}

		String viewName = file.getName();
		if (file.isDirectory()) {
			b = mFolderIcon;
			if ((pos == 0) && (mHasParentFolder)) {
				viewName = "..";
			}
			viewName += "/";
			holder.name.setText(viewName);
			holder.name.setTextColor(Color.CYAN);
			holder.desc.setText("");
			holder.desc.setVisibility(View.GONE);
		} else {
			b = mMdxIcon;
			holder.name.setText(viewName);
			holder.name.setTextColor(Color.WHITE);
			holder.desc.setText(getDescs(pos));
			holder.desc.setVisibility(View.VISIBLE);
		}
		holder.icon.setImageBitmap(b);
		if (pos == mHighlightPos) {
			v.setBackgroundColor(Color.argb(128,128,128,128));
		} else {
			v.setBackgroundColor(Color.TRANSPARENT);
		}

		if (T) Log.v(TAG, M()+"@out: v="+v);
		return v;
	}

}

// [EOF]
