---
title: Baby Buddy for Android - Privacy Policy
subtitle: Version 1.1
author:
- Paul Konstantin Gerke
date: 2023-03-27
documentclass: scrartcl

---

[Open privacy policy as PDF](https://www.pkgsoftware.eu/media/babybuddy-privacy-v1.1.pdf)

This document explains how the Android app "Baby Buddy for Android"
uses your data to provide its services, how it processes your data, and how you 
can delete your data from the app.

# Short summary

The Baby Buddy for Android app (hereafter: "the app" or "this app") allows you to 
interact with a Baby Buddy Website. For this purpose, data is stored locally on the your 
Android device. The app does not interact with any other service, website or 
device besides your Android device and the connected 
[Baby Buddy Website](https://github.com/babybuddy/babybuddy).

## What is stored?

* This app stores the following data locally on your Android device:
	* Login information for logging in to the Baby Buddy Website you connect the app to
	* Copies of data from the Baby Buddy Website you connect the app to (more details in section [Baby Buddy local data](#baby-buddy-local-data))

* The app does not connect with any other service, website or device. This means:
	- The app does not communicate with any other service or website other than the Baby Buddy Website you connect the app to.
	- The app does not store data on any other devices besides your Android device and the Baby Buddy Website that you connect the app to.

# What is your data used for?

The app uses your data to provide its features and functions. The app's purpose is to
streamline access to a connected Baby Buddy Website so that it is easier to use on a handheld
Android device. In order to do so, the app uses your login information to authenticate itself
with a Baby Buddy Website. It then copies some of the data from the website on 
your Android device to provide you fast access to information stored on the
Baby Buddy Website.

If you choose to start or stop timers, or log events for children, data you provide
through those actions is sent and stored on the connected Baby Buddy Website.

# Security

Data transfers between the app and the connected Baby Buddy Website is only secured (encrypted) if the Baby Buddy Website uses Secure HTTP (`https`).

You can choose to override a security warning and login to an unsecured (`http`) Baby Buddy Website if you like. Communication between the Android app and the server will then be _unencrypted_ and _insecure_. This mode is mainly intended for local network installations of the Baby Buddy Website which do not necessarily require full encryption of the communication between app and website.

# Baby Buddy local data

Baby Buddy uses a local file on your Android device to store:

* The API-Token that associated with the user that you use to login to the connected Baby Buddy Website.
* A subset of data that is stored on the Baby Buddy Website as part of a local _cache_. This 
data includes, but is not limited to:
    - All children (names, ids, etc.) registered on the Baby Buddy Website
    - The name of the user that is logged in to the Baby Buddy Website
    - A subset of all logged events for all children
    - Tag names, tag colors
    - The timezone of the Baby Buddy Website
    - User messages stored as part of logged events for children
    - The list of timers, start times of the timers, and names of all timers

# Data retention and deletion

Data which is stored on your Android device can be deleted by you at any time
through the app by:

- logging out from a connected Baby Buddy Website via the app menu,
- or by deleting the app-data via the Android App-settings interface.

Data which the app sends to a connected Baby Buddy Website will be stored on the Baby 
Buddy Website and cannot be deleted directly through the app. How data is deleted from
the website depends on the privacy policy and implementation of the Baby Buddy Website.
*This is not part of the privacy policy of this Android app*.

## No data collection by third parties

Baby Buddy for Android does not use a third-party server or other service 
to provide its functions. Instead, you are expected to host your own instance of 
the [Baby Buddy Website (https://github.com/babybuddy/babybuddy)](https://github.com/babybuddy/babybuddy) 
which you control yourself.

All information entered into the application will only be transmitted to the connected server. Baby Buddy for Android does not collect and share your or your device's data with any other party. 

# Contact information

~~~~~~~
Paul Konstantin Gerke
Dominicanenstraat 4R
6521KD Nijmegen (NL)
~~~~~~~

You can find more contact options on the developer website:

[https://www.pkgsoftware.eu/contact.php](https://www.pkgsoftware.eu/contact.php).

### Document Changes

- 2022-05-08: v1.0 First version of this document published
- 2023-03-27: v1.1 Added more details and an html version