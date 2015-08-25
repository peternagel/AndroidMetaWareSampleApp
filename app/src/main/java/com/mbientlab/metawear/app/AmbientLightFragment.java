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

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Ltr329AmbientLight;

import static com.mbientlab.metawear.module.Ltr329AmbientLight.*;

/**
 * Created by etsai on 8/22/2015.
 */
public class AmbientLightFragment extends SingleDataSensorFragment {
    private static final String STREAM_KEY= "light_stream";
    private Ltr329AmbientLight ltr329Module;
    private long startTime= -1;

    public AmbientLightFragment() {
        super("Ambient Light", "illuminance", R.layout.fragment_sensor, 0.5f, 32000f);
        chartDescription= "Illuminance (lx) vs. Time";
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        ltr329Module= mwBoard.getModule(Ltr329AmbientLight.class);
    }

    @Override
    protected void setup() {
        ltr329Module.configure().setGain(Gain.LTR329_GAIN_2X)
                .setMeasurementRate(MeasurementRate.LTR329_RATE_500MS)
                .setIntegrationTime(IntegrationTime.LTR329_TIME_100MS)
                .commit();
        ltr329Module.routeData().fromSensor().stream(STREAM_KEY).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        streamRouteManager= result;
                        result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message msg) {
                                final Float lux = msg.getData(Long.class) / 1000.f;

                                LineData data = chart.getData();

                                if (startTime == -1) {
                                    data.addXValue("0");
                                    startTime= System.currentTimeMillis();
                                } else {
                                    data.addXValue(String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.f));
                                }

                                data.addEntry(new Entry(lux, sampleCount), 0);

                                sampleCount++;
                            }
                        });
                        ltr329Module.start();
                    }
                });
    }

    @Override
    protected void clean() {
        ltr329Module.stop();
    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
