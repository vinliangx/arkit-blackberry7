/**
 * 
 */
package com.vinrosa.arkit.location.handler;

import java.util.Calendar;
import java.util.Date;

import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;

import net.rim.device.api.gps.GPSInfo;
import net.rim.device.api.system.EventLogger;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.util.Arrays;

/**
 * @author nbautista
 * 
 */
public class LocationThread extends Thread {

	/**
	 * If true, A LocationListener will be assigned to the LocationProvider.
	 * Otherwise a single LocationProvider.getLocation() will be performed.
	 * Default value is true.
	 */
	private boolean isMultipleFixes = true;
	/**
	 * Used in SmartMode. Essentially, this number represents the number of
	 * provider resets to perform until a fall back to MS-Assisted is triggered
	 * for a single assisted fix.
	 */
	private final int FALL_BACK_COUNTER_THRESHOLD = 2;
	/**
	 * If > FALL_BACK_COUNTER_THRESHOLD a single fallBack fix will be computed
	 * in MS-Assisted mode. Used in SmartMode.
	 */
	private int fallBackCounter = FALL_BACK_COUNTER_THRESHOLD;
	/**
	 * The available modes the user can choose. (m) stands for "multiple
	 * fix" and (s) stands for "single fix"
	 * 
	 * In Smart Mode the application operates in MS-Based mode but falls back to
	 * MS-Assisted for a single fix if the LocationProvider is unable to return
	 * a valid fix in maxInvalidTime seconds. The provider then goes back to
	 * MS-Based again.
	 */
	private int gpsChoices = 6;
	/**
	 * In Smart Mode the application operates mostly in MS-Based mode but falls
	 * back to MS-Assisted for a single fix if the LocationProvider is unable to
	 * return a valid fix after FALL_BACK_COUNTER_THRESHOLD many resets. So on
	 * the (FALL_BACK_COUNTER_THRESHOLD+1)'th reset a single assisted fix will
	 * be computed. A reset is performed if the provider fails to return a valid
	 * fix for more than "maxInvalidTimeField.getText()" seconds. The provider
	 * then goes back to MS-Based again.
	 */
	private boolean isSmartMode = false;
	/** Flag to map or not to map location data */
	private boolean enableMapLocationField = true;
	/** to show the logs */
	private String log = "Log: ";
	/** Zoom level of BlackBerry maps */
	private int zoomLevelField = 1;
	/** This Thread performs all the location related work for this test app */
	private static LocationThread locThread;
	/** Displays the total number of updates so far */
	private int numberUpdatesField = 0;
	/** Displays the total number of MS-Assisted updates so far */
	private int numberAssistedUpdatesField = 0;
	/** Displays the total number of MS-Based updates so far */
	private int numberUnassistedUpdatesField = 0;
	/** Displays the total number of valid updates so far */
	private int numberValidUpdatesField = 0;
	/** Displays the total number of invalid updates so far */
	private int numberInvalidUpdatesField = 0;
	/** Displays the time of the last valid fix */
	private String lastValidFixField = "-";
	/** Displays the time of the last LocationProvider reset */
	private String lastResetField = "-";
	/** Displays the location information for the current fix */
	/**
	 * Lets the user set the Horizontal Accuracy. If the selected mode uses a
	 * fixed Horizontal Accuracy value then, this user value is ignored.
	 */
	private int horizontalAccuracyField = 20;
	/** Lets the user set the Preferred Response Time. */
	private int preferredResponseTimeField = 16;
	/** Lets the user set the interval parameter of LocationListener */
	private int frequencyField = 5;
	/** Lets the use set the timeout parameter of LocationListener */
	private int timeoutField = -1;
	/** Lets the user set the maxAge parameter of LocationListener */
	private int maxAgeField = -1;
	/** Displays the location information for the current fix */
	private String currentLocationField = "-";
	/**
	 * Lets the user set how long consecutive invalid updates can occur until a
	 * LocationProvider reset is done
	 */
	private int maxInvalidTimeField = 300;
	/** Displays the value returned by Location.getLocationMethod() */
	private String currentModeField = "-";
	/** Current geoLocation based on cell id */
	private static Location geolocation;

