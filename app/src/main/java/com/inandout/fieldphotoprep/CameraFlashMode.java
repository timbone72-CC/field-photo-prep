package com.inandout.fieldphotoprep;

enum CameraFlashMode {
    AUTO("Flash: Auto"),
    ON("Flash: On"),
    OFF("Flash: Off");

    private final String buttonLabel;

    CameraFlashMode(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    CameraFlashMode next() {
        switch (this) {
            case AUTO:
                return ON;
            case ON:
                return OFF;
            case OFF:
            default:
                return AUTO;
        }
    }

    String buttonLabel() {
        return buttonLabel;
    }
}
