# Banana Leaf Nutrient Deficiency Android Prototype

Android classroom/research prototype for on-device banana leaf nutrient-deficiency image classification.

## Project Summary

This project targets an Android app that accepts a banana leaf image, runs an on-device image classifier, and displays a preliminary screening result with uncertainty information.

The app is intended for academic demonstration and research prototyping only. It is not a validated agronomic diagnostic product and must not be used as the sole basis for fertilizer treatment.

## Planned Android Stack

- Kotlin
- Jetpack Compose
- Material 3
- CameraX
- Android Photo Picker
- LiteRT/TensorFlow Lite on-device inference
- ViewModel, StateFlow, and coroutines

## Model Scope

The planned model is a MobileNetV2 `.tflite` classifier with 11 classes:

- boron
- calcium
- healthy
- iron
- magnesium
- manganese
- nitrogen
- phosphorous
- potassium
- sulphur
- zinc

Nitrogen and phosphorous are supplemental maize-derived classes and require explicit limitation messaging in the app.
