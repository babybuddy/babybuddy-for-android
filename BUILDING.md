# Prerequisits

I use Linux for building the repository. Other operating systems might
work, but I did not put time into getting everything compatible.

## Flaticon API key

I am a software engineer, not a graphic designer. Therefore, I use
images and icons from third parties in this project which are _not free_.
The graphics are therefore also not part of this repository, but
this repository contains code to download and generate the required
images.

However, to do so, you will need to sign the license agreement of
the thirs party provides yourself. Right now, this means that you
need to take steps to obtain a flaticon-API license key - which
mainly consists of reading and accepting their terms of service.

Visit https://api.flaticon.com/ for instructions how to obtain an
API key. Then create the file flaticon-apikey in the root of the
project, and populate it with the apikey your received.
**DO NOT CHECKIN THIS FILE, EVER.**

Example how this looks (this NOT a real API-key!):

~~~~~~~~~~~~~~~~~~
$ cat flaticon-apikey
55fbe63a5c33311463c40204e7c1900b5828b01f1
~~~~~~~~~~~~~~~~~~

## Required software

I use Android Studio to build the project. Gradle on its own should
work, too, but you need to find out yourself how to invoke it in your
setup.

In addition the following Linux software packages are required:

 - bash
 - curl
 - GNU make
 - jq (JSON query tool) 
 - ImageMagick
   - Specifically the commands: convert, composite
 - python >=3.8
   - pipenv installed 
 - pandoc >= 3.1.1

## Building

1.Run submodule updates
```
git submodule init
git submodule update
```

2.Open `app/src/submodules/zxing-cpp/wrappers/android/zxingcpp/build.gradle` and comment out the line abiFilters:
```
         ndk {
             // speed up build: compile only arm versions
-            abiFilters 'armeabi-v7a', 'arm64-v8a'
+            // abiFilters 'armeabi-v7a', 'arm64-v8a'
         }
```

3.Open Android Studio and make sure you got `NDK` installed from the sdk-manager

That is it. The everything else is referenced from the gradle files.
Launch Android Studio, open the project, and build it. It should work.
