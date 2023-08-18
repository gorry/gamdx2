package net.gorry.ndk;


@SuppressWarnings("unused")
public class Natives
{
	private static final String TAG = "Natives";

	private static EventListener listener;

	/** */
	public static interface EventListener
	{
		/**
		 * @param ch チャンネル数(16)
		 * @param workSize mdxChannelWork[]の1chあたりのサイズ
		 * @param mdxChannelWork チャンネルワーク（MXDRVG_WORK_CH）
		 * @param mdxGlobalWork グローバルワーク（MXDRVG_WORK_GLOBAL）
		 *
		 */
		void onOPMIntFunc(int ch, int workSize, byte[] mdxChannelWork, byte[] mdxGlobalWork);

		/**
		 * @param i 
		 *
		 */
		void onTerminatePlayFunc(int i);
	}

	/**
	 * @param l EventListener
	 */
	public static void setListener (final EventListener l) {
		listener = l;
	}

	/** */
	public static native int ndkEntry(String[] argv);

	/** */
	public static native int mxdrvgStart(int samprate, int fastmode, int mdxbufsize, int pdxbufsize, int useirq);
	/** */
	public static native int mxdrvgEnd();
	/** */
	public static native int mxdrvgGetPCM(short[] buf, int ofs, int len);
	/** */
	public static native int mxdrvgSetData(byte[] mdx, int mdxsize, byte[] pdx, int pdxsize);
	/** */
	public static native int mxdrvgMeasurePlayTime(int loop, int fadeout);
	/** */
	public static native void mxdrvgTotalVolume(int vol);
	/** */
	public static native int mxdrvgGetTotalVolume();
	/** */
	public static native void mxdrvgPlay();
	/** */
	public static native void mxdrvgPlayAt(int playat, int loop, int fadeout);
	/** */
	public static native int mxdrvgGetPlayAt();
	/** */
	public static native int mxdrvgGetTerminated();
	/** */
	public static native void mxdrvgChannelMask(int mask);
	/** */
	public static native int mxdrvgGetChannelMask();
	/** */
	public static native void mxdrvgPCM8Enable(int sw);
	/** */
	public static native int mxdrvgGetPCM8Enable();
	/** */
	public static native void mxdrvgFadeout();
	/** */
	public static native void mxdrvgFadeout2();

	public static void onOPMIntFunc(final int ch, final int workSize, final byte[] mdxChannelWork, final byte[]mdxGlobalWork) {
		if (listener != null) {
			listener.onOPMIntFunc(ch, workSize, mdxChannelWork, mdxGlobalWork);
		}
	}

	public static void onTerminatePlayFunc(final int i) {
		if (listener != null) {
			listener.onTerminatePlayFunc(i);
		}
	}


}
