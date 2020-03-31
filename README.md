# BioStamp3™ Android SDK

Build Android applications that communicate with BioStamp3™ sensors via
[Bluetooth Low Energy (BLE)][7].

## Table of Contents

  * [Requirements](#requirements)
  * [Getting started](#getting-started)
  * [Connecting to sensors](#connecting-to-sensors)

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

[1]: https://developer.android.com/studio/write/java8-support
[2]: https://help.github.com/en/packages
[3]: https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
[4]: https://developer.android.com/reference/android/app/Application
[5]: https://developer.android.com/training/permissions/requesting
[6]: https://developer.android.com/topic/libraries/architecture/livedata

