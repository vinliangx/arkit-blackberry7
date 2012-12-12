package com.vinrosa.arkit;

import javax.microedition.location.QualifiedCoordinates;

import net.rim.device.api.util.MathUtilities;

public class ARGeoCoordinate extends ARCoordinate {
	private ARField displayField;
	private QualifiedCoordinates coordinates;

	private static final double minimumScaleFactor = 0.6;

	public ARGeoCoordinate(ARField arField, double latitude, double longitude) {
		displayField = arField;
		coordinates = new QualifiedCoordinates(latitude, longitude, 0, 0, 0);
	}

	public ARField getDisplayField() {
		return displayField;
	}
	
	public QualifiedCoordinates getCoordinates() {
		return coordinates;
	}

	public void calibrateUsingOrigin(QualifiedCoordinates origin) {
		if (origin == null)
			return;
		origin.setAltitude(0);
		float baseDistance = origin.distance(coordinates);
		System.out.println("BaseDistance: " + baseDistance);
		System.out.println("- origin.getAltitude: " + origin.getAltitude());
		System.out.println("- coordinates.getAltitude: " + coordinates.getAltitude());
		this.radialDistance = Math.sqrt(MathUtilities.pow(origin.getAltitude() - coordinates.getAltitude(), 2)) + MathUtilities.pow(baseDistance, 2);
		System.out.println("RadiaDistance: " + this.radialDistance);
		double angle = Math.sin(Math.abs(origin.getAltitude() - coordinates.getAltitude())) / this.radialDistance;
		if (origin.getAltitude() > coordinates.getAltitude()) {
			angle = -angle;
		}
		this.inclination = angle;
		this.azimuth = getAngleBetweenCoordinates(origin, coordinates);

	}

	private double getAngleBetweenCoordinates(QualifiedCoordinates first, QualifiedCoordinates second) {
		double longitudinalDifference = second.getLongitude() - first.getLongitude();
		double latitudinalDifference = second.getLatitude() - first.getLatitude();
		double possibleAzimuth = (Math.PI * .5) - MathUtilities.atan(latitudinalDifference / longitudinalDifference);

		if (longitudinalDifference > 0) {
			return possibleAzimuth;
		} else if (longitudinalDifference < 0) {
			return possibleAzimuth + Math.PI;
		} else if (latitudinalDifference < 0) {
			return Math.PI;
		}
		return 0;
	}

	public ARSize getDisplayFieldSize() {
		return displayField.getBoxSize();
	}

	public String toString() {
		return "ARGeoCoordinate=[Latitude:" + coordinates.getLatitude() + ", Longitude:" + coordinates.getLongitude() + " PointAzimuth:" + azimuth
				+ "]";
	}

	public void setMaximumScaleDistance(double maximumScaleDistance) {
		System.out.println("maximumScaleDistance " + maximumScaleDistance);
		if (maximumScaleDistance <= 0)
			maximumScaleDistance = 1;
		double scaleFactor = 1.0 - minimumScaleFactor * (this.radialDistance / maximumScaleDistance);
		displayField.setScaleFactor(scaleFactor);
		System.out.println("scaleFactor " + scaleFactor);
	}
}
