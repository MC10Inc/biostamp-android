# BioStamp3™ Android SDK

Build Android applications that communicate with BioStamp3™ sensors via
[Bluetooth Low Energy (BLE)][7].

## Table of Contents

  * [Requirements](#requirements)
  * [Getting started](#getting-started)
  * [Connecting to sensors](#connecting-to-sensors)
  * [Controlling sensors](#controlling-sensors)
  * [Sensing](#sensing)

## Requirements

This SDK requires Android 7.0 Nougat (API level 24) or higher. It will not
work on older devices.

Your application must enable [Java 8 language features][1].

Your application must run on a device that supports Bluetooth 4.0 with
Bluetooth Low Energy (BLE). Bluetooth 4.0 or 4.1 is sufficient for command
and control of the sensor but data throughput is slow. Bluetooth 4.2
provides acceptable data throughput, and Bluetooth 5.0 or newer provides a
further improvement.

## Getting started

### GitHub Packages

This SDK is supplied as an Android library hosted by [GitHub Packages][2].
To access the library, first [create a personal access token][3] with the
`read:packages` scope for your GitHub account. To make the access token
available to any Android Studio project, create a file in your home
directory named `.gradle/gradle.properties`:

```
gpr.user=your_github_username
gpr.key=your_access_token
```

### Project Setup

To use this SDK, start by opening your existing Android application or creating
a new application in Android Studio. Open your application's `build.gradle`
script and make the following changes:

Confirm that the minimum SDK version is at least 24:

```gradle
android {
    ...
    defaultConfig {
        ...
        minSdkVersion 24
    }
}
```

Enable Java 8 language features:

```gradle
android {
    ...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
```

Add the GitHub Packages repositories. Your GitHub username and access token can
optionally be hardcoded here instead of referencing the values in
`gradle.properties`:

```gradle
repositories {
    maven() {
        url = uri("https://maven.pkg.github.com/mc10inc/bitgatt")
        credentials {
            username = project.findProperty("gpr.user")
            password = project.findProperty("gpr.key")
        }
    }
    maven() {
        url = uri("https://maven.pkg.github.com/mc10inc/biostamp-android")
        credentials {
            username = project.findProperty("gpr.user")
            password = project.findProperty("gpr.key")
        }
    }
}
```

Add the SDK library to the application's dependencies:

```gradle
dependencies {
    ...
    implementation 'com.mc10inc.biostamp3:sdk:0.0.1-a06e1d1'
}
```

### Initializing the SDK

The `BioStampManager` class must be initialized before any other SDK functions
are called. This must be done once every time the application is launched. Ideally,
you should do this from within the `onCreate` method of the [Application][4] class.

If your existing application already subclasses [Application][4], then add
the following line to the `onCreate` method:

```java
@Override
public void onCreate() {
    super.onCreate();
    ...    
    BioStampManager.initialize(this);
    ...
}
```

If you are creating a new application or your existing application does not subclass
`Application`, then create a class like this:

```java
import android.app.Application;

import com.mc10inc.biostamp3.sdk.BioStampManager;

public class MyCustomApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BioStampManager.initialize(this);
    }
}
```

and add it to your application's `AndroidManifest.xml` file:

```xml
<application
    android:name=".MyCustomApplication"
    ...    
```

All SDK functions are accessed through the `BioStampManager` class. It is a
singleton, and once it is initialized it can be accessed from anywhere in the
application by calling `BioStampManager.getInstance()`. It is a fatal error to
call `getInstance()` before calling `initialize()`.

### Documentation

In addition to this document the SDK include Javadoc for classes and methods
that are part of the SDK's public API. The Javadoc is automatically downloaded
along with the library and is accessible from within Android Studio.

To view Javadoc from within Android Studio, click on an SDK class or method
(for example `BioStampManager`) in a source file and press Ctrl-Q. A popup
containing the documentation will open.

It is also possible to browse the Javadocs in a web browser, through a local
web server built in to Android Studio. To browse the docs, click on any
BioStamp SDK class or method (for example `BioStampManager`) in a source file,
and select View -> External Documentation from the Android Studio menu. Note
that this menu item does not appear unless code with external Javadoc is
selected.

## Connecting to sensors

Bluetooth Low Energy defines roles for BLE devices. The BioStamp sensor is in
the Peripheral role and the Android device is in the Central role. As a
Peripheral, any time the sensor is powered on it is either advertising
(broadcasting its presence and accepting connection requests) or it is in a
connection with a Central. As a Central, the Android device scans for
advertising sensors and initiates connections to sensors.

As soon as a connection is established with the sensor it stops advertising
until that connection is disconnected.

### Permissions

To use Bluetooth Low Energy, the application must have the [Android permission][5]
`ACCESS_COARSE_LOCATION` granted by the end user. The application's UI is
responsible for initiating that request. The SDK provides optional utility
methods for requesting the permission.

The `BioStampManager.hasPermissions()` method checks if the permissions are
granted, and `BioStampManager.requestPermissions(activity)` requests the
permissions. For example, from within an Activity:

```java
BioStampManager bs = BioStampManager.getInstance();
if (!bs.hasPermissions()) {
    bs.requestPermissions(this);
}
```

If `hasPermissions()` returns `false` then scanning for sensors or connecting
to a sensor will fail.

### Scanning for sensors

The SDK can scan to find all sensors that are powered on and in range of the
Android device. In a typical application this would be used to let the user
select which sensor(s) they want to use.

If you have only one sensor and you know its serial number, it is not necessary
to implement scanning for a minimal test app. You can instead directly connect
to the sensor using a hardcoded serial number.

The scanning function is accessed through a [LiveData object][6]. Scanning is
enabled any time that object is being observed. The value of the LiveData is a
`Map` whose keys are sensor serial numbers and whose values are
`ScannedSensorStatus` objects containing info about the sensor.

Do not observe the scanning LiveData until the necessary permissions have been
granted and `BioStampManager.hasPermissions()` returns true.

Sensors are added to the set of sensors in range any time an advertisement from
the sensor is received. Sensors are removed after they have not been seen for
10 seconds. Sensors do not advertise while connected, so a sensor will
disappear from the set of sensors in range soon after a connection is opened to
it.

This minimal example prints out the serial numbers of all sensors in range:

```java
BioStampManager bs = BioStampManager.getInstance();
bs.getSensorsInRangeLiveData().observe(this, sensors -> {
    Log.i("app", String.format("%d sensors in range:", sensors.size()));
    for (String serialNumber : sensors.keySet()) {
        Log.i("app", serialNumber);
    }
});
```

### Connecting to a sensor

All interaction between the application and a specific BioStamp is through a
`BioStamp` object dedicated to that sensor. The same object is used for that
sensor for as long as the application is running; it is not necessary to obtain
a new object when reconnecting after disconnecting.

The object is obtained from the SDK by providing a sensor serial number, which
would normally be obtained by scanning but can also be hardcoded.

```java
BioStamp sensor = BioStampManager.getInstance().getBioStamp(serialNumber);
```

The connection state of the `BioStamp` is defined by `BioStamp.State` and is
either `DISCONNECTED`, `CONNECTING`, or `CONNECTED`. The sensor is initially
`DISCONNECTED`. When a connection attempt is initiated it enters the
`CONNECTING` state, and then enters either `CONNECTED` if the connection is
successful or `DISCONNECTED` if the connection fails. The connection state must
be `DISCONNECTED` in order to initiate a connection; it is a fatal error to
call `connect()` if the state is `CONNECTING` or `CONNECTED`.

```java
if (sensor.getState() != BioStamp.State.DISCONNECTED) {
    Log.i("app", "Cannot initiate a connection now");
}
```

To connect to the sensor, call the `connect()` method supplying a
`ConnectionListener` which handles the result of the connection attempt. The
connection attempt always completes within a finite time, either succeeding or
failing.  If the connection attempt succeeds, then `connected()` is called and
at some point in the future when the connection is eventually disconnected,
`disconnected()` will be called. But if the connection attempt fails, then
`connectFailed()` is called and `disconnected()` will never be called.

```java
sensor.connect(new BioStamp.ConnectListener() {
    @Override
    public void connected() {
        Log.i("app", "Connected successfully");
    }

    @Override
    public void connectFailed() {
        Log.i("app", "Failed to connect");
    }

    @Override
    public void disconnected() {
        Log.i("app", "Disconnected after connecting successfully");
    }
});
```

To disconnect from the sensor, call the `disconnect()` method. If the sensor is
not connected, then nothing happens. If the sensor is connected, then it will
be disconnected and the `disconnect()` method of the `ConnectListener` that was
passed to `connect()` will be called.

```java
sensor.disconnect();
```

### Observing connection state

The `BioStampManager` provides a way to keep track of the connection state of
all sensors in one place: `BioStampManager.getBioStampsLiveData()` returns a
[LiveData][6] whose value is a `Map` containing an entry for each `BioStamp`
that has been accessed since the application was launched. The key is the
serial number and the value is the `BioStamp` object. The observer is notified
of a change any time any of the sensors' connection state changes.

For example, this code prints a message every time a sensor connects or
disconnects.

```java
BioStampManager bs = BioStampManager.getInstance();
bs.getBioStampsLiveData().observe(this, sensors -> {
    int count = 0;
    for (BioStamp sensor : sensors.values()) {
        if (sensor.getState() == BioStamp.State.CONNECTED) {
            count++;
        }
    }
    Log.i("app", String.format("There are %d sensors connected", count));
});
```

## Controlling sensors

Once a connection to a sensor is established, it is controlled through the
`BioStamp` object which provides methods for executing various sensor tasks.
See the Javadoc for the `BioStamp` class for a full list of all sensor tasks.
All of these methods are asynchronous and have a similar interface: the method
accepts a `BioStamp.Listener` as one of its parameters. The method returns
immediately and the task starts executing in the background. When the task
completes, the listener is called with the result of the task. The listener is
always called on the main thread so it is safe to directly access the Android
UI.

The `BioStamp` executes one task at a time; if task methods are called while
another task is already executing, they are enqueued. It is guaranteed that if
a task method is called, then its listener will be called exactly once within a
finite time, regardless of whether the task succeeds, fails, or times out. If
the connection to the sensor is lost, then all pending tasks' listeners are
called with an error.

The `BioStamp.Listener` is a method with the signature:

```java
void done(Throwable error, T result);
```

`error` is defined for all tasks and indicates whether the task succeeded or
failed. If the task succeeded then `error` is `null`. If the task failed, then
`error` indicates what went wrong. For tasks which return a result, it is
passed in `result`, which is only valid if `error` is `null`. For tasks which
do not return a result, `result` is ignored.

For example, this is a simple task that does not return any result other
than success or failure.

```java
sensor.blinkLed((error, result) -> {
    if (error == null) {
        Log.i("app", "Blinked the LED successfully");
    } else {
        Log.i("app", "Failed to blink the LED: " + error.toString());
    }
});
```

This is an example of a task which does return a result:

```java
sensor.getSensorStatus((error, result) -> {
    if (error == null) {
        Log.i("app", String.format("The firmware version is %s", result.getFirmwareVersion()));
    } else {
        Log.i("app", "Failed to get sensor status: " + error.toString());
    }
});
```

Some long-running tasks additionally accept an optional
`BioStamp.ProgressListener`. This is a method whose signature is:

```java
void updateProgress(double progress);
```

As the task executes the method is called periodically on the main thread with
a value that starts at 0 and increments until it reaches 1 when the task is
complete.

## Sensing

### Overview

The primary function of the BioStamp is sensing: it captures continuous streams
of samples from one or more of its sensors. It then streams those samples in
real time over a BLE connection to a device like a smarphone, and / or records
those samples to its internal flash memory to be downloaded later.

At startup the BioStamp is idle with all sensors off. Sensing is started by
sending a command over BLE which contains a sensing configuration. The sensing
configuration indicates which sensors are to be enabled and contains the
settings for each sensor (sampling rates, ranges, and modes). The sensing
configuration also contains a flag that indicates whether or not samples should
be recorded to flash memory. Sensing continues until a stop command is sent
over BLE, flash memory fills up (if recording is enabled), or the battery dies.
A fixed duration can also be requested when starting sensing, and sensing will
stop automatically after that time. After sensing is stopped the sensor returns
to the idle state ready for sensing to be started again.

It is not possible to enable or disable individual sensors or change sensor
settings while sensing is started. The only way to make a change is to stop
sensing and then start sensing again with a different sensing configuration.

Any time sensing is started, streaming can be enabled to stream a specific type
of samples in real time over BLE. Once sensing is started, the sensors remain
enabled, sampling and consuming power, even if they are not being streamed or
recorded to flash. It is possible to disconnect the BLE connection, reconnect,
and start streaming again, with sensors always remaining enabled.

### Sensor configuration

The sensor configuration is represented by the `SensorConfig` object. It
provides a `Builder` which may be used to create a sensor configuration
programmatically. For example, this code creates a sensor configuration that
enables:

* Motion sensor in accelerometer + gyroscope mode with +/-16G accelerometer
  full scale range and +/-2000DPS gyroscope full scale range, at 125Hz sampling
  rate
* AFE4900 in biopotential (ECG) mode with gain 12
* Recording all samples to flash memory

```java
SensorConfig config = new SensorConfig.Builder()
        .enableMotionAccelGyro(8000, 16, 2000)
        .enableAfe4900Ecg(12)
        .enableRecording()
        .build();
```

Note that `enableMotionAccelGyro` accepts a sampling period in microseconds.
8000µs corresponds to a sampling rate of 1000000 / 8000 = 200Hz.

For the purpose of the sensor configuration, the BioStamp defines four sensors:
* Motion sensor ([ICM-20948][8])
* Biopotential (ECG) and PPG ([AFE4900][9])
* Bio-impedance ([AD5940][10])
* Environment (temperature and atmospheric pressure)

Even though the temperature and pressure sensors are physically separate, to
simplify usage they are treated as a single 'environment' sensor, always
enabled together and sampled at the same rate.

The Javadoc for `SensorConfig.Builder` lists all of the available modes for
each sensor.

The sensor configuration must enable at least one sensor, and may only contain
one configuration for each sensor. For example the following configuration is
not valid because it specifies multiple configurations for the motion sensor.

```java
// Not valid - throws IllegalArgumentException
SensorConfig config = new SensorConfig.Builder()
        .enableMotionAccel(8000, 16)
        .enableMotionAccelGyro(8000, 16, 2000)
        .build();
```

When sensing is enabled, the result of the `getSensorStatus` task includes the
sensor configuration that is currently in use. The `SensorConfig` implements
`toString()` to generate a human readable description of the configuration.
This example gets the sensor configuration and prints it.

```java
sensor.getSensorStatus((error, result) -> {
    if (error == null) {
        SensorConfig config = result.getSensingInfo().getSensorConfig();
        if (config == null) {
            Log.i("app", "Sensing is not enabled");
        } else {
            Log.i("app", config.toString());
        }
    }
});
```

The output looks like:

```
Rec
Motion(Accel+Gyro +/-16G +/-500dps 100Hz) Environment(1Hz) AFE(ECG)
```

### Starting and Streaming

There are three steps to receive streaming sensor samples from the BioStamp
over BLE:
1. Start sensing. The sensors within the BioStamp are turned on and configured,
   but the data goes nowhere unless recording to flash is enabled.
2. Enable streaming. Sensor samples start streaming over the BLE connection to
   the SDK.
3. Add a streaming listener. This makes the samples available to the application.

Streaming is enabled or disabled individually for each sensor. For example, if
the sensor configuration enables both the motion and bio-impedance sensors,
streaming can be enabled for only motion samples, only bio-impedance samples,
or both. Because the BLE connection has limited bandwidth and streaming
increases power consumption, streaming should only be enabled when needed.

For example, we can we create a sensor configuration and start sensing. This
configuration enables the accelerometer at 31.25Hz and enables temperature and
pressure at 1Hz, for streaming only without recording to flash.

```java
SensorConfig config = new SensorConfig.Builder()
        .enableMotionAccel(32000, 16)
        .enableEnvironment(1000000)
        .build();

sensor.startSensing(config, 0, null, (error, result) -> {
    if (error == null) {
        Log.i("app", "Sensing enabled successfully");
    }
});
```

Once sensing is enabled, we can enable streaming. Here we only enable streaming
for the environment samples:

```java
sensor.startStreaming(StreamingType.ENVIRONMENT, (error, result) -> {
    if (error == null) {
        Log.i("app", "Streaming enabled successfully");
    }
});
```

Once streaming is enabled we can add a listener which will receive the samples.
The `BioStamp.StreamingListener` is a method with the signature:
```java
boolean handleRawSamples(RawSamples samples);
```

The listener is always called on the main thread so may directly access the
Android UI. The listener should return `true` to continue receiving samples, or
`false` to stop receiving samples and be removed from the list of registered
listeners for its type of sample.

```java
sensor.addStreamingListener(StreamingType.ENVIRONMENT, samples -> {
    Log.i("app", "Received environment samples");
    return true;
});
```

[1]: https://developer.android.com/studio/write/java8-support
[2]: https://help.github.com/en/packages
[3]: https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
[4]: https://developer.android.com/reference/android/app/Application
[5]: https://developer.android.com/training/permissions/requesting
[6]: https://developer.android.com/topic/libraries/architecture/livedata
[7]: https://en.wikipedia.org/wiki/Bluetooth_Low_Energy
[8]: https://invensense.tdk.com/products/motion-tracking/9-axis/icm-20948/
[9]: http://www.ti.com/product/AFE4900
[10]: https://www.analog.com/en/products/ad5940.html

