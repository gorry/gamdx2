package net.gorry.gamdx;

import java.io.File;
import java.util.HashMap;

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
	private final File[] mFiles;
	private final String[] mTypes;
	private final String[] mDescs;
	private final HashMap<String, String> typeTable = new HashMap<String, String>();

	private final Bitmap mFolderIcon;
	private final Bitmap mMdxIcon;
	private final int mHighlightPos;


	/**
	 * @param context context
	 * @param files files
	 * @param pos pos
	 */
	public SelectMdxFileAdapter(final Context context, final File[] files, final int pos) {
		if (T) Log.v(TAG, M()+"@in: context="+context+", files="+files+", pos="+pos);

		mHighlightPos = ((pos < 0) ? 0 : pos);
		mInflater = LayoutInflater.from(context);

		mFiles = files;
		mTypes = new String[files.length];
		mDescs = new String[files.length];

		typeTable.put("mdx", "audio/x-mdx");

		for (int i=0; i<files.length; i++) {
			final File file = files[i];

			if (file.isDirectory()) {
				mTypes[i] = "text/directory";
				continue;
			}

			final String filename = file.getName();
			final int extpos = filename.lastIndexOf('.');
			if (extpos >= 0) {
				final String ext = filename.substring(extpos + 1);
				mTypes[i] = typeTable.get(ext);
			} else {
				mTypes[i] = null;
			}
		}

		mFolderIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_folder);
		mMdxIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_mdx);

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public int getCount() {
		return mFiles.length;
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
	 * @param position position
	 * @return type
	 */
	public String getType(final int position) {
		if ((position < 0) || (position >= mTypes.length)) {
			return null;
		}
		return mTypes[position];
	}

	/**
	 * @param position position
	 * @return desc
	 */
	public String getDescs(final int position) {
		if ((position < 0) || (position >= mTypes.length)) {
			return null;
		}
		return mDescs[position];
	}

	/**
	 * @param position position
	 * @param desc desc
	 */
	public void setDescs(final int position, final String desc) {
		if (T) Log.v(TAG, M()+"@in: position="+position+", desc="+desc);

		mDescs[position] = desc;

		if (T) Log.v(TAG, M()+"@out");
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		if (T) Log.v(TAG, M()+"in: position="+position+", convertView="+convertView+", parent="+parent);

		ViewHolder holder;
		View v;
		final File file = mFiles[position];
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
		if (file.isDirectory() || (file.getName().equals(".."))) {
			b = mFolderIcon;
			viewName += "/";
			holder.name.setText(viewName);
			holder.name.setTextColor(Color.CYAN);
			holder.desc.setText("");
			holder.desc.setVisibility(View.GONE);
		} else {
			b = mMdxIcon;
			holder.name.setText(viewName);
			holder.name.setTextColor(Color.WHITE);
			holder.desc.setText(mDescs[position]);
			holder.desc.setVisibility(View.VISIBLE);
		}
		holder.icon.setImageBitmap(b);
		if (position == mHighlightPos) {
			v.setBackgroundColor(Color.argb(128,128,128,128));
		} else {
			v.setBackgroundColor(Color.TRANSPARENT);
		}

		if (T) Log.v(TAG, M()+"@out: v="+v);
		return v;
	}

}

// [EOF]
