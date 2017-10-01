package com.bswe.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bswe.pWallet;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.height = pWallet.SCREEN_HEIGHT;
		config.width = pWallet.SCREEN_WIDTH;
        StringSelection selection = new StringSelection("Hello desktop from pWallet");
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
		new LwjglApplication(new pWallet(), config);
	}
}
