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

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.MultiChannelTemperature;
import com.mbientlab.metawear.module.SingleChannelTemperature;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by etsai on 8/19/2015.
 */
public class TemperatureFragment extends SingleDataSensorFragment {
    private static final int TEMP_SAMPLE_PERIOD= 500, SINGLE_EXT_THERM_INDEX= 1;
    private static final String STREAM_KEY= "temp_stream";

    private byte gpioDataPin= 0, gpioPulldownPin= 1;
    private boolean activeHigh= false;

    private long startTime= -1;
    private Temperature tempModule;
    private Timer timerModule;
    private List<MultiChannelTemperature.Source> availableSources= null;
    private List<String> spinnerEntries= null;
    private int selectedSourceIndex= 0;
    private final RouteManager.MessageHandler tempMsgHandler= new RouteManager.MessageHandler() {
        @Override
        public void process(Message message) {
            final Float celsius = message.getData(Float.class);

            LineData data = chart.getData();

            if (startTime == -1) {
                data.addXValue("0");
                startTime = System.currentTimeMillis();
            } else {
                data.addXValue(String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.f));
            }

            data.addEntry(new Entry(celsius, sampleCount), 0);

            sampleCount++;
        }
    };

    private Spinner sourceSelector;

    public TemperatureFragment() {
        super(R.string.navigation_fragment_temperature, "celsius", R.layout.fragment_temperature, 15, 45);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sourceSelector= (Spinner) view.findViewById(R.id.temperature_source);
        sourceSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View innerView, int position, long id) {
                if (tempModule instanceof MultiChannelTemperature) {
                    int[] extThermResIds = new int[]{R.id.ext_thermistor_data_pin_wrapper, R.id.ext_thermistor_pulldown_pin_wrapper,
                            R.id.ext_thermistor_active_setting_title, R.id.ext_thermistor_active_setting
                    };

                    for (int resId : extThermResIds) {
                        if (availableSources.get(position) instanceof MultiChannelTemperature.ExtThermistor) {
                            view.findViewById(resId).setVisibility(View.VISIBLE);
                        } else {
                            view.findViewById(resId).setVisibility(View.GONE);
                        }
                    }
                } else if (tempModule instanceof SingleChannelTemperature) {
                    int[] extThermResIds = new int[]{R.id.ext_thermistor_data_pin_wrapper, R.id.ext_thermistor_pulldown_pin_wrapper
                    };

                    for (int resId : extThermResIds) {
                        if (position == SINGLE_EXT_THERM_INDEX) {
                            view.findViewById(resId).setVisibility(View.VISIBLE);
                        } else {
                            view.findViewById(resId).setVisibility(View.GONE);
                        }
                    }
                }

                selectedSourceIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (spinnerEntries != null) {
            final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerEntries);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sourceSelector.setAdapter(spinnerAdapter);
            sourceSelector.setSelection(selectedSourceIndex);
        }

        final EditText extThermPinText= (EditText) view.findViewById(R.id.ext_thermistor_data_pin);
        extThermPinText.setText(String.format("%d", gpioDataPin));
        extThermPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextInputLayout extThermWrapper = (TextInputLayout) view.findViewById(R.id.ext_thermistor_data_pin_wrapper);
                try {
                    gpioDataPin = Byte.valueOf(s.toString());
                    view.findViewById(R.id.sample_control).setEnabled(true);
                    extThermWrapper.setError(null);
                } catch (Exception e) {
                    view.findViewById(R.id.sample_control).setEnabled(false);
                    extThermWrapper.setError(e.getLocalizedMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final EditText pulldownPinText= (EditText) view.findViewById(R.id.ext_thermistor_pulldown_pin);
        pulldownPinText.setText(String.format("%d", gpioPulldownPin));
        pulldownPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextInputLayout extThermWrapper = (TextInputLayout) view.findViewById(R.id.ext_thermistor_pulldown_pin_wrapper);

                try {
                    gpioPulldownPin = Byte.valueOf(s.toString());
                    view.findViewById(R.id.sample_control).setEnabled(true);
                    extThermWrapper.setError(null);
                } catch (Exception e) {
                    view.findViewById(R.id.sample_control).setEnabled(false);
                    extThermWrapper.setError(e.getLocalizedMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final Spinner activeSelections= (Spinner) view.findViewById(R.id.ext_thermistor_active_setting);
        activeSelections.setSelection(activeHigh ? 1 : 0);
        activeSelections.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeHigh = (position != 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        timerModule= mwBoard.getModule(Timer.class);
        tempModule= mwBoard.getModule(Temperature.class);

        spinnerEntries = new ArrayList<>();
        if (tempModule instanceof MultiChannelTemperature) {
            availableSources = ((MultiChannelTemperature) tempModule).getSources();

            for (byte i = 0; i < availableSources.size(); i++) {
                spinnerEntries.add(availableSources.get(i).getName());
            }
        } else if (tempModule instanceof SingleChannelTemperature) {
            spinnerEntries.add("NRF On-Die Sensor");
            spinnerEntries.add("External Thermistor");
        }

        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, spinnerEntries);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSelector.setAdapter(spinnerAdapter);
        sourceSelector.setSelection(selectedSourceIndex);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_temp_source, R.string.config_desc_temp_source));
        adapter.add(new HelpOption(R.string.config_name_temp_active, R.string.config_desc_temp_active));
        adapter.add(new HelpOption(R.string.config_name_temp_data_pin, R.string.config_desc_temp_data_pin));
        adapter.add(new HelpOption(R.string.config_name_temp_pulldown_pin, R.string.config_desc_temp_pulldown_pin));
    }

    @Override
    protected void setup() {
        if (tempModule instanceof MultiChannelTemperature) {
            if (availableSources.get(selectedSourceIndex) instanceof MultiChannelTemperature.ExtThermistor) {
                ((MultiChannelTemperature.ExtThermistor) availableSources.get(selectedSourceIndex))
                        .configure(gpioDataPin, gpioPulldownPin, activeHigh);
            }

            ((MultiChannelTemperature) tempModule).routeData().fromSource(availableSources.get(selectedSourceIndex)).stream(STREAM_KEY).commit()
                    .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            streamRouteManager = result;
                            result.subscribe(STREAM_KEY, tempMsgHandler);
                        }
                    });
            timerModule.scheduleTask(new Timer.Task() {
                @Override
                public void commands() {
                    ((MultiChannelTemperature) tempModule).readTemperature(availableSources.get(selectedSourceIndex));
                }
            }, TEMP_SAMPLE_PERIOD, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.start();
                }
            });
        } else if (tempModule instanceof SingleChannelTemperature) {
            if (selectedSourceIndex == SINGLE_EXT_THERM_INDEX) {
                ((SingleChannelTemperature) tempModule).enableThermistorMode(gpioDataPin, gpioPulldownPin);
            } else {
                ((SingleChannelTemperature) tempModule).disableThermistorMode();
            }

            tempModule.routeData().fromSensor().stream(STREAM_KEY).commit()
                    .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            streamRouteManager = result;
                            result.subscribe(STREAM_KEY, tempMsgHandler);
                        }
                    });
            timerModule.scheduleTask(new Timer.Task() {
                @Override
                public void commands() {
                    tempModule.readTemperature();
                }
            }, TEMP_SAMPLE_PERIOD, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                @Override
                public void success(Timer.Controller result) {
                    result.start();
                }
            });
        }
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
