package com.bswe;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.bswe.pWallet;

class SystemClipboardWriter implements SystemClipboard {
    String appName;
    ClipboardManager clipBoard;

    public SystemClipboardWriter (String name, ClipboardManager clipboard){
        appName = name;
        clipBoard = clipboard;
    }

    public void write (String s) {
        Gdx.app.log (appName, "android SystemClipboardWriter: s=" + s);
        // Creates a new text clip to put on the clipboard
        ClipData clip = ClipData.newPlainText ("simple text", s);
        // Set the clipboard's primary clip.
        clipBoard.setPrimaryClip (clip);
    }
}
public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        SystemClipboard scw = new SystemClipboardWriter(pWallet.class.getName(),
                                                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		//	if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		//		requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        initialize(new pWallet(scw), config);
	}
}
