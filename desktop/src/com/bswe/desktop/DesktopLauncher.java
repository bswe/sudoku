package com.bswe.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.bswe.pWallet;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.height = pWallet.SCREEN_HEIGHT;
		config.width = pWallet.SCREEN_WIDTH;
		new LwjglApplication(new pWallet(), config);
	}
}
