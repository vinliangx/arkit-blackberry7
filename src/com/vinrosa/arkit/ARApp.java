package com.vinrosa.arkit;

import net.rim.device.api.system.RuntimeStore;
import net.rim.device.api.ui.UiApplication;

/**
 * This class extends the UiApplication class, providing a graphical user
 * interface.
 */
public class ARApp extends UiApplication {
	private static ARApp app;

	/**
	 * Entry point for application
	 * 
	 * @param args
	 *            Command line arguments (not used)
	 */
	public static void main(String[] args) {
		app = new ARApp();
		app.enterEventDispatcher();
	}

	/**
	 * Creates a new MyApp object
	 */
	public ARApp() {
		// Push a screen onto the UI stack for rendering.
		RuntimeStore.getRuntimeStore().remove(ARController.ID);
		pushScreen(new ARScreen());
	}

	public static ARApp getInstance() {
		return app;
	}
}
