package com.vinrosa.arkit;

public class ARCoordinate {
	protected double radialDistance;
	protected double inclination;
	protected double azimuth;
	protected boolean display = true;

	public void setRadialDistance(double radialDistance) {
		this.radialDistance = radialDistance;
	}

	public double getRadialDistance() {
		return radialDistance;
	}

	public void setInclination(double inclination) {
		this.inclination = inclination;
	}

	public double getInclination() {
		return inclination;
	}

	public void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
	}

	public double getAzimuth() {
		return azimuth;
	}

	public void setDisplay(boolean display) {
		this.display = display;
	}

	public boolean isDisplay() {
		return display;
	}
}