	// Class fields

	/** Determines the mode of the LocationProvider */
	private Criteria criteria;
	/** parameters of LocationProvider.setLocationListener() */
	private int frequency, timeout, maxage;
	/** Counter variable of valid, invalid and total update */
	private int totalUpdates, validUpdates, inValidUpdates, assistedUpdates, unassistedUpdates;
	/** The location provider */
	private LocationProvider provider;
	/**
	 * variable to store the date/time (in long format) of the last valid fix
	 */
	private long lastValid = System.currentTimeMillis();
	/**
	 * variable to store the date/time (in long format) of the last
	 * LocationProvider reset
	 */
	private long lastReset = -1;

	/**
	 * Calls setupCriteria() to get the right Criteria setup for the mode
	 * selected by the user. Inits frequency, tiemout and maxage counters and
	 * sets PDE parameters according to the PDE IP and PORT provided by the
	 * user.
	 */
	public LocationThread() {
		setupCriteria();
		log("Criteria Initialized.");
		frequency = frequencyField;
		timeout = timeoutField;
		maxage = maxAgeField;
	}

	/**
	 * Calls resetDataFields() and setupProvider()
	 */
	public void run() {
		log("Starting Updates: " + dateFormatter(System.currentTimeMillis()));
		setupCriteria();
		// resetDataFields();
		setupProvider();
	}

	public Location getLocation() {
		run();
		return geolocation;
	}

	/**
	 * Initializes criteria according to the mode selected by the user.
	 * Following algorithm is used: If costAllowed = FALSE mode is standalone
	 * Otherwise, if costAllowed=TRUE, -if horizontalAccuracy = 0, mode is Data
	 * Optimal -if horizontalAccuracy > 0, -if multipled fixes requested, -if
	 * Telus, mode is MS-based -otherwise, -if powerUsage = HIGH, mode is Speed
	 * Optimal; -if powerUsage != HIGH, mode is MS-based -else if single fix
	 * requested, -if powerUsage = HIGH, mode is Accuracy Optimal; -if
	 * powerUsage != HIGH, mode is PDE Calculate -if powerUsage = LOW mode is
	 * Cellsite
	 */
	private void setupCriteria() {
		criteria = new Criteria();
		criteria.setPreferredResponseTime(preferredResponseTimeField);
		switch (gpsChoices) {
		case 0: // StandAlone
			isMultipleFixes = true;
			isSmartMode = false;
			criteria.setCostAllowed(false);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			log("Criteria set for Standalone");
			break;
		case 1: // Data Optimal
			isMultipleFixes = true;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(true);
			criteria.setHorizontalAccuracy(0);
			log("Criteria set for Data Optimal");
			break;
		case 2: // Speed Optimal
			isMultipleFixes = true;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_HIGH);
			log("Criteria set for Speed Optimal");
			break;
		case 3: // MS-Based
			isMultipleFixes = true;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			criteria.setVerticalAccuracy(50);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_MEDIUM);
			log("Criteria set for MS-Based");
			break;
		case 4: // Accuracy Optimal
			isMultipleFixes = false;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_HIGH);
			log("Criteria set for Accuracy Optimal");
			break;
		case 5: // PDE Calculate
			isMultipleFixes = false;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			criteria.setVerticalAccuracy(50);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_MEDIUM);
			log("Criteria set for PDE Calculate");
			break;
		case 6: // Cellsite
			isMultipleFixes = false;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			criteria.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
			criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_LOW);
			log("Criteria set for Cellsite(s)");
			break;
		case 7: // Cellsite (multiple fix)
			isMultipleFixes = true;
			isSmartMode = false;
			criteria.setCostAllowed(true);
			criteria.setHorizontalAccuracy(Criteria.NO_REQUIREMENT);
			criteria.setVerticalAccuracy(Criteria.NO_REQUIREMENT);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_LOW);
			log("Criteria set for Cellsite(m)");
			break;
		case 8: // default mode
			criteria = null;
			isSmartMode = false;
			log("Criteria set for Default Mode");
			break;
		case 9: // Smart Mode
			isMultipleFixes = true;
			isSmartMode = true;
			criteria.setCostAllowed(true);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			criteria.setVerticalAccuracy(50);
			criteria.setPreferredPowerConsumption(Criteria.POWER_USAGE_MEDIUM);
			log("Criteria set for Smart Mode");
			break;
		case 10:
			isMultipleFixes = false;
			isSmartMode = false;
			criteria.setCostAllowed(false);
			validateHorizontalAccuracy(false);
			criteria.setHorizontalAccuracy(horizontalAccuracyField);
			log("Criteria set for Standalone");
			break;
		default: // default
			criteria = null;
			break;
		}
	}

	/**
	 * This method validates the value in the horizontalAccuracy EditField. if
	 * mustBeZero is true then the value is force set to 0. Otherwise the user
	 * set value is validated, i.e. if user sets a value <=0 the value is force
	 * set to a default value: 100.
	 * 
	 * @param mustBeZero
	 *            indicates if the value must be 0
	 */
	private void validateHorizontalAccuracy(final boolean mustBeZero) {
		UiApplication.getUiApplication().invokeAndWait(new Runnable() {
			public void run() {
				int value = horizontalAccuracyField;
				if (!mustBeZero) {
					if (value <= 0)
						horizontalAccuracyField = 100;
				} else {
					horizontalAccuracyField = 0;
				}
			}
		});
	}

	/**
	 * This method intializes the LocationProvider and sets a LocationListener
	 * if isMultipleFixes is TRUE. Otherwise it simply calls
	 * singleFixLocationUpdate() which calls LocationProvider.getLocation() once
	 * to get a single fix.
	 */
	private void setupProvider() {
		try { // Just to give setPDE and criteria setup.. enough time. This
				// may not be needed at all.
			try {
				// Thread.sleep(20000);
			} catch (Throwable e) {
				log(e.toString());
			}
			provider = LocationProvider.getInstance(criteria);
			log("LocationProvider initialized");
			if (provider != null) {
				if ((isMultipleFixes && isSmartMode && fallBackCounter < FALL_BACK_COUNTER_THRESHOLD) || (isMultipleFixes && !isSmartMode)) { // Multifix
					// non-SmartMode
					// or
					// SmartMode
					// going
					// back to
					// MS-Based.
					if (isSmartMode) {
						log("SmartMode in MS-Based mode. fallBackCounter: " + fallBackCounter);
					}
					provider.setLocationListener(new LocListener(), frequency, timeout, maxage);
					log("LocationListener started");
				} else { // Singlefix non-SmartMode or SmartMode Falling
					// back to MS-Assisted
					if (isSmartMode)
						log("SmartMode in MS-Assisted mode. fallBackCounter: " + fallBackCounter);

					log("Initiating single shot gps fix");
					singleFixLocationUpdate();
				}
			} else {
				log("Provider unavailable for that Criteria");
			}
		} catch (LocationException le) {
			log(le.toString());
		}
	}

	/**
	 * Gets a single fix by calling LocationProvider.getLocation(). Updates the
	 * UI with the fix information. In case of a valid fix it maps the fix by
	 * invoking the map application.
	 */
	private void singleFixLocationUpdate() {
		Location location = null;
		geolocation = null;
		try {
			location = provider.getLocation(100);
		} catch (InterruptedException ie) {
			log("InterruptedException thrown by getLocation(): " + ie.getMessage());
		} catch (LocationException le) {
			log("LocationException thrown by getLocation(): " + le.getMessage());
		}
		if (location != null) {
			synchronized (UiApplication.getEventLock()) {
				numberUpdatesField = ++totalUpdates;
			}
			if (location.isValid()) {
				lastValid = System.currentTimeMillis();
				geolocation = location;
				synchronized (UiApplication.getEventLock()) {
					lastValidFixField = dateFormatter(lastValid);
					currentModeField = getLocMethodString(location.getLocationMethod());
					currentLocationField = location.getQualifiedCoordinates().getLatitude() + " " + location.getQualifiedCoordinates().getLongitude();
					numberValidUpdatesField = ++validUpdates;
					numberAssistedUpdatesField = ++assistedUpdates;
				}
				log("Valid single fix: " + location.getQualifiedCoordinates().getLatitude() + ", "
						+ location.getQualifiedCoordinates().getLongitude());

				// if (isSmartMode) { // if Smart Mode, go back to MS-Based
				// log("Smart Mode got single MS-Assisted fix");
				// fallBackCounter = 0;
				// shutdownProvider();
				// setupProvider();
				// }
				// if (enableMapLocationField)
				// mapLocation(location);
			} else {
				currentLocationField = "InValid";
				synchronized (UiApplication.getEventLock()) {
					numberInvalidUpdatesField = ++inValidUpdates;
				}
				log("GPSError: " + getErrorMessage(GPSInfo.getLastGPSError()));
				log("Invalid single fix");
				if (((System.currentTimeMillis() - lastValid >= maxInvalidTimeField * 1000) && (System.currentTimeMillis() - lastReset >= maxInvalidTimeField * 1000))
						|| (System.currentTimeMillis() - lastReset >= maxInvalidTimeField * 1000)) {
					log("Resetting Location Provider because: " + "\nInvalid fixes for " + (System.currentTimeMillis() - lastValid) / 1000
							+ " seconds" + "\nNo provider reset for " + (System.currentTimeMillis() - lastReset) / 1000 + " seconds");

					// if (isSmartMode) { // if Smart Mode, go back to
					// MS-Based
					// log("Smart Mode failed to get single MS-Assisted fix");
					// fallBackCounter = 0;
					// shutdownProvider();
					// setupProvider();
					// }

					shutdownProvider();
					setupProvider();
				}
			}
		} else {
			log("Location is null");
			if (isSmartMode) { // if Smart Mode, Assisted failed..so go back
				// to MS-Based
				log("Smart Mode FAILED! to get single MS-Assisted fix");
				fallBackCounter = 0;
				shutdownProvider();
				setupProvider();
			}
		}
	}

	/**
	 * nulls the LocationProviders LocationListener resets the LocationProvider
	 * nulls the LocationProvider
	 */
	private void shutdownProvider() {
		/**
		 * This is set to indicate that the provider has been reset
		 */
		lastReset = System.currentTimeMillis();
		synchronized (UiApplication.getEventLock()) {
			lastResetField = dateFormatter(lastReset);
		}
		log("Resettign LocationProvider");
		provider.setLocationListener(null, 0, 0, 0);
		provider.reset();
		provider = null;
	}

	/**
	 * store the message in the log
	 * 
	 * @param message
	 *            This is what gets logged
	 */
	public void log(String message) {
		if (message != null) {
			final String newMsg = dateFormatter(System.currentTimeMillis()) + message + "\n";
			EventLogger.logEvent(0x9876543212345L, newMsg.getBytes(), EventLogger.ALWAYS_LOG);
			UiApplication.getUiApplication().invokeLater(new Runnable() {
				public void run() {
					if (log.length() > 1000)
						log = "";

					log = (log + newMsg);
				}
			});
		}
	}

	private String getLocMethodString(int method) {
		StringBuffer buf = new StringBuffer();
		if ((method & Location.MTA_ASSISTED) > 0)
			buf.append("*MTA_ASSISTED");
		if ((method & Location.MTA_UNASSISTED) > 0)
			buf.append("*MTA_UNASSISTED");
		if ((method & Location.MTE_ANGLEOFARRIVAL) > 0)
			buf.append("*MTE_ANGLEOFARRIVAL");
		if ((method & Location.MTE_CELLID) > 0)
			buf.append("*MTE_CELLID");
		if ((method & Location.MTE_SATELLITE) > 0)
			buf.append("*MTE_SATELLITE");
		if ((method & Location.MTE_SHORTRANGE) > 0)
			buf.append("*MTE_SHORTRANGE");
		if ((method & Location.MTE_TIMEDIFFERENCE) > 0)
			buf.append("*MTE_TIMEDIFFERENCE");
		if ((method & Location.MTE_TIMEOFARRIVAL) > 0)
			buf.append("*MTE_TIMEOFARRIVAL");
		if ((method & Location.MTY_NETWORKBASED) > 0)
			buf.append("*MTY_NETWORKBASED");
		if ((method & Location.MTY_TERMINALBASED) > 0)
			buf.append("*MTY_TERMINALBASED");
		buf.append("*");
		return buf.toString();
	}

	/**
	 * Returns a readable error message for a given gps error code err.
	 * 
	 * @param err
	 *            Error code
	 * @return Human readable error message.
	 */
	public String getErrorMessage(int err) {
		String msg = "";
		switch (err) {
		case (GPSInfo.GPS_ERROR_ALMANAC_OUTDATED):
			msg = "Almanac outdated.";
			break;
		case (GPSInfo.GPS_ERROR_AUTHENTICATION_FAILURE):
			msg = "Authentication failed with the network.";
			break;
		case (GPSInfo.GPS_ERROR_CHIPSET_DEAD):
			msg = "GPS chipset dead; no fix.";
			break;
		case (GPSInfo.GPS_ERROR_DEGRADED_FIX_IN_ALLOTTED_TIME):
			msg = "Degraded fix; poor accuracy.";
			break;
		case (GPSInfo.GPS_ERROR_GPS_LOCKED):
			msg = "GPS service locked.";
			break;
		case (GPSInfo.GPS_ERROR_INVALID_NETWORK_CREDENTIAL):
			msg = "Invalid network credential.";
			break;
		case (GPSInfo.GPS_ERROR_INVALID_REQUEST):
			msg = "Request is invalid.";
			break;
		case (GPSInfo.GPS_ERROR_LOW_BATTERY):
			msg = "Low battery; fix cannot be obtained.";
			break;
		case (GPSInfo.GPS_ERROR_NETWORK_CONNECTION_FAILURE):
			msg = "Unable to connect to the data network.";
			break;
		case (GPSInfo.GPS_ERROR_NO_FIX_IN_ALLOTTED_TIME):
			msg = "No fix obtained in alloted time.";
			break;
		case (GPSInfo.GPS_ERROR_NO_SATELLITE_IN_VIEW):
			msg = "No Satellite is in view or the signal strength is too low to get a position fix.";
			break;
		case (GPSInfo.GPS_ERROR_NONE):
			msg = "No GPS Error.";
			break;
		case (GPSInfo.GPS_ERROR_PRIVACY_ACCESS_DENIED):
			msg = "Privacy setting denies getting a fix.";
			break;
		case (GPSInfo.GPS_ERROR_SERVICE_UNAVAILABLE):
			msg = "GPS service is not available due to no cellular service or no data service or no resources, etc.";
			break;
		case (GPSInfo.GPS_ERROR_TIMEOUT_DEGRADED_FIX_NO_ASSIST_DATA):
			msg = "Degraded fix (no assist data); poor accuracy.";
			break;
		case (GPSInfo.GPS_ERROR_TIMEOUT_NO_FIX_NO_ASSIST_DATA):
			msg = "No fix in alloted time, no assist.";
			break;
		default:
			msg = "Unknown error.";
			break;
		}

		return msg;
	}

	/**
	 * Given a date in long returns a nice String representation.
	 * 
	 * @param date
	 *            date in long format
	 * @return formatted in String format
	 */
	public String dateFormatter(long date) {
		Date t = new Date(date);
		Calendar c = Calendar.getInstance();
		c.setTime(t);
		return "[" + c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "]";
	}

	public LabelField getLocationInString() {
		return new LabelField("Location : " + currentLocationField);
	}

	/**
	 * LocationListener implementation
	 */
	private class LocListener implements LocationListener {
		/**
		 * Updates the UI with the fix information. In case of a valid fix it
		 * maps the fix by invoking the map application.
		 */
		public void locationUpdated(LocationProvider provider, Location location) {
			synchronized (UiApplication.getEventLock()) {
				numberUpdatesField = ++totalUpdates;
			}
			if (location.isValid()) {
				lastValid = System.currentTimeMillis();
				synchronized (UiApplication.getEventLock()) {
					lastValidFixField = dateFormatter(lastValid);
					currentModeField = getLocMethodString(location.getLocationMethod());
					currentLocationField = location.getQualifiedCoordinates().getLatitude() + " " + location.getQualifiedCoordinates().getLongitude();
					numberValidUpdatesField = ++validUpdates;
					numberUnassistedUpdatesField = ++unassistedUpdates;
				}
				log("Valid multiple fix: " + location.getQualifiedCoordinates().getLatitude() + ", "
						+ location.getQualifiedCoordinates().getLongitude());
				notifyListeners(location);
				// if (enableMapLocationField)
				// mapLocation(location);
			} else {
				currentLocationField = "InValid";
				synchronized (UiApplication.getEventLock()) {
					numberInvalidUpdatesField = ++inValidUpdates;
				}
				log("GPSError: " + getErrorMessage(GPSInfo.getLastGPSError()));
				log("Invalid multiple fix");
				if (((System.currentTimeMillis() - lastValid >= maxInvalidTimeField * 1000) && (System.currentTimeMillis() - lastReset >= maxInvalidTimeField * 1000))
						|| (System.currentTimeMillis() - lastReset >= maxInvalidTimeField * 1000)) {
					log("Resetting Location Provider because: " + "\nInvalid fixes for " + (System.currentTimeMillis() - lastValid) / 1000
							+ " seconds" + "\nNo provider reset for " + (System.currentTimeMillis() - lastReset) / 1000 + " seconds");
					if (isSmartMode) {
						fallBackCounter++;
					}
					shutdownProvider();
					setupProvider();
				}
			}
		}

		/**
		 * Resets the provider and restarts it if state is temporarily
		 * unavailable
		 */
		public void providerStateChanged(LocationProvider provider, int newState) {
			switch (newState) {
			case LocationProvider.AVAILABLE: // This is never triggered by
				// the system to preserve
				// battery
				log("State Change: Available");
				break;
			case LocationProvider.OUT_OF_SERVICE: // Triggered when a BES
				// policy does not allow
				// location capabilities
				log("State Change: Out of Service");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE: // Triggered when
				// the system
				// has stopped
				// looking for a
				// fix and went
				// cold
				log("State Change: Temp Unavailable");
				/**
				 * This is set to indicate that the provider has been reset
				 */
				lastValid = System.currentTimeMillis();
				lastValidFixField = "" + lastValid;
				log("Resetting Location Provider due to TEMPORARILY UNAVAILABLE state");
				if (isSmartMode) { // Fall back to MS-Assisted
					log("Smart Mode resetting...");
					fallBackCounter++;
					shutdownProvider();
					setupProvider();
				} else {
					shutdownProvider();
					setupProvider();
				}
				break;
			}
		}
	}
	
	private void notifyListeners(Location location){
		if (listeners == null){
			return;
		}
		for (int i  = 0 ; i < listeners.length ; i++){
			listeners[i].updateLocation(location);
		}
	}

	private LocationThreadListener[] listeners;

	public interface LocationThreadListener {
		void updateLocation(Location location);
	}

	public void addListener(LocationThreadListener listener) {
		if (listeners == null) {
			listeners = new LocationThreadListener[0];
		}
		Arrays.add(listeners, listener);
	}

	public void removeListener(LocationThreadListener listener) {
		if (listeners == null) {
			return;
		}
		if (Arrays.contains(listeners, listener)) {
			Arrays.remove(listeners, listener);
		}
	}
}