/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.app;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Accelerometer;

/**
 * Created by etsai on 8/19/2015.
 */
public class AccelerometerFragment extends ThreeAxisChartFragment {
    private static final float ACC_RANGE = 8.f, ACC_FREQ= 50.f;
    private static final String STREAM_KEY= "accel_stream";

    private Accelerometer accelModule= null;

    public AccelerometerFragment() {
        super("accel", "Acceleration (g) along XYZ axes vs. Time",
                "Accelerometer", STREAM_KEY, -ACC_RANGE, ACC_RANGE, ACC_FREQ);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException{
        accelModule= mwBoard.getModule(Accelerometer.class);
    }

    @Override
    protected void setup() {
        accelModule.setOutputDataRate(50.f);
        accelModule.setAxisSamplingRange(ACC_RANGE);

        AsyncOperation<RouteManager> routeManagerResult= accelModule.routeData().fromAxes().stream(STREAM_KEY).commit();
        routeManagerResult.onComplete(dataStreamManager);
        routeManagerResult.onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                accelModule.enableAxisSampling();
                accelModule.start();
            }
        });
    }

    @Override
    protected void clean() {
        accelModule.stop();
        accelModule.disableAxisSampling();
    }
}
