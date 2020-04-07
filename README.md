# BioStamp3™ Android SDK

Build Android applications that communicate with BioStamp3™ sensors via
[Bluetooth Low Energy (BLE)][7].

## Table of Contents

  * [Requirements](#requirements)
  * [Getting started](#getting-started)
  * [Connecting to sensors](#connecting-to-sensors)
  * [Controlling sensors](#controlling-sensors)
  * [Sensing](#sensing)
  * [Recordings](#recordings)

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
    implementation 'com.mc10inc.biostamp3:sdk:0.0.1-e3a004e'
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

### Raw Samples

When streaming is enabled, the `BioStamp.StreamingListener` is called
repeatedly with a `RawSamples` object which contains a set of timestamped
samples from one sensor. Each sample has one or more 'columns' which contain a
certain type of value. All possible types of values are defined by
`RawSamples.ColumnType`.

For example, if in the sensor configuration we enable the motion sensor in
accelerometer-only mode and then we add a streaming listener of type
`StreamingType.MOTION`, we will receive `RawSamples` objects whose samples
contain columns of type `ACCEL_X`, `ACCEL_Y`, and `ACCEL_Z`. The `RawSamples`
provides methods for accessing the timestamp of each sample and each individual
value. In the following example, `samples` is a `RawSamples` object containing
accelerometer samples. Values are always returned as floating point numbers in
physical units; for example accelerometer samples are in units of G's. See the
Javadoc for `RawSamples.ColumnType` for the units of each type of sample.

```java
Log.i("app", String.format("There are %d samples", samples.getSize()));
Log.i("app", String.format("The timestamp of the first sample is %f seconds",
        samples.getTimestamp(0)));
Log.i("app", String.format("The timestamp of the last sample is %f seconds",
        samples.getTimestamp(samples.getSize() - 1)));
Log.i("app", String.format("The first sample value is X=%f G, Y=%f G, Z=%f G",
        samples.getValue(RawSamples.ColumnType.ACCEL_X, 0),
        samples.getValue(RawSamples.ColumnType.ACCEL_Y, 0),
        samples.getValue(RawSamples.ColumnType.ACCEL_Z, 0)));
```

If we had enabled the motion sensor in accelerometer + gyroscope mode by
calling `enableMotionAccelGyro` instead of `enableMotionAccel` when setting up
the sensor configuration, then we would also be able to access `GYRO_X`,
`GYRO_Y`, and `GYRO_Z` values. But since we did not, those values are not
available and an exception would be thrown for the example above if we called:

```java
// Throws IllegalArgumentException since we didn't enable the gyro
samples.getValue(RawSamples.ColumnType.GYRO_X, 0);
```

The Javadoc for each `enableXXXXX()` method of `SensorConfig.Builder` lists the
types of values that are generated by that sensor in that mode.

## Recordings

### Overview

The sensor's flash memory is organized as a list of recordings in order from
oldest to newest. Every time sensing is started with the recording enabled flag
set, a new recording is created, and that recording ends when sensing is
stopped. The same sensor configuration is used unchanged for the duration of a
recording; the only way to change the sensor configuration is to stop sensing
and start sensing again, creating a new recording with a different
configuration. The recording contains all samples from all sensors that were
enabled in the sensor configuration.

Recordings may be downloaded from the sensor in any order. There are two ways
to free up space in flash memory for new recordings: either delete all
recordings at once, or delete only the oldest recording. Deleting only the
oldest recording leaves all other recordings in a contiguous block from oldest
to newest; it is not possible to select a recording to delete at random. To get
all of the data from the sensor, an application can either download all
recordings at once and then delete all recordings, or can repeatedly download
and delete the oldest recording until there are no recordings left.

The SDK maintains an internal database within the Android application's local
storage to hold downloaded recordings. Recordings are always downloaded into
this database, which stores recording data in a compact binary format. The
database can hold incomplete downloads, and it is possible to resume and finish
an incomplete download. Once a recording is fully downloaded its contents may
be accessed by the application; the SDK decodes the data and supplies it to the
application as floating-point numbers scaled to physical units like G's and
volts.

Some applications may keep recordings in the SDK's database and access them
through the SDK. Other applications may decode a recording as soon as it is
downloaded, either send it to a remote server or store it in a different
format, and then delete it from the SDK's database after decoding.

### Metadata

The sensor can store a small amount of metadata with each individual recording,
which is included with other info like start time and configuration when
recordings are listed or downloaded. Metadata is optional and its format is
defined by the application. Some possible uses of metadata are identifying the
subject wearing the sensor and / or the body location of the sensor.

The SDK accepts metadata as a `byte[]` array. The maximum size is given by the
`getRecordingMetadataMaxSize()` method of `BioStamp`. Currently the size is
limited by firmware to 128 bytes, but this may change in the future.

In the following example the application uses the metadata to hold a human
readable string. The metadata does not have to be a string; a different
application might use a binary serialization format instead.

```java
SensorConfig config = new SensorConfig.Builder()
        .enableMotionAccel(32000, 16)
        .enableRecording();
        .build();

String metadataStr = "Right thigh recording for Mar 2";
byte[] metadata = metadataStr.getBytes(StandardCharsets.UTF_8);
if (metadata.length > sensor.getRecordingMetadataMaxSize()) {
    // Metadata is too large - exception would be thrown!
}

sensor.startSensing(config, 0, metadata, (error, result) -> {
    if (error == null) {
        Log.i("app", "Sensing enabled successfully");
    }
});
```

### Annotations

An annotation is a small message of application-defined data which is inserted
into a recording alongside sensor samples while the recording is in progress.
Annotations are timestamped with the time the sensor receives them and are
included in the downloaded recording.

Similarly to [recording metadata](#metadata), the SDK accepts an annotation as
a `byte[]` array. The maximum size is given by the `getAnnotationDataMaxSize()`
method of `BioStamp`. Currently the size is limited by firmware to 220 bytes,
but this may change in the future.

In the following example the application defines the format of the metadata as
a human readable string.

To insert an annotation in a recording in progress:

```java
String annoStr = "Subject starts running now";
byte[] annotation = annoStr.getBytes(StandardCharsets.UTF_8);
if (annotation.length > sensor.getAnnotationDataMaxSize()) {
    // Annotation is too large - exception would be thrown!
}
sensor.annotate(annotation, (error, timestamp) -> {
    if (error == null) {
        Log.i("app", String.format("Annotation added at timestamp %f", timestamp));
    }
});
```

The sensor returns the exact timestamp that it gave the annotation as a
floating point number in seconds. When the recording is downloaded and decoded,
the annotation timestamp in the recording will exactly match this value.

### Downloading

The first step in downloading a recording is to get a list of recordings in the
sensor's flash memory. The `RecordingInfo` object is used to represent a
recording in a sensor's flash memory, or a recording in the SDK's recording
database. `getRecordingList` returns a `List<RecordingInfo>` list of all
recordings.

```java
sensor.getRecordingList((error, result) -> {
    if (error == null) {
        Log.i("app", String.format("There are %d recordings in memory", result.size()));
        if (result.size() > 0) {
            RecordingInfo recInfo = result.get(0);
            Log.i("app", String.format("Duration of the oldest recording is %d seconds",
                    recInfo.getDurationSec()));
        }
    }
});
```

While sensing is enabled, recordings may be listed and older recordings may be
downloaded. However, the current recording that is still in progress may not be
downloaded until sensing is stopped.

```java
if (!recInfo.isInProgress()) {
    // Can download this recording now    
} else {
    // Still in progress; must call sensor.stopSensing() before downloading
}
```

The `RecordingInfo` object also indicates the status of that recording's
download within the SDK's database.

```java
DownloadStatus downloadStatus = recInfo.getDownloadStatus();
if (downloadStatus == null) {
    // Recording in sensor's memory is not downloaded yet.
} else {
    if (downloadStatus.isComplete()) {
        // Recording is in the database; can safely delete it from the sensor now.
    } else {
        // Recording is partially downloaded; can resume the download now.
    }
}
```

To download a recording we use the `RecordingInfo` object as the parameter that
indicates which recording to download. Downloading a recording can take a long
time, so the download recording task accepts a progress listener which may be
used to update a progress bar in the application's UI.

```java
sensor.downloadRecording(recInfo, (error, result) -> {
    if (error == null) {
        Log.i("app", "Download completed successfully");
    } else {
        Log.i("app", "Download failed");
    }
}, progress -> {
    Log.i("app", String.format("The download is %d%% complete",
            (int)(progress * 100)));
});
```

No other tasks can be executed through the `BioStamp` object while the download
is in progress. We can stop the download before it is complete.

```java
sensor.cancelTask();
```

Once the recording is downloaded, if it is the oldest recording in the sensor's
memory (the first element of the list returned by `getRecordingList`), then it
can be deleted to free up space.

```java
sensor.clearOldestRecording((error, result) -> {
    if (error == null) {
        Log.i("app", "Deleted oldest recording");
    }
});
```

Instead of deleting recordings one by one, they can all be deleted at once.

```java
sensor.clearAllRecordings((error, result) -> {
    if (error == null) {
        Log.i("app", "Deleted all recordings");
    }
});
```

### Recording Database

The `BioStampDb` singleton object provides access to the recording database. To
access the database:

```java
BioStampDb db = BioStampManager.getInstance().getDb();
```

The `BioStampDb` methods may sometimes take a long time to execute. Therefore
it is recommended (although not mandatory) to call them from a background
thread instead of the main thread to avoid blocking the app's UI.

To get a list of all recordings in the database, call `getRecordings()`. The
result is a list of `RecordingInfo`, exactly like the response to
`getRecordingList` from the sensor.

```java
List<RecordingInfo> recordings = db.getRecordings();
Log.i("app", String.format("There are %d recordings in the database",
        recordings.size()));
```

Some applications may have a UI which needs to be updated based on the contents
of the database. To support this, the list of recordings is also provided as a
[LiveData][6] which notifies observers any time the contents of the database
change.

```java
db.getRecordingsLiveData().observe(this, recordings -> {
    Log.i("app", String.format("There are %d recordings in the database",
            recordings.size()));
});
```

We can query the `RecordingInfo` for each recording. In this example we assume
that the application is using metadata and has defined its format as a string,
as in the [Metadata](#metadata) example above.

```java
RecordingInfo recInfo = recordings.get(0);
Log.i("app", String.format("This recording is from sensor %s",
        recInfo.getSerial()));
Log.i("app", String.format("It started at date and time %s",
        recInfo.getDurationSec()));
Log.i("app", String.format("The sensor configuration is: %s",
        recInfo.getSensorConfig().toString()));
if (recInfo.getMetadata() == null) {
    Log.i("app", "There is no metadata");
} else {
    String metadataStr = new String(recInfo.getMetadata(), StandardCharsets.UTF_8);
    Log.i("app", String.format("The metadata is: %s", metadataStr));
}
if (recInfo.getDownloadStatus().isComplete()) {
    Log.i("app", String.format("The download is complete and it can be decoded now"));
}
```

Once we are done with a recording in the database we can delete that specific
recording to free up space. The `RecordingInfo` object identifies which
recording to delete.

```java
db.deleteRecording(recInfo);
```

Or all recordings in the database can be deleted at once.

```java
db.deleteAllRecordings();
```

### Decoding a Recording

The `RecordingDecoder` object is used to decode a recording from the database.
It is constructed with a `RecordingInfo` object that identifies which recording
to decode. The application registers a listener for each type of sensor sample
it wants to decode, and optionally a listener for annotations. Once the
listeners are registered, the `decode()` method is called and the
`RecordingDecoder` iterates through the entire recording, passing the data to
the listeners as `RawSamples` objects. These are exactly the same as the
objects received from the sensor during real time streaming, as described in
[Raw Samples](#raw-samples).

For a large recording, the `decode()` method can take a long time to execute so
it is important to call it from a background thread, not the main thread, to
avoid blocking the UI. Once the `decode()` method returns, the entire recording
has been decoded.

In this example we print out all of the annotations and ignore the sensor
samples. We assume that the application has defined the format of the
annotation as a human readable string.

```java
RecordingDecoder decoder = new RecordingDecoder(recInfo);

decoder.setAnnotationListener(annotation -> {
    String annoStr = new String(annotation.getData(), StandardCharsets.UTF_8);
    Log.i("app", String.format("Annotation at %f: %s\n",
            annotation.getTimestamp(), annoStr));
});

decoder.decode();
```

This example is for an accelerometer recording. We read all of the
accelerometer samples into memory and calculate the mean acceleration on the Z
axis in G's. We create a list with one element for each sample. Each element is
a list whose elements are the timestamp, X axis, Y axis, and Z axis.

```java
RecordingDecoder decoder = new RecordingDecoder(recInfo);

List<List<Double>> accelSamples = new ArrayList<>();
decoder.setListener(RecordingDecoder.RawSamplesType.MOTION, samples -> {
    for (int i = 0; i < samples.getSize(); i++) {
        List<Double> s = new ArrayList<>();
        s.add(samples.getTimestamp(i));
        s.add(samples.getValue(RawSamples.ColumnType.ACCEL_X, i));
        s.add(samples.getValue(RawSamples.ColumnType.ACCEL_Y, i));
        s.add(samples.getValue(RawSamples.ColumnType.ACCEL_Z, i));
        accelSamples.add(s);
    }
});

decoder.decode();

Log.i("app", String.format("The timestamp of the first sample is %f",
        accelSamples.get(0).get(0)));
Log.i("app", String.format("The timestamp of the last sample is %f",
        accelSamples.get(accelSamples.size() - 1).get(0)));

double zSum = 0;
for (List<Double> sample : accelSamples) {
    zSum += sample.get(3);
}
Log.i("app", String.format("The mean value of the Z axis is %f G",
        zSum / accelSamples.size()));
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

