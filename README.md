# FaceTrackerDemo

This demo project refers to **Face Tracker** in [Android Vision API Samples](https://github.com/googlesamples/android-vision).

## Pre-requisites

Android Play Services SDK level 26 or greater.

## Highlight Features

* Front camera preview screen
* Face detection for the most centre and largest face using **LargestFaceFocusingProcessor**
* Mask drawing based on head tilting, left / right eyes blinking and smiling mouth

## Detector Settings

### FAST_MODE vs ACCURATE_MODE

A randomly moving face will be missing much easier under FAST_MODE than ACCURATE_MODE. Considering the importance of **mask**, **ACCURATE_MODE** is adapted in this demo at the expense of fast detection.

### Landmarks = none

No facial landmarks are required for this demo. 

Note 1: A method using the landmarks NOSE_BASE and BOTTOM_MOUTH (landmarks = all) was once considered to calculate the angle of head tilting. However, the performace appears quite similar as using Euler Z value. 

Note 2: The range of head tilting angle is between -45% and 45% according to [Google](https://developers.google.com/vision/face-detection-concepts#face_orientation).

### Classifications = all

Classifications are set to ALL in this demo. The probabilities of eyes opening and smiling are necessary to draw corresponding images on the overlay.

Take the left eye for example. When getIsLeftEyeOpenProbability() is larger than a threshold (0.7 in this demo), a fully open left eye is drawn on the canvas; when it goes smaller than another threshold (0.4), a closed eye is drawn; when the value lies between these two thresholds, a half open eye will be presented.

## Test Device

Motorola Nexus 6 (Android 6.0.1, API 23)
