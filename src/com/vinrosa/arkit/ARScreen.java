package com.vinrosa.arkit;

import javax.microedition.location.QualifiedCoordinates;

import net.rim.device.api.system.Display;
import net.rim.device.api.system.EncodedImage;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.container.ComponentCanvas;
import net.rim.device.api.ui.container.MainScreen;

import com.vinrosa.arkit.ARController.ARControllerListener;

public final class ARScreen extends MainScreen implements ARControllerListener {
	private static final boolean DEBUG = true;
	private ComponentCanvas canvas;
	private LabelField debug;
	private ARController controller;

	public ARScreen() {
		setTitle("AR Screen");
		controller = new ARController();
		controller.setListener(this);
		controller.addCoordinates(new ARGeoCoordinate(new ARField("Cecomsa, Rómulo", EncodedImage.getEncodedImageResource("location.png")),
				18.456002, -69.942604));
		controller.addCoordinates(new ARGeoCoordinate(new ARField("Pollos Victorina, Rómulo", EncodedImage.getEncodedImageResource("location.png")), 18.4524,
				-69.9496));
		controller.addCoordinates(new ARGeoCoordinate(new ARField("Yogen Früz, Nuñez", EncodedImage.getEncodedImageResource("location.png")), 18.466692,
				-69.959175));

		canvas = new ComponentCanvas(Display.getWidth(), Display.getHeight() - 90);
		add(canvas);
		if (DEBUG) {
			add(debug = new LabelField("?"));
		}
	}

	protected void onUiEngineAttached(boolean attached) {
		super.onUiEngineAttached(attached);
		if (attached) {
			controller.setRealityViewSize(Display.getWidth(), Display.getHeight() - 90);
			controller.start();
		} else {
			controller.stop();
		}
	}

	public boolean onClose() {
		System.exit(0);
		return true;
	}

	public void willUpdateARFields() {
		canvas.deleteAll();
	}

	public void displayField(int x, int y, ARGeoCoordinate coordinate) {
		System.out.println("Log - displaying field");
		synchronized (ARApp.getEventLock()) {
			try {
				if (canvas != null && (x >= 0 && y >= 0))
					canvas.add(coordinate.getDisplayField(), x, y);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	public void deviceARInfo(double north, QualifiedCoordinates center) {
		if (DEBUG) {
			debug.setText("North:" + (int) north + " QC:" + center.getLatitude() + " " + center.getLongitude());
		}
	}
}
