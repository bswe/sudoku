package com.bswe.sudoku;

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

class AndroidSystemAccess implements SystemAccess {
    AndroidApplication app;
    String appName;
    ClipboardManager clipBoard;

    public AndroidSystemAccess (AndroidApplication aa, String name, ClipboardManager clipboard) {
        app = aa;
        appName = name;
        clipBoard = clipboard;
        }

    public void WriteClipboard (String s) {
        Gdx.app.log (appName, "android SystemClipboardWriter: s=" + s);
        // Creates a new text clip to put on the clipboard
        ClipData clip = ClipData.newPlainText ("simple text", s);
        // Set the clipboard's primary clip.
        clipBoard.setPrimaryClip (clip);
        }

    public void RequestExternalAccess () {
        Gdx.app.log (appName, "android RequestExternalAccess: called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Gdx.app.log(appName, "android RequestExternalAccess: requesting permission");
                app.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
        }
    }


public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
        AndroidSystemAccess asa = new AndroidSystemAccess (this, sudoku.class.getName(),
                                                           (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize (new sudoku(asa), config);
	    }
    }
