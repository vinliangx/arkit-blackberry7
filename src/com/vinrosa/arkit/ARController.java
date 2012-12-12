package com.vinrosa.arkit;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.location.Location;
import javax.microedition.location.QualifiedCoordinates;

import net.rim.device.api.system.Backlight;
import net.rim.device.api.system.Display;
import net.rim.device.api.system.MagnetometerCalibrationException;
import net.rim.device.api.system.MagnetometerData;
import net.rim.device.api.system.MagnetometerListener;
import net.rim.device.api.system.MagnetometerSensor;
import net.rim.device.api.system.MagnetometerSensor.Channel;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.MathUtilities;

import com.vinrosa.arkit.location.handler.LocationThread;
import com.vinrosa.arkit.location.handler.LocationThread.LocationThreadListener;

public class ARController implements MagnetometerListener, LocationThreadListener {
	static final long ID = 0xfea9627a3f385a91L;

	private ARGeoCoordinate[] coordinates;
	private ARControllerListener listener;
	private ARSize realityView;

	private LocationThread locThread;

	private double degreeRange;
	private double viewAngle;
	private double currentAzimuth;

	private double northDegrees;

	private QualifiedCoordinates centerCoordinates;

	private Channel channel;

	private double maxRadialDistance;

	private Timer timer;

	public ARController() {
		super();
		realityView = new ARSize();
		centerCoordinates = new QualifiedCoordinates(0, 0, 0, 0, 0);
	}

	public void start() {
		if (locThread == null) {
			locThread = new LocationThread();
		}
		locThread.addListener(this);
		Location location = locThread.getLocation();
		if (location != null) {
			this.updateLocation(location);
		}
		channel = MagnetometerSensor.openChannel(UiApplication.getApplication());
		channel.addMagnetometerListener(this);

		timer = new Timer();
		timer.schedule(new BacklightTimerTask(), 5000, 5000);
	}

	public void stop() {
		if (channel != null) {
			channel.removeMagnetometerListener(this);
			channel.close();
		}
		if (timer != null) {
			timer.cancel();
		}
		locThread.removeListener(this);
	}

