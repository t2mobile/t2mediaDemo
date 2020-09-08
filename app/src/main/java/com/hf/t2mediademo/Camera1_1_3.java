package com.hf.t2mediademo;

import com.t2m.Camera1Helper;
import com.t2m.CameraHelper;

public class Camera1_1_3 extends CameraBase_1_3 {
    @Override
    protected CameraHelper createCameraHelper() {
        return new Camera1Helper();
    }
}