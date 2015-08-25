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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.config.SensorConfig;
import com.mbientlab.metawear.app.config.SensorConfigAdapter;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Gpio.AnalogReadMode;
import com.mbientlab.metawear.module.Gpio.PullMode;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;

/**
 * Created by etsai on 8/21/2015.
 */
public class GpioFragment extends SingleDataSensorFragment {
    private static final String STREAM_KEY= "gpio_stream";
    private static final byte READ_ADC= 0, READ_ABS_REF= 1, READ_DIGITAL= 2;
    private static final int SET_OUTPUT= 0, CLEAR_OUTPUT= 1, GPIO_SAMPLE_PERIOD= 500;

    private byte gpioPin= 0;
    private int readMode= 0;
    private Gpio gpioModule;
    private Timer timerModule;
    private long startTime= -1;

    private SensorConfigAdapter configAdapter;

    private final CompletionHandler<RouteManager> GpioStreamSetup= new CompletionHandler<RouteManager>() {
        @Override
        public void success(RouteManager result) {
            streamRouteManager= result;
            result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                @Override
                public void process(Message message) {
                    final Short gpioValue = message.getData(Short.class);

                    LineData data = chart.getData();
                    if (startTime == -1) {
                        data.addXValue("0");
                        startTime= System.currentTimeMillis();
                    } else {
                        data.addXValue(String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.f));
                    }

                    data.addEntry(new Entry(gpioValue, sampleCount), 0);

                    sampleCount++;
                }
            });
        }
    };

    public GpioFragment() {
        super("Gpio", "adc", R.layout.fragment_gpio, 0, 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        configAdapter= new SensorConfigAdapter(getActivity(), R.id.sensor_config_entry_layout);
        configAdapter.setNotifyOnChange(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ListView) view.findViewById(R.id.gpio_config)).setAdapter(configAdapter);

        final Activity owner= getActivity();
        final ArrayList<SensorConfig> configSettings= new ArrayList<>();
        configSettings.add(new SensorConfig(R.string.config_name_gpio_pin, R.string.config_desc_gpio_pin,
                gpioPin, R.layout.popup_gpio_pin_config) {

            private EditText gpioPinText;

            @Override
            public void setup(View v) {
                gpioPinText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                gpioPinText.setText(String.format("%d", gpioPin));
            }

            @Override
            public void changeCommitted() {
                gpioPin = Byte.valueOf(gpioPinText.getText().toString());
                value = gpioPin;
                filenameExtraString= String.format("%d", gpioPin);
            }
        });
        final String[] readModeStringValues= owner.getResources().getStringArray(R.array.values_gpio_read_mode);
        configSettings.add(new SensorConfig(R.string.config_name_gpio_read_mode,R.string.config_desc_gpio_read_mode,
                readModeStringValues[readMode], R.layout.popup_config_spinner) {

            private Spinner readModeValues;

            @Override
            public void setup(View v) {
                readModeValues = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_gpio_read_mode, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                readModeValues.setAdapter(readModeAdapter);
                readModeValues.setSelection(readMode);
            }

            @Override
            public void changeCommitted() {
                value = readModeStringValues[readMode];
                readMode = readModeValues.getSelectedItemPosition();
            }
        });
        configSettings.add(new SensorConfig(R.string.config_name_pull_mode, R.string.config_desc_pull_mode,
                "", R.layout.popup_config_spinner) {

            private Spinner pullModeValues;
            @Override
            public void setup(View v) {
                pullModeValues = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> pullModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_gpio_pull_mode, android.R.layout.simple_spinner_item);
                pullModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                pullModeValues.setAdapter(pullModeAdapter);
            }

            @Override
            public void changeCommitted() {
                gpioModule.setPinPullMode(gpioPin, PullMode.values()[pullModeValues.getSelectedItemPosition()]);
            }
        });
        configSettings.add(new SensorConfig(R.string.config_name_output_control, R.string.config_desc_output_control,
                "", R.layout.popup_config_spinner) {

            private Spinner outputControlValues;

            @Override
            public void setup(View v) {
                outputControlValues = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> outputControlAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_gpio_output_control, android.R.layout.simple_spinner_item);
                outputControlAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                outputControlValues.setAdapter(outputControlAdapter);
            }

            @Override
            public void changeCommitted() {
                switch (outputControlValues.getSelectedItemPosition()) {
                    case SET_OUTPUT:
                        gpioModule.setDigitalOut(gpioPin);
                        break;
                    case CLEAR_OUTPUT:
                        gpioModule.clearDigitalOut(gpioPin);
                        break;
                }
            }
        });
        configAdapter.addAll(configSettings);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        gpioModule= mwBoard.getModule(Gpio.class);
        timerModule= mwBoard.getModule(Timer.class);
    }

    @Override
    protected void setup() {
        final YAxis leftAxis = chart.getAxisLeft();
        switch(readMode) {
            case READ_ADC:
                max= 1023;
                leftAxis.setAxisMaxValue(max);
                chartDescription= "Analog ADC value vs. Time";
                csvHeaderDataName= "adc";

                gpioModule.routeData().fromAnalogIn(gpioPin, AnalogReadMode.ADC).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
            case READ_ABS_REF:
                max= 3000;
                leftAxis.setAxisMaxValue(max);
                chartDescription= "Analog absolute reference value (mV) vs. Time";
                csvHeaderDataName= "abs reference";

                gpioModule.routeData().fromAnalogIn(gpioPin, AnalogReadMode.ABS_REFERENCE).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
            case READ_DIGITAL:
                max= 1;
                leftAxis.setAxisMaxValue(max);
                chartDescription= "Digital values vs. Time";
                csvHeaderDataName= "digital";

                gpioModule.routeData().fromDigitalIn(gpioPin).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
        }
        filenameExtraString= String.format("%s_pin_%d", csvHeaderDataName, gpioPin);
        chart.setDescription(chartDescription);
        timerModule.scheduleTask(new Timer.Task() {
            @Override
            public void commands() {
                switch(readMode) {
                    case READ_ADC:
                        gpioModule.readAnalogIn(gpioPin, AnalogReadMode.ADC);
                        break;
                    case READ_ABS_REF:
                        gpioModule.readAnalogIn(gpioPin, AnalogReadMode.ABS_REFERENCE);
                        break;
                    case READ_DIGITAL:
                        gpioModule.readDigitalIn(gpioPin);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized read mode: " + readMode);
                }
            }
        }, GPIO_SAMPLE_PERIOD, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
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
    protected void resetData(boolean clearData) {
        super.resetData(clearData);

        if (clearData) {
            startTime= -1;
        }
    }
}
