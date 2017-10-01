package com.bswe;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.bswe.pWallet;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		// Get a handle to the android clipboard service.
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		// Creates a new text clip to put on the clipboard
		ClipData clip = ClipData.newPlainText ("simple text", "Hello android from pWallet!");
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip (clip);
        initialize(new pWallet(), config);
	}
}
