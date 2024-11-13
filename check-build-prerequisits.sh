#!/bin/bash

set -e

echo "== Checking build tools =="

echo Check if GNU make is installed...
if ! make --version > /dev/null 2>&1 ; then
    echo "make is not installed"
    exit 1
fi
echo "Check if curl is installed..."
if ! curl --version > /dev/null 2>&1 ; then
    echo "curl is not installed"
    exit 1
fi
echo "Check if image magick is installed..."
if ! convert --version > /dev/null 2>&1 ; then
    echo "image magick is not installed"
    exit 1
fi
echo "Check if python3 and pipenv are installed..."
if ! python3 --version > /dev/null 2>&1 ; then
    echo "python3 is not installed"
    exit 1
fi
if ! pipenv --version > /dev/null 2>&1 ; then
    echo "pipenv is not installed"
    exit 1
fi
echo "Check if jq is installed..."
if ! jq --version > /dev/null 2>&1 ; then
    echo "jq is not installed"
    exit 1
fi

echo "Check if java 18.* is installed and accessible via JAVA_HOME..."
if [[ -z "$JAVA_HOME" ]]; then
    echo "JAVA_HOME is not set"
    exit 1
fi
if ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q "openjdk version \"18\." ; then
    echo "java 18 is not installed"
    exit 1
fi

echo "Verify if ANDROID_HOME is set and contains the android sdk we need..."
if [[ -z "$ANDROID_HOME" ]]; then
    echo "ANDROID_HOME is not set"
    exit 1
fi
echo "Reading targetSdk value from app/build.gradle..."
if [[ ! -f "app/build.gradle" ]]; then
    echo "app/build.gradle file not found"
    exit 1
fi
targetSdk=$(grep -oP 'targetSdk\s+\K\d+' app/build.gradle)
if [[ -z "$targetSdk" ]]; then
    echo "targetSdkVersion not found in app/build.gradle"
    exit 1
fi
if [[ ! -d "$ANDROID_HOME/platforms/android-$targetSdk" ]]; then
    echo "Android SDK for targetSdk $targetSdk not found in $ANDROID_HOME"
    exit 1
fi
echo "Check if Android NDK is installed..."
if ! ls "$ANDROID_HOME/ndk/"*"/build/tools" > /dev/null 2>&1 ; then
    echo "Android NDK is not installed"
    exit 1
fi

echo "== Checking build dependencies =="
echo "Check if zxing is downloaded..."
if [[ ! -e 3rdparty/zxing-cpp/zxing.cmake ]]; then
    echo "zxing is not downloaded; please run: git submodule update --init --recursive"
    exit 1
fi

echo "== All required build tools and dependencies are installed =="
