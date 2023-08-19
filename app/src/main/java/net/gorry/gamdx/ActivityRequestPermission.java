package net.gorry.gamdx;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest.permission;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * 
 * メインアクティビティ
 * 
 * @author gorry
 *
 */
public class ActivityRequestPermission extends AppCompatActivity {
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
	private static Activity me;


	/*
	 * 作成
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		if (T) Log.v(TAG, M()+"@in: savedInstanceState="+savedInstanceState);

		super.onCreate(savedInstanceState);
		me = this;

		setContentView(R.layout.activityrequestpermission);

		requestPostNotification();

		if (T) Log.v(TAG, M()+"@out");
	}

	private void requestPostNotification() {
		if (T) Log.v(TAG, M()+"@in");

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
			int perm = ContextCompat.checkSelfPermission(me, permission.POST_NOTIFICATIONS);
			if (perm == PackageManager.PERMISSION_GRANTED) {
				bootActivityMain();
 			} else {
				// boolean flag = shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS);
				boolean flag = false; // 必ず許可を要請する
				if (!flag) {
					ActivityResultLauncher<String> requestPermissionLauncher =
						registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
							if (T) Log.v(TAG, M()+"@in: isGranded="+isGranted);

							if (!isGranted) {
								new AlertDialog.Builder(this)
										.setTitle(R.string.title_request_permission_notification)
										.setMessage(R.string.msg_request_permission_notification)
										.setCancelable(false)
										.setPositiveButton("OK", new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												if (T) Log.v(TAG, M()+"@in: dialog="+dialog+", which="+which);

												String uriString = "package:" + getPackageName();
												Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(uriString));
												startActivity(intent);
												finish();

												if (T) Log.v(TAG, M()+"@out");
											}
										})
										.show();
							} else {
								bootActivityMain();
							}

							if (T) Log.v(TAG, M()+"@out");
						});
					requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
				}
			}
		}

		if (T) Log.v(TAG, M()+"@in");
	}

	private void bootActivityMain() {
		final Intent intent = new Intent(
				me,
				ActivityMain.class
		);
		me.startActivity(intent);
		finish();
	}
}

// [EOF]
