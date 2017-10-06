package com.bswe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bswe.pWallet;
import com.bswe.SystemClipboard;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

class SystemClipboardWriter implements SystemClipboard {
    String appName;

    public SystemClipboardWriter (String name){
        appName = name;
        }

    public void write (String s) {
        Gdx.app.log (appName, "windows SystemClipboardWriter: s=" + s);
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents (selection, selection);
        }
    }

public class DesktopLauncher {
    private static final String TAG = pWallet.class.getName();

    public static void main (String[] arg) {
        SystemClipboardWriter scw = new SystemClipboardWriter (TAG);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.height = pWallet.SCREEN_HEIGHT;
		config.width = pWallet.SCREEN_WIDTH;
		new LwjglApplication (new pWallet(scw), config);
	    }
    }
