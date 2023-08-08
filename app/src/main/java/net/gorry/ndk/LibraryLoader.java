package net.gorry.ndk;

import android.util.Log;

public class LibraryLoader {

	static final String TAG = "LibraryLoader";

	static public void load(final String name) {
		final String LD_PATH = System.getProperty("java.library.path");

		Log.d(TAG, "Trying to load library " + name + " from LD_PATH: " + LD_PATH);

		try {
			System.loadLibrary(name);
		} catch (final UnsatisfiedLinkError e) {
			Log.e(TAG, e.toString());
		}
	}
}