	public void setRealityViewSize(int width, int height) {
		realityView.width = width;
		realityView.height = height;
		degreeRange = width / 12;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.rim.device.api.system.MagnetometerListener#onData(net.rim.device.
	 * api.system.MagnetometerData)
	 */
	public void onData(MagnetometerData magData) {
		float[] xyz = new float[3];
		magData.getAccelerometerData(xyz);
		double viewAnglef;
		switch (Display.getOrientation()) {
		case Display.ORIENTATION_PORTRAIT: // UIDeviceOrientationPortrait:
			viewAnglef = MathUtilities.atan2(xyz[1], xyz[2]);
			break;
		case Display.ORIENTATION_LANDSCAPE: // UIDeviceOrientationPortrait:
			viewAnglef = MathUtilities.atan2(xyz[1], xyz[2]);
			break;
		default:
			viewAnglef = viewAngle;
			break;
		}
		try {
			switch (magData.getCalibrationQuality()) {
			case MagnetometerData.MAGNETOMETER_QUALITY_LOW:
				if (channel.isCalibrating())
					channel.stopCalibration();
				break;
			case MagnetometerData.MAGNETOMETER_QUALITY_UNRELIABLE:
				channel.startCalibration();
				break;
			case MagnetometerData.MAGNETOMETER_QUALITY_MEDIUM:
				if (channel.isCalibrating())
					channel.stopCalibration();
				break;
			case MagnetometerData.MAGNETOMETER_QUALITY_HIGH:
				if (channel.isCalibrating())
					channel.stopCalibration();
				break;
			default:
				break;
			}
		} catch (IllegalStateException e) {
			log(e.getMessage());
		} catch (MagnetometerCalibrationException e) {
			log(e.getMessage());
		}

		viewAngle = (viewAnglef * 0.05) + (viewAnglef * (1 - 0.05));
		northDegrees = magData.getDirectionBack();
		switch (MagnetometerData.getHeading((float) northDegrees)) {
		case MagnetometerData.MAGNETOMETER_HEADING_NORTH:
			currentAzimuth = degreesToRadian(0 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_EAST_NORTH_EAST:
			currentAzimuth = degreesToRadian(1 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_EAST:
			currentAzimuth = degreesToRadian(2 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_EAST:
			currentAzimuth = degreesToRadian(3 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_SOUTH:
			currentAzimuth = degreesToRadian(4 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_SOUTH_WEST:
			currentAzimuth = degreesToRadian(5 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_WEST:
			currentAzimuth = degreesToRadian(6 * 360 / 8);
			break;
		case MagnetometerData.MAGNETOMETER_HEADING_NORTH_WEST:
			currentAzimuth = degreesToRadian(7 * 360 / 8);
			break;
		default:
			currentAzimuth = degreesToRadian(northDegrees);
		}
		currentAzimuth = degreesToRadian(northDegrees);

		log("Magnetometer data! " + xyz[0] + " " + xyz[1] + " " + xyz[2]);
		this.updateCenterCoordinate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.vinrosa.arkit.location.handler.LocationThread.LocationThreadListener
	 * #updateLocation(javax.microedition.location.Location)
	 */
	public void updateLocation(Location location) {
		log("Location updated " + location);
		if (coordinates == null)
			return;
		centerCoordinates = location.getQualifiedCoordinates();
		System.out.println("CenterCoordinates.Altitude " + centerCoordinates.getAltitude());
		maxRadialDistance = 0;
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i].calibrateUsingOrigin(centerCoordinates);
			if (maxRadialDistance < Math.abs(coordinates[i].getRadialDistance())) {
				maxRadialDistance = Math.abs(coordinates[i].getRadialDistance());
			}
		}
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i].setMaximumScaleDistance(maxRadialDistance);
		}
		this.updateCenterCoordinate();
	}

	public void setListener(ARControllerListener listener) {
		this.listener = listener;
	}

	/**
	 * @author vinrosa
	 * 
	 */
	public interface ARControllerListener {
		/**
		 * 
		 */
		void willUpdateARFields();

		/**
		 * @param north
		 */
		void deviceARInfo(double north, QualifiedCoordinates center);

		/**
		 * @param x
		 * @param y
		 * @param northDegrees
		 *            TODO
		 * @param coordinate
		 */
		void displayField(int x, int y, ARGeoCoordinate coordinate);
	}

	public void addCoordinates(ARGeoCoordinate arGeoCoordinate) {
		if (coordinates == null) {
			coordinates = new ARGeoCoordinate[0];
		}
		Arrays.add(coordinates, arGeoCoordinate);
		// arGeoCoordinate.calibrateUsingOrigin(centerCoordinates);
	}

	private double findDeltaOfRadianCenter(DeltaRadianCenter delta) {
		if (delta.centerAzimuth < 0.0)
			delta.centerAzimuth = (Math.PI * 2.0) + delta.centerAzimuth;

		if (delta.centerAzimuth > (Math.PI * 2.0))
			delta.centerAzimuth = delta.centerAzimuth - (Math.PI * 2.0);

		double deltaAzimuth = Math.abs(delta.pointAzimuth - delta.centerAzimuth);
		delta.betweenNorth = false;
		if (delta.centerAzimuth < degreesToRadian(this.degreeRange) && delta.pointAzimuth > degreesToRadian(360 - this.degreeRange)) {
			deltaAzimuth = (delta.centerAzimuth + ((Math.PI * 2.0) - delta.pointAzimuth));
			delta.betweenNorth = true;
		} else if (delta.pointAzimuth < degreesToRadian(this.degreeRange) && delta.centerAzimuth > degreesToRadian(360 - this.degreeRange)) {
			deltaAzimuth = (delta.pointAzimuth + ((Math.PI * 2.0) - delta.centerAzimuth));
			delta.betweenNorth = true;
		}
		return deltaAzimuth;
	}

	private double radianToDegrees(double x) {
		return ((x) * 180.0 / Math.PI);
	}

	private double degreesToRadian(double x) {
		return (Math.PI * (x) / 180.0);
	}

	private ARPoint pointInView(ARSize realityView, ARSize viewToDraw, ARGeoCoordinate coordinate) {
		ARPoint point = new ARPoint();
		DeltaRadianCenter delta = new DeltaRadianCenter();
		delta.centerAzimuth = currentAzimuth;
		delta.pointAzimuth = coordinate.azimuth;
		delta.betweenNorth = false;
		double deltaAzimuth = this.findDeltaOfRadianCenter(delta);
		if ((delta.pointAzimuth > delta.centerAzimuth && !delta.betweenNorth)
				|| (delta.centerAzimuth > degreesToRadian(360 - this.degreeRange) && delta.pointAzimuth < degreesToRadian(this.degreeRange)))
			point.x = (int) ((realityView.width / 2) + ((deltaAzimuth / degreesToRadian(1)) * 12)) - (viewToDraw.width / 2);
		else
			point.x = (int) ((realityView.width / 2) - ((deltaAzimuth / degreesToRadian(1)) * 12)) - (viewToDraw.width / 2);
		point.y = (int) ((realityView.height / 2) - ((radianToDegrees((Math.PI / 2) - viewAngle)) / 180) * realityView.height)
				- (viewToDraw.height / 2);
		return point;
	}

	private boolean viewportContains(ARGeoCoordinate coordinate) {
		DeltaRadianCenter delta = new DeltaRadianCenter();
		delta.centerAzimuth = currentAzimuth;
		delta.pointAzimuth = coordinate.azimuth;
		delta.betweenNorth = false;
		log(delta);
		double deltaAzimuth = this.findDeltaOfRadianCenter(delta);
		log(delta);
		boolean result = false;
		if (deltaAzimuth <= degreesToRadian(this.degreeRange)) {
			result = true;
		}
		return result && coordinate.display;
	}

	private void updateCenterCoordinate() {
		// TODO Make adjustment on device based rotations.
		this.updateLocations();
	}

	class BacklightTimerTask extends TimerTask {
		public void run() {
			Backlight.enable(true, 255);
		}
	}

	private void updateLocations() {
		if (coordinates == null || (coordinates != null && coordinates.length == 0)) {
			return;
		}
		listener.willUpdateARFields();
		listener.deviceARInfo(northDegrees, centerCoordinates);
		for (int i = 0; i < coordinates.length; i++) {
			ARGeoCoordinate coordinate = coordinates[i];
			// coordinate.calibrateUsingOrigin(centerCoordinates);
			log("Coordinate: " + coordinate);
			ARSize viewToDraw = coordinate.getDisplayFieldSize();
			if (viewportContains(coordinate)) {
				ARPoint loc = pointInView(realityView, viewToDraw, coordinate);
				log("Displaying point: " + loc + " RadialDistance:" + coordinate.getRadialDistance() + " Azimuth:" + coordinate.azimuth);
				listener.displayField(loc.x, loc.y, coordinate);
			}
		}
	}

	private class DeltaRadianCenter {
		public double centerAzimuth;
		public double pointAzimuth;
		public boolean betweenNorth;

		public String toString() {
			return "CenterAzimuth: " + centerAzimuth + " PointAzimuth:" + pointAzimuth + " BetweenNorth: " + betweenNorth;
		}
	}

	private void log(Object o) {
		System.out.println("[BB ARKit]: " + o);
	}

}
