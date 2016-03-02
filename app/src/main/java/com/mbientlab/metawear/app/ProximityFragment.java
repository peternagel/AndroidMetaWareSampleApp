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
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.module.Tsl2671Proximity;

/**
 * Created by etsai on 2/23/2016.
 */
public class ProximityFragment extends SingleDataSensorFragment {
    private static final int PROXIMITY_SAMPLE_PERIOD = 33;
    private static final String STREAM_KEY= "proximity_stream";

    private long startTime= -1;
    private Tsl2671Proximity proximityyModule;
    private Timer timerModule;

    public ProximityFragment() {
        super(R.string.navigation_fragment_proximity, "adc", R.layout.fragment_sensor, 0, 1024);
    }

    @Override
    protected void setup() {
        proximityyModule.configure()
                .setTransmitterDriver(Tsl2671Proximity.TransmitterDrive.CURRENT_12_5MA)
                .commit();
        proximityyModule.routeData().fromSensor(false).stream(STREAM_KEY).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        streamRouteManager= result;
                        result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message message) {
                                LineData data = chart.getData();

                                if (startTime == -1) {
                                    data.addXValue("0");
                                    startTime = System.currentTimeMillis();
                                } else {
                                    data.addXValue(String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.f));
                                }

                                data.addEntry(new Entry(message.getData(Integer.class), sampleCount), 0);

                                sampleCount++;
                            }
                        });
                    }
                });
        timerModule.scheduleTask(new Timer.Task() {
            @Override
            public void commands() {
                proximityyModule.readProximity(false);
            }
        }, PROXIMITY_SAMPLE_PERIOD, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
            @Override
            public void success(Timer.Controller result) {
                result.start();
            }
        });
    }

    @Override
    protected void clean() {
        timerModule.removeTimers();
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        proximityyModule= mwBoard.getModule(Tsl2671Proximity.class);
        timerModule= mwBoard.getModule(Timer.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
