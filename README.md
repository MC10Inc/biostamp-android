# BioStamp® 3.0 Android SDK

The SDK is used to build Android applications which communicate with BioStamp®
3.0 sensors.

## Table of Contents

  * [Requirements](#requirements)
  * [Getting started](#getting-started)

## Requirements

The minimum required Android SDK version for applications built with this SDK
is API level 24 (Android 7.0 Nougat). The application will not run on older
devices.

The application must enable [Java 8 language features][1].

The device that the application runs on must support at minimum Bluetooth 4.0
with Bluetooth Low Energy (BLE). Bluetooth 4.0 or 4.1 is sufficient for command
and control of the sensor but data throughput is slow. Bluetooth 4.2 provides
acceptable data throughput, and Bluetooth 5.0 or newer provides a further
improvement.

## Getting started

### GitHub Packages

The SDK is supplied as an Android library which is hosted on [GitHub
Packages][2].  To access the library, first [create a personal access token][3]
with the `read:packages` scope for your GitHub account. To make the access
token available to any Android Studio project, create a file in your home
directory named `.gradle/gradle.properties`:

```
gpr.user=your_github_username
gpr.key=your_access_token
```

### Project Setup

To use the SDK, start by opening your existing Android application or creating
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
`gradle.properties`.
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

The BioStamp SDK must be initialized before any other SDK functions may be
called. This must be done once every time the application is launched. It is
recommended to initialize the SDK from within the `onCreate` method of the
[Application][4] class.

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

If you are creating a new application or your existing application does not subclass Application, then create a class like this:
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

### Documentation

In addition to this document the SDK supplies Javadoc documentation for classes
and methods that are part of the SDK's public API. The Javadoc is automatically
downloaded along with the library and is accessible from within Android Studio.

To view Javadoc from within Android Studio, click on an SDK class or method
(for example `BioStampManager`) in a source file and press Ctrl-Q. A popup
containing the documentation will open.

It is also possible to browse the Javadocs in a web browser, through a local
web server built in to Android Studio. To browse the docs, click on any
BioStamp SDK class or method (for example `BioStampManager`) in a source file,
and select View -> External Documentation from the Android Studio menu. Note
that this menu item does not appear unless code with external Javadoc is
selected.

[1]: https://developer.android.com/studio/write/java8-support
[2]: https://help.github.com/en/packages
[3]: https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
[4]: https://developer.android.com/reference/android/app/Application
