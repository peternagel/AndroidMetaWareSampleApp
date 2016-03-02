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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOption;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Gpio.AnalogReadMode;
import com.mbientlab.metawear.module.Gpio.PullMode;
import com.mbientlab.metawear.module.Timer;

/**
 * Created by etsai on 8/21/2015.
 */
public class GpioFragment extends SingleDataSensorFragment {
    private static final String STREAM_KEY= "gpio_stream";
    private static final byte READ_ADC= 0, READ_ABS_REF= 1, READ_DIGITAL= 2, DEFAULT_GPIO_PIN= 0;
    private static final int GPIO_SAMPLE_PERIOD= 33;
    private static final int[] CONTROL_RES_IDS= {
            R.id.sample_control,
            R.id.gpio_digital_up, R.id.gpio_digital_down, R.id.gpio_digital_none,
            R.id.gpio_output_set, R.id.gpio_output_clear
    };

    private byte gpioPin= DEFAULT_GPIO_PIN;
    private int readMode= 0;
    private Gpio gpioModule;
    private Timer timerModule;
    private long startTime= -1;

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
        super(R.string.navigation_fragment_gpio, "adc", R.layout.fragment_gpio, 0, 1023);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner accRangeSelection= (Spinner) view.findViewById(R.id.gpio_read_mode);
        accRangeSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                readMode = position;
                YAxis leftAxis = chart.getAxisLeft();

                switch (readMode) {
                    case READ_ADC:
                        max = 1023;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "adc";
                        break;
                    case READ_ABS_REF:
                        max = 3000;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "abs reference";
                        break;
                    case READ_DIGITAL:
                        max = 1;
                        leftAxis.setAxisMaxValue(max);
                        csvHeaderDataName = "digital";
                        break;
                }

                leftAxis.setAxisMaxValue(max);
                refreshChart(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> spinnerAdapter= ArrayAdapter.createFromResource(getContext(), R.array.values_gpio_read_mode, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accRangeSelection.setAdapter(spinnerAdapter);
        accRangeSelection.setSelection(readMode);

        EditText gpioPinText= (EditText) view.findViewById(R.id.gpio_pin_value);
        gpioPinText.setText(String.format("%d", gpioPin));
        gpioPinText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final TextInputLayout gpioTextWrapper = (TextInputLayout) view.findViewById(R.id.gpio_pin_wrapper);

                try {
                    gpioPin = Byte.valueOf(s.toString());
                    gpioTextWrapper.setError(null);
                    for (int id : CONTROL_RES_IDS) {
                        view.findViewById(id).setEnabled(true);
                    }
                } catch (Exception e) {
                    gpioTextWrapper.setError(e.getLocalizedMessage());
                    for (int id : CONTROL_RES_IDS) {
                        view.findViewById(id).setEnabled(false);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.gpio_digital_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.setPinPullMode(gpioPin, PullMode.PULL_UP);
            }
        });
        view.findViewById(R.id.gpio_digital_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.setPinPullMode(gpioPin, PullMode.PULL_DOWN);
            }
        });
        view.findViewById(R.id.gpio_digital_none).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.setPinPullMode(gpioPin, PullMode.NO_PULL);
            }
        });

        Button setDoBtn= (Button) view.findViewById(R.id.gpio_output_set);
        setDoBtn.setText(R.string.value_gpio_output_set);
        setDoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.setDigitalOut(gpioPin);
            }
        });

        Button clearDoBtn= (Button) view.findViewById(R.id.gpio_output_clear);
        clearDoBtn.setText(R.string.value_gpio_output_clear);
        clearDoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioModule.clearDigitalOut(gpioPin);
            }
        });
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        gpioModule= mwBoard.getModule(Gpio.class);
        timerModule= mwBoard.getModule(Timer.class);
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {
        adapter.add(new HelpOption(R.string.config_name_gpio_pin, R.string.config_desc_gpio_pin));
        adapter.add(new HelpOption(R.string.config_name_gpio_read_mode, R.string.config_desc_gpio_read_mode));
        adapter.add(new HelpOption(R.string.config_name_output_control, R.string.config_desc_output_control));
        adapter.add(new HelpOption(R.string.config_name_pull_mode, R.string.config_desc_pull_mode));
    }

    @Override
    protected void setup() {
        switch(readMode) {
            case READ_ADC:
                gpioModule.routeData().fromAnalogIn(gpioPin, AnalogReadMode.ADC).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
            case READ_ABS_REF:
                gpioModule.routeData().fromAnalogIn(gpioPin, AnalogReadMode.ABS_REFERENCE).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
            case READ_DIGITAL:
                gpioModule.routeData().fromDigitalIn(gpioPin).stream(STREAM_KEY).commit()
                        .onComplete(GpioStreamSetup);
                break;
        }
        filenameExtraString= String.format("%s_pin_%d", csvHeaderDataName, gpioPin);
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
