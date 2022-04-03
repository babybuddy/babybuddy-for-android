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
 - ImageMagick
    - Specifically the commands: convert, composite

## Building

That is it. The everything else is referenced from the gradle files.
Launch Android Studio, open the project, and build it. It should work.
