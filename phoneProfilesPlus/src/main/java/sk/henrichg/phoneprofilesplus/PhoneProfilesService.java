package sk.henrichg.phoneprofilesplus;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.sip.SipManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.Timer;
import java.util.TimerTask;


public class PhoneProfilesService extends Service
                                    implements SensorEventListener,
                                    AudioManager.OnAudioFocusChangeListener
{
    public static PhoneProfilesService instance = null;

    private final BatteryEventBroadcastReceiver batteryEventReceiver = new BatteryEventBroadcastReceiver();
    private final HeadsetConnectionBroadcastReceiver headsetPlugReceiver = new HeadsetConnectionBroadcastReceiver();
    //private final RestartEventsBroadcastReceiver restartEventsReceiver = new RestartEventsBroadcastReceiver();
    //private final WifiStateChangedBroadcastReceiver wifiStateChangedReceiver = new WifiStateChangedBroadcastReceiver();
    //private final WifiConnectionBroadcastReceiver wifiConnectionReceiver = new WifiConnectionBroadcastReceiver();
    private final ScreenOnOffBroadcastReceiver screenOnOffReceiver = new ScreenOnOffBroadcastReceiver();
    //private final BluetoothStateChangedBroadcastReceiver bluetoothStateChangedReceiver = new BluetoothStateChangedBroadcastReceiver();
    private DeviceIdleModeBroadcastReceiver deviceIdleModeReceiver = null;
    private PowerSaveModeBroadcastReceiver powerSaveModeReceiver = null;
    private InterruptionFilterChangedBroadcastReceiver interruptionFilterChangedReceiver = null;

    private static SettingsContentObserver settingsContentObserver = null;

    //-----------------------

    public static GeofencesScanner geofencesScanner = null;

    public static SensorManager mSensorManager = null;
    public static boolean mStartedSensors = false;

    private int mEventCountSinceGZChanged = 0;
    private static final int MAX_COUNT_GZ_CHANGE = 5;
    private float mGravity[] = new float[3];
    private float mGeomagnetic[] = new float[3];
    private float mProximity = -100;
    private float mMaxProximityDistance;
    private float mGravityZ = 0;  //gravity acceleration along the z axis

    public static PhoneStateScanner phoneStateScanner = null;

    public static final int DEVICE_ORIENTATION_UNKNOWN = 0;
    public static final int DEVICE_ORIENTATION_RIGHT_SIDE_UP = 3;
    public static final int DEVICE_ORIENTATION_LEFT_SIDE_UP = 4;
    public static final int DEVICE_ORIENTATION_UP_SIDE_UP = 5;
    public static final int DEVICE_ORIENTATION_DOWN_SIDE_UP = 6;
    public static final int DEVICE_ORIENTATION_HORIZONTAL = 9;

    public static final int DEVICE_ORIENTATION_DISPLAY_UP = 1;
    public static final int DEVICE_ORIENTATION_DISPLAY_DOWN = 2;

    public static final int DEVICE_ORIENTATION_DEVICE_IS_NEAR = 7;
    public static final int DEVICE_ORIENTATION_DEVICE_IS_FAR = 8;

    public static int mDisplayUp = DEVICE_ORIENTATION_UNKNOWN;
    public static int mSideUp = DEVICE_ORIENTATION_UNKNOWN;
    public static int mDeviceDistance = DEVICE_ORIENTATION_UNKNOWN;

    private static int tmpSideUp = DEVICE_ORIENTATION_UNKNOWN;
    private static int tmpDeviceDistance = DEVICE_ORIENTATION_UNKNOWN;
    private static long tmpSideTimestamp = 0;
    private static long tmpDistanceTimestamp = 0;

    //------------------------

    private AudioManager audioManager = null;
    private boolean ringingCallIsSimulating = false;
    public static int ringingVolume = 0;
    private int oldMediaVolume = 0;
    private MediaPlayer ringingMediaPlayer = null;
    private int mediaVolume = 0;
    private int usedStream = AudioManager.STREAM_MUSIC;

    private MediaPlayer eventNotificationMediaPlayer = null;
    private boolean eventNotificationIsPlayed = false;

    //--------------------------

    public static SipManager mSipManager = null;

    @Override
    public void onCreate()
    {
        super.onCreate();

        GlobalData.logE("$$$ PhoneProfilesService.onCreate", "android.os.Build.VERSION.SDK_INT="+android.os.Build.VERSION.SDK_INT);

        instance = this;

        // save version code (is used in PackageReplacedReceiver)
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int actualVersionCode = pinfo.versionCode;
            GlobalData.setSavedVersionCode(getApplicationContext(), actualVersionCode);
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
        }

        GlobalData.loadPreferences(getApplicationContext());

        //GlobalData.initPhoneProfilesServiceMessenger(getApplicationContext());

        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction(Intent.ACTION_BATTERY_CHANGED);
        getApplicationContext().registerReceiver(batteryEventReceiver, intentFilter1);

        IntentFilter intentFilter2 = new IntentFilter();
        for (String action: HeadsetConnectionBroadcastReceiver.HEADPHONE_ACTIONS) {
            intentFilter2.addAction(action);
        }
        getApplicationContext().registerReceiver(headsetPlugReceiver, intentFilter2);

        /*
        IntentFilter intentFilter7 = new IntentFilter();
        intentFilter7.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateChangedReceiver, intentFilter7);

        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiConnectionReceiver, intentFilter3);
        */

        IntentFilter intentFilter5 = new IntentFilter();
        intentFilter5.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter5.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter5.addAction(Intent.ACTION_USER_PRESENT);
        getApplicationContext().registerReceiver(screenOnOffReceiver, intentFilter5);

        /*
        IntentFilter intentFilter8 = new IntentFilter();
        intentFilter8.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateChangedReceiver, intentFilter8);
        */

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            deviceIdleModeReceiver = new DeviceIdleModeBroadcastReceiver();
            IntentFilter intentFilter9 = new IntentFilter();
            intentFilter9.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
            // is @hide :-(
            //if (android.os.Build.VERSION.SDK_INT >= 24)
            //    intentFilter9.addAction(PowerManager.ACTION_LIGHT_DEVICE_IDLE_MODE_CHANGED);
            getApplicationContext().registerReceiver(deviceIdleModeReceiver, intentFilter9);
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            powerSaveModeReceiver = new PowerSaveModeBroadcastReceiver();
            IntentFilter intentFilter10 = new IntentFilter();
            intentFilter10.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            getApplicationContext().registerReceiver(powerSaveModeReceiver, intentFilter10);
        }

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            boolean no60 = !Build.VERSION.RELEASE.equals("6.0");
            if (no60) {
                interruptionFilterChangedReceiver = new InterruptionFilterChangedBroadcastReceiver();
                IntentFilter intentFilter11 = new IntentFilter();
                intentFilter11.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
                getApplicationContext().registerReceiver(interruptionFilterChangedReceiver, intentFilter11);
            }
        }


        /*
        // receivers for system date and time change
        // events must by restarted
        IntentFilter intentFilter99 = new IntentFilter();
        intentFilter99.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter99.addAction(Intent.ACTION_TIME_CHANGED);
        getApplicationContext().registerReceiver(restartEventsReceiver, intentFilter99);
        */

        //SMSBroadcastReceiver.registerSMSContentObserver(this);
        //SMSBroadcastReceiver.registerMMSContentObserver(this);

        if (settingsContentObserver != null)
            getContentResolver().unregisterContentObserver(settingsContentObserver);
        settingsContentObserver = new SettingsContentObserver(this, new Handler(getMainLooper()));
        getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsContentObserver);

        /*
        if (mSipManager != null) {
            mSipManager = SipManager.newInstance(getApplicationContext());

            SipProfile sipProfile = null;
            try {
                SipProfile.Builder builder = new SipProfile.Builder("henrichg", "domain");
                builder.setPassword("password");
                sipProfile = builder.build();

                Intent intent = new Intent();
                intent.setAction("sk.henrichg.phoneprofilesplus.INCOMING_SIPCALL");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, Intent.FILL_IN_DATA);
                mSipManager.open(sipProfile, pendingIntent, null);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */


        // start service for first start
        Intent serviceIntent = new Intent(getApplicationContext(), FirstStartService.class);
        getApplicationContext().startService(serviceIntent);

        // this starts also listeners!!!
        // but will by stopped when events not exists
        startGeofenceScanner();
        startOrientationScanner();
        GlobalData.logE("PhoneProfilesService.startPhoneStateScanner", "+++");
        startPhoneStateScanner();

    }

    @Override
    public void onDestroy()
    {
        GlobalData.logE("PhoneProfilesService.onDestroy", "xxxxx");

        getApplicationContext().unregisterReceiver(batteryEventReceiver);
        getApplicationContext().unregisterReceiver(headsetPlugReceiver);
        //unregisterReceiver(wifiStateChangedReceiver);
        //unregisterReceiver(wifiConnectionReceiver);
        getApplicationContext().unregisterReceiver(screenOnOffReceiver);
        //unregisterReceiver(bluetoothStateChangedReceiver);
        //getApplicationContext().unregisterReceiver(restartEventsReceiver);
        if (android.os.Build.VERSION.SDK_INT >= 23)
            getApplicationContext().unregisterReceiver(deviceIdleModeReceiver);
        if (android.os.Build.VERSION.SDK_INT >= 21)
            getApplicationContext().unregisterReceiver(powerSaveModeReceiver);
        if (android.os.Build.VERSION.SDK_INT >= 23)
            getApplicationContext().unregisterReceiver(interruptionFilterChangedReceiver);

        //SMSBroadcastReceiver.unregisterSMSContentObserver(this);
        //SMSBroadcastReceiver.unregisterMMSContentObserver(this);

        if (settingsContentObserver != null)
            getContentResolver().unregisterContentObserver(settingsContentObserver);

        stopGeofenceScanner();
        stopOrientationScanner();
        stopPhoneStateScanner();

        instance = null;

        stopSimulatingRingingCall();

        //GlobalData.cleanPhoneProfilesServiceMessenger(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "xxxxx");

        if (intent != null) {
            boolean simulateRingingCall = intent.getBooleanExtra(GlobalData.EXTRA_SIMULATE_RINGING_CALL, false);
            if (simulateRingingCall)
                doSimulatingRingingCall(intent);
            boolean startStopScanner = intent.getBooleanExtra(GlobalData.EXTRA_START_STOP_SCANNER, false);
            if (startStopScanner) {
                switch (intent.getIntExtra(GlobalData.EXTRA_START_STOP_SCANNER_TYPE, 0)) {
                    case GlobalData.SCANNER_START_GEOFENCE_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_START_GEOFENCE_SCANNER");
                        startGeofenceScanner();
                        break;
                    case GlobalData.SCANNER_STOP_GEOFENCE_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_STOP_GEOFENCE_SCANNER");
                        stopGeofenceScanner();
                        break;
                    case GlobalData.SCANNER_START_ORIENTATION_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_START_ORIENTATION_SCANNER");
                        startOrientationScanner();
                        break;
                    case GlobalData.SCANNER_STOP_ORIENTATION_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_STOP_ORIENTATION_SCANNER");
                        stopOrientationScanner();
                        break;
                    case GlobalData.SCANNER_START_PHONE_STATE_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_START_PHONE_STATE_SCANNER");
                        startPhoneStateScanner();
                        break;
                    case GlobalData.SCANNER_STOP_PHONE_STATE_SCANNER:
                        GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "SCANNER_STOP_PHONE_STATE_SCANNER");
                        stopPhoneStateScanner();
                        break;
                }
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    //------------------------

    // Location ----------------------------------------------------------------

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();;
            }
            return  locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return  !TextUtils.isEmpty(locationProviders);
        }
    }

    private void startGeofenceScanner() {

        if (geofencesScanner != null) {
            geofencesScanner.disconnect();
            geofencesScanner = null;
        }

        if (GlobalData.getApplicationStarted(this)) {
            geofencesScanner = new GeofencesScanner(this);
            geofencesScanner.connect();
        }
    }

    private void stopGeofenceScanner() {
        if (geofencesScanner != null) {
            geofencesScanner.disconnect();
            geofencesScanner = null;
        }
    }

    public static boolean isGeofenceScannerStarted() {
        return (geofencesScanner != null);
    }

    //--------------------------------------------------------------------------

    // Device orientation ----------------------------------------------------------------

    private void startOrientationScanner() {
        if (mStartedSensors)
            stopListeningOrientationSensors();

        if (GlobalData.getApplicationStarted(this))
            startListeningOrientationSensors();
    }

    private void stopOrientationScanner() {
        stopListeningOrientationSensors();
    }

    public static boolean isOrientationScannerStarted() {
        return mStartedSensors;
    }

    public static Sensor getAccelerometerSensor(Context context) {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    public static Sensor getMagneticFieldSensor(Context context) {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }
    public static Sensor getProximitySensor(Context context) {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        return mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }
    @SuppressWarnings("deprecation")
    public static Sensor getOrientationSensor(Context context) {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        return mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    //--------------------------------------------------------------------------

    // Phone state ----------------------------------------------------------------

    private void startPhoneStateScanner() {
        if (phoneStateScanner != null) {
            phoneStateScanner.disconnect();
            //phoneStateScanner = null;
        }

        if (GlobalData.getApplicationStarted(this)) {
            if (phoneStateScanner == null)
                phoneStateScanner = new PhoneStateScanner(this);
            phoneStateScanner.connect();
        }
    }

    private void stopPhoneStateScanner() {
        if (phoneStateScanner != null) {
            phoneStateScanner.disconnect();
            //phoneStateScanner = null;
        }
    }

    public static boolean isPhoneStateStarted() {
        return (phoneStateScanner != null);
    }

    //--------------------------------------------------------------------------

    public void startListeningOrientationSensors() {
        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (!mStartedSensors) {

            if (GlobalData.isPowerSaveMode && GlobalData.applicationEventOrientationScanInPowerSaveMode.equals("2"))
                // start scanning in power save mode is not allowed
                return;

            DataWrapper dataWrapper = new DataWrapper(this, false, false, 0);
            if (dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_ORIENTATION) == 0)
                return;

            int interval = GlobalData.applicationEventOrientationScanInterval;
            if (GlobalData.isPowerSaveMode && GlobalData.applicationEventOrientationScanInPowerSaveMode.equals("1"))
                interval *= 2;
            Sensor accelerometer = getAccelerometerSensor(this);
            GlobalData.logE("PhoneProfilesService.startListeningOrientationSensors","accelerometer="+accelerometer);
            if (accelerometer != null)
                mSensorManager.registerListener(this, accelerometer, 1000000 * interval);
            Sensor magneticField = getMagneticFieldSensor(this);
            GlobalData.logE("PhoneProfilesService.startListeningOrientationSensors","magneticField="+magneticField);
            if (magneticField != null)
                mSensorManager.registerListener(this, magneticField, 1000000 * interval);
            Sensor proximity = getProximitySensor(this);
            GlobalData.logE("PhoneProfilesService.startListeningOrientationSensors","proximity="+proximity);
            if (proximity != null) {
                mMaxProximityDistance = proximity.getMaximumRange();
                mSensorManager.registerListener(this, proximity, 1000000 * interval);
            }
            //Sensor orientation = GlobalData.getOrientationSensor(this);
            //GlobalData.logE("PhoneProfilesService.startListeningOrientationSensors","orientation="+orientation);
            mStartedSensors = true;
        }
    }

    public void stopListeningOrientationSensors() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        mStartedSensors = false;
    }

    public void resetListeningOrientationSensors(boolean oldPowerSaveMode, boolean forceReset) {
        if ((forceReset) || (GlobalData.isPowerSaveMode != oldPowerSaveMode)) {
            stopListeningOrientationSensors();
            startListeningOrientationSensors();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        //if (event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
        //    return;

        if (sensorType == Sensor.TYPE_PROXIMITY) {
            //GlobalData.logE("PhoneProfilesService.onSensorChanged", "proximity value="+event.values[0]);
            //if ((event.values[0] == 0) || (event.values[0] == mMaxProximityDistance)) {
            //if (event.timestamp - tmpDistanceTimestamp >= 250000000L /*1000000000L*/) {
            //    tmpDistanceTimestamp = event.timestamp;
                mProximity = event.values[0];
                //if (mProximity == 0)
                if (mProximity < mMaxProximityDistance)
                    tmpDeviceDistance = DEVICE_ORIENTATION_DEVICE_IS_NEAR;
                else
                    tmpDeviceDistance = DEVICE_ORIENTATION_DEVICE_IS_FAR;

                if (tmpDeviceDistance != mDeviceDistance) {
                    mDeviceDistance = tmpDeviceDistance;
                    Intent broadcastIntent = new Intent(this, DeviceOrientationBroadcastReceiver.class);
                    sendBroadcast(broadcastIntent);
                    //setAlarm(this);
                }
            //}
            return;
        }
        if ((sensorType == Sensor.TYPE_ACCELEROMETER) || (sensorType == Sensor.TYPE_MAGNETIC_FIELD)) {
            if (getMagneticFieldSensor(this) != null) {
                if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                    mGravity = exponentialSmoothing(event.values, mGravity, 0.2f);
                }
                if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                    mGeomagnetic = exponentialSmoothing(event.values, mGeomagnetic, 0.5f);
                }
                if (event.timestamp - tmpSideTimestamp >= 250000000L /*1000000000L*/) {
                    tmpSideTimestamp = event.timestamp;
                    /*if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                        // Isolate the force of gravity with the low-pass filter.
                        mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                        mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                        mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
                    }
                    if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                        mGeomagnetic[0] = event.values[0];
                        mGeomagnetic[1] = event.values[1];
                        mGeomagnetic[2] = event.values[2];
                    }*/
                    if (mGravity != null && mGeomagnetic != null) {
                        float R[] = new float[9];
                        float I[] = new float[9];
                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                        if (success) {
                            float orientation[] = new float[3];
                            //orientation[0]: azimuth, rotation around the -Z axis, i.e. the opposite direction of Z axis.
                            //orientation[1]: pitch, rotation around the -X axis, i.e the opposite direction of X axis.
                            //orientation[2]: roll, rotation around the Y axis.

                            SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, I);
                            SensorManager.getOrientation(I, orientation);

                            //float azimuth = (float)Math.toDegrees(orientation[0]);
                            float pitch = (float) Math.toDegrees(orientation[1]);
                            float roll = (float) Math.toDegrees(orientation[2]);

                            //GlobalData.logE("PhoneProfilesService.onSensorChanged", "pitch=" + pitch);
                            //GlobalData.logE("PhoneProfilesService.onSensorChanged", "roll=" + roll);

                            int side = DEVICE_ORIENTATION_UNKNOWN;
                            if (pitch > -30 && pitch < 30) {
                                if (roll > -60 && roll < 60)
                                    side = DEVICE_ORIENTATION_DISPLAY_UP;
                                if (roll > 150 && roll < 180)
                                    side = DEVICE_ORIENTATION_DISPLAY_DOWN;
                                if (roll > -180 && roll < -150)
                                    side = DEVICE_ORIENTATION_DISPLAY_DOWN;
                                if (roll > 65 && roll < 115)
                                    side = DEVICE_ORIENTATION_UP_SIDE_UP;
                                if (roll > -115 && roll < -65)
                                    side = DEVICE_ORIENTATION_DOWN_SIDE_UP;
                            }
                            if (pitch > 30 && pitch < 90) {
                                side = DEVICE_ORIENTATION_LEFT_SIDE_UP;
                            }
                            if (pitch > -90 && pitch < -30) {
                                side = DEVICE_ORIENTATION_RIGHT_SIDE_UP;
                            }

                            if ((tmpSideUp == DEVICE_ORIENTATION_UNKNOWN) || (/*(side != DEVICE_ORIENTATION_UNKNOWN) &&*/ (side != tmpSideUp))) {
                                mEventCountSinceGZChanged = 0;

                                //GlobalData.logE("PhoneProfilesService.onSensorChanged", "azimuth="+azimuth);
                                //GlobalData.logE("PhoneProfilesService.onSensorChanged", "pitch=" + pitch);
                                //GlobalData.logE("PhoneProfilesService.onSensorChanged", "roll=" + roll);

                                tmpSideUp = side;
                            } else {
                                ++mEventCountSinceGZChanged;
                                if (mEventCountSinceGZChanged == MAX_COUNT_GZ_CHANGE) {

                                    if (tmpSideUp != mSideUp) {

                                        mSideUp = tmpSideUp;

                                        if ((mSideUp == DEVICE_ORIENTATION_DISPLAY_UP) || (mSideUp == DEVICE_ORIENTATION_DISPLAY_DOWN))
                                            mDisplayUp = mSideUp;

                                        /*
                                        if (mDisplayUp == DEVICE_ORIENTATION_DISPLAY_UP)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now screen is facing up.");
                                        if (mDisplayUp == DEVICE_ORIENTATION_DISPLAY_DOWN)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now screen is facing down.");

                                        if (mSideUp == DEVICE_ORIENTATION_UP_SIDE_UP)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now up side is facing up.");
                                        if (mSideUp == DEVICE_ORIENTATION_DOWN_SIDE_UP)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now down side is facing up.");
                                        if (mSideUp == DEVICE_ORIENTATION_RIGHT_SIDE_UP)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now right side is facing up.");
                                        if (mSideUp == DEVICE_ORIENTATION_LEFT_SIDE_UP)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "now left side is facing up.");
                                        if (mSideUp == DEVICE_ORIENTATION_UNKNOWN)
                                            GlobalData.logE("PhoneProfilesService.onSensorChanged", "unknown side.");
                                        */

                                        Intent broadcastIntent = new Intent(this, DeviceOrientationBroadcastReceiver.class);
                                        sendBroadcast(broadcastIntent);
                                        //setAlarm(this);

                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                if (event.timestamp - tmpSideTimestamp >= 250000000L /*1000000000L*/) {
                    tmpSideTimestamp = event.timestamp;

                    float gravityZ = event.values[2];
                    if (mGravityZ == 0) {
                        mGravityZ = gravityZ;
                    } else {
                        if ((mGravityZ * gravityZ) < 0) {
                            mEventCountSinceGZChanged++;
                            if (mEventCountSinceGZChanged == MAX_COUNT_GZ_CHANGE) {
                                mGravityZ = gravityZ;
                                mEventCountSinceGZChanged = 0;

                                if (gravityZ > 0) {
                                    GlobalData.logE("PhoneProfilesService.onSensorChanged", "now screen is facing up.");
                                    mSideUp = DEVICE_ORIENTATION_DISPLAY_UP;
                                } else if (gravityZ < 0) {
                                    GlobalData.logE("PhoneProfilesService.onSensorChanged", "now screen is facing down.");
                                    mSideUp = DEVICE_ORIENTATION_DISPLAY_DOWN;
                                }

                                if ((mSideUp == DEVICE_ORIENTATION_DISPLAY_UP) || (mSideUp == DEVICE_ORIENTATION_DISPLAY_DOWN))
                                    mDisplayUp = mSideUp;

                                Intent broadcastIntent = new Intent(this, DeviceOrientationBroadcastReceiver.class);
                                sendBroadcast(broadcastIntent);
                                //setAlarm(this);
                            }
                        } else {
                            if (mEventCountSinceGZChanged > 0) {
                                mGravityZ = gravityZ;
                                mEventCountSinceGZChanged = 0;
                            }
                        }
                    }
                }
            }
        }
    }

    private float[] exponentialSmoothing(float[] input, float[] output, float alpha) {
        if (output == null)
            return input;
        for (int i=0; i<input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //---------------------------

    private void doSimulatingRingingCall(Intent intent) {
        if (intent.getBooleanExtra(GlobalData.EXTRA_SIMULATE_RINGING_CALL, false))
        {
            GlobalData.logE("$$$ PhoneProfilesService.onStartCommand", "simulate ringing call");

            Context context = getApplicationContext();

            ringingCallIsSimulating = false;

            int oldRingerMode = intent.getIntExtra(GlobalData.EXTRA_OLD_RINGER_MODE, 0);
            int oldZenMode = intent.getIntExtra(GlobalData.EXTRA_OLD_ZEN_MODE, 0);
            String oldRingtone = intent.getStringExtra(GlobalData.EXTRA_OLD_RINGTONE);
            int newRingerMode = GlobalData.getRingerMode(context);
            int newZenMode = GlobalData.getZenMode(context);
            String newRingtone;
            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
                if (uri != null)
                    newRingtone = uri.getPath();
                else
                    newRingtone = "";
            } catch (SecurityException e) {
                Permissions.grantPlayRingtoneNotificationPermissions(context, true);
                newRingtone = "";
            } catch (Exception e) {
                newRingtone = "";
            }

            GlobalData.logE("PhoneProfilesService.onStartCommand", "oldRingerMode=" + oldRingerMode);
            GlobalData.logE("PhoneProfilesService.onStartCommand", "oldZenMode=" + oldZenMode);
            GlobalData.logE("PhoneProfilesService.onStartCommand", "newRingerMode=" + newRingerMode);
            GlobalData.logE("PhoneProfilesService.onStartCommand", "newZenMode=" + newZenMode);
            GlobalData.logE("PhoneProfilesService.onStartCommand", "oldRingtone=" + oldRingtone);
            GlobalData.logE("PhoneProfilesService.onStartCommand", "newRingtone=" + newRingtone);

            if (ActivateProfileHelper.isAudibleRinging(newRingerMode, newZenMode)) {

                GlobalData.logE("PhoneProfilesService.onStartCommand", "ringing is audible");

                boolean simulateRinging = false;
                int stream = AudioManager.STREAM_RING;

                if ((android.os.Build.VERSION.SDK_INT >= 21)) {
                    if (!(((newRingerMode == 4) && (android.os.Build.VERSION.SDK_INT >= 23)) ||
                            ((newRingerMode == 5) && ((newZenMode == 3) || (newZenMode == 6))))) {
                        // actual ringer/zen mode is changed to another then NONE and ONLY_ALARMS
                        // Android 6 - ringerMode=4 = ONLY_ALARMS

                        // test old ringer and zen mode
                        if (((oldRingerMode == 4) && (android.os.Build.VERSION.SDK_INT >= 23)) ||
                                ((oldRingerMode == 5) && ((oldZenMode == 3) || (oldZenMode == 6)))) {
                            // old ringer/zen mode is NONE and ONLY_ALARMS
                            simulateRinging = true;
                            stream = AudioManager.STREAM_MUSIC;
                            GlobalData.logE("PhoneProfilesService.onStartCommand", "stream=MUSIC");
                        }
                    }

                    if (!simulateRinging) {
                        if (!(((newRingerMode == 4) && (android.os.Build.VERSION.SDK_INT < 23)) ||
                                ((newRingerMode == 5) && (newZenMode == 2)))) {
                            // actual ringer/zen mode is changed to another then PRIORITY
                            // Android 5 - ringerMode=4 = PRIORITY
                            if (((oldRingerMode == 4) && (android.os.Build.VERSION.SDK_INT < 23)) ||
                                    ((oldRingerMode == 5) && (oldZenMode == 2))) {
                                // old ringer/zen mode is PRIORITY
                                simulateRinging = true;
                                stream = AudioManager.STREAM_RING;
                                GlobalData.logE("PhoneProfilesService.onStartCommand", "stream=RING");
                            }
                        }
                    }
                }

                if (oldRingtone.isEmpty() || (!newRingtone.isEmpty() && !newRingtone.equals(oldRingtone)))
                    simulateRinging = true;

                GlobalData.logE("PhoneProfilesService.onStartCommand", "simulateRinging=" + simulateRinging);

                if (simulateRinging)
                    startSimulatingRingingCall(stream);
            }

        }
    }

    private void startSimulatingRingingCall(int stream) {
        if (!ringingCallIsSimulating) {
            GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "stream="+stream);
            if (audioManager == null )
                audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            if ((eventNotificationMediaPlayer != null) && eventNotificationIsPlayed)
                if (eventNotificationMediaPlayer.isPlaying())
                    eventNotificationMediaPlayer.stop();

            Ringtone ringtone = RingtoneManager.getRingtone(this, Settings.System.DEFAULT_RINGTONE_URI);
            if (ringtone != null) {
                RingerModeChangeReceiver.removeAlarm(this);
                RingerModeChangeReceiver.internalChange = true;

                usedStream = stream;
                // play repeating: default ringtone with ringing volume level
                try {
                    int requestType = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
                    if (android.os.Build.VERSION.SDK_INT >= 19)
                        requestType = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
                    int result = audioManager.requestAudioFocus(this, usedStream, requestType);
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        ringingMediaPlayer = new MediaPlayer();
                        ringingMediaPlayer.setAudioStreamType(usedStream);
                        ringingMediaPlayer.setDataSource(this, Settings.System.DEFAULT_RINGTONE_URI);
                        ringingMediaPlayer.prepare();
                        ringingMediaPlayer.setLooping(true);

                        oldMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                        GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "ringingVolume=" + ringingVolume);

                        int maximumRingValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                        int maximumMediaValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                        float percentage = (float) ringingVolume / maximumRingValue * 100.0f;
                        mediaVolume = Math.round(maximumMediaValue / 100.0f * percentage);

                        GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "mediaVolume=" + mediaVolume);

                        /*if (android.os.Build.VERSION.SDK_INT >= 23)
                            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
                        else
                            audioManager.setStreamMute(AudioManager.STREAM_RING, true);*/
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);

                        ringingMediaPlayer.start();

                        ringingCallIsSimulating = true;
                        GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "ringing played");
                    } else
                        GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "focus not granted");
                } catch (SecurityException e) {
                    GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", " security exception");
                    Permissions.grantPlayRingtoneNotificationPermissions(this, true);
                    ringingMediaPlayer = null;
                    RingerModeChangeReceiver.setAlarmForDisableInternalChange(this);
                } catch (Exception e) {
                    GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "exception");
                    ringingMediaPlayer = null;
                    RingerModeChangeReceiver.setAlarmForDisableInternalChange(this);
                }
            }
        }
    }

    public void stopSimulatingRingingCall() {
        //if (ringingCallIsSimulating) {
            GlobalData.logE("PhoneProfilesService.stopSimulatingRingingCall", "xxx");
            if (audioManager == null )
                audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            if (ringingMediaPlayer != null) {
                if (ringingMediaPlayer.isPlaying())
                    ringingMediaPlayer.stop();
                ringingMediaPlayer.release();

                /*if (android.os.Build.VERSION.SDK_INT >= 23)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
                else
                    audioManager.setStreamMute(AudioManager.STREAM_RING, false);*/

                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldMediaVolume, 0);
                GlobalData.logE("PhoneProfilesService.startSimulatingRingingCall", "ringing stopped");
            }
            audioManager.abandonAudioFocus(this);
        //}
        ringingCallIsSimulating = false;
        RingerModeChangeReceiver.setAlarmForDisableInternalChange(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (audioManager == null )
            audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback
            if (ringingMediaPlayer != null)
                if (ringingMediaPlayer.isPlaying())
                    ringingMediaPlayer.pause();
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume
            if (usedStream == AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            if (usedStream == AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);
            if (ringingMediaPlayer != null)
                if (!ringingMediaPlayer.isPlaying())
                    ringingMediaPlayer.start();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (ringingMediaPlayer != null) {
                if (ringingMediaPlayer.isPlaying())
                    ringingMediaPlayer.stop();
                ringingMediaPlayer.release();
            }
            audioManager.abandonAudioFocus(this);
            // Stop playback
        }
    }

    //---------------------------

    public void playEventNotificationSound (final String eventNotificationSound) {
        if (!ringingCallIsSimulating) {

            if (audioManager == null )
                audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

            if ((eventNotificationMediaPlayer != null) && eventNotificationIsPlayed) {
                if (eventNotificationMediaPlayer.isPlaying())
                    eventNotificationMediaPlayer.stop();
            }

            if (!eventNotificationSound.isEmpty())
            {
                Uri notificationUri = Uri.parse(eventNotificationSound);

                try {
                    RingerModeChangeReceiver.removeAlarm(this);
                    RingerModeChangeReceiver.internalChange = true;

                    eventNotificationMediaPlayer = new MediaPlayer();
                    eventNotificationMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    eventNotificationMediaPlayer.setDataSource(this, notificationUri);
                    eventNotificationMediaPlayer.prepare();
                    eventNotificationMediaPlayer.setLooping(false);

                    oldMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                    int notificationVolume = GlobalData.getNotificationVolume(this);

                    GlobalData.logE("PhoneProfilesService.playEventNotificationSound", "notificationVolume=" + notificationVolume);

                    int maximumNotificationValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                    int maximumMediaValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                    float percentage = (float) notificationVolume / maximumNotificationValue * 100.0f;
                    mediaVolume = Math.round(maximumMediaValue / 100.0f * percentage);

                    GlobalData.logE("PhoneProfilesService.playEventNotificationSound", "mediaVolume=" + mediaVolume);

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mediaVolume, 0);

                    eventNotificationMediaPlayer.start();

                    eventNotificationIsPlayed = true;

                    final Context context = this;
                    //Thread.sleep(eventNotificationMediaPlayer.getDuration());
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {

                            if (eventNotificationMediaPlayer.isPlaying())
                                eventNotificationMediaPlayer.stop();

                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldMediaVolume, 0);
                            GlobalData.logE("PhoneProfilesService.playEventNotificationSound", "event notification stopped");

                            eventNotificationIsPlayed = false;

                            RingerModeChangeReceiver.setAlarmForDisableInternalChange(context);

                        }
                    }, eventNotificationMediaPlayer.getDuration());

                } catch (SecurityException e) {
                    GlobalData.logE("PhoneProfilesService.playEventNotificationSound", "security exception");
                    Permissions.grantPlayRingtoneNotificationPermissions(this, true);
                    eventNotificationIsPlayed = false;
                    RingerModeChangeReceiver.setAlarmForDisableInternalChange(this);
                } catch (Exception e) {
                    GlobalData.logE("PhoneProfilesService.playEventNotificationSound", "exception");
                    e.printStackTrace();
                    eventNotificationIsPlayed = false;
                    RingerModeChangeReceiver.setAlarmForDisableInternalChange(this);
                }

            }
        }
    }

    //---------------------------

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        GlobalData.logE("$$$ PhoneProfilesService.onTaskRemoved", "xxxxx");

        ActivateProfileHelper.screenTimeoutUnlock(getApplicationContext());
        super.onTaskRemoved(rootIntent);
    }

}
