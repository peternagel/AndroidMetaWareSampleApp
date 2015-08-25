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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.MultiChannelTemperature;
import com.mbientlab.metawear.module.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by etsai on 8/19/2015.
 */
public class TemperatureFragment extends SingleDataSensorFragment {
    private static final int TEMP_SAMPLE_PERIOD= 500;
    private static final String STREAM_KEY= "temp_stream";

    private byte gpioDataPin= 0, gpioPulldownPin= 1;
    private boolean activeHigh= true;

    private long startTime= -1;
    private MultiChannelTemperature multiChnlTempModule;
    private Timer timerModule;
    private List<MultiChannelTemperature.Source> availableSources= null;
    private List<String> spinnerEntries;
    private int selectedSourceIndex= 0;

    private TextView configValueText;

    public TemperatureFragment() {
        super("MultiChannel Temp", "celsius", R.layout.fragment_temperature, 15, 45);
        chartDescription= "Ambient temperature (\u00B0C) vs. Time";

    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.config_name)).setText(R.string.config_name_temp_source);

        configValueText= ((TextView) view.findViewById(R.id.config_value));
        if (availableSources != null) {
            configValueText.setText(spinnerEntries.get(selectedSourceIndex));
        }

        view.findViewById(R.id.edit_config_value).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.popup_multi_chnl_temp_config, fragContainer, false);
                final Spinner configValue = (Spinner) dialogLayout.findViewById(R.id.temp_source_selector),
                        extActiveText= (Spinner) dialogLayout.findViewById(R.id.spinner);
                final EditText gpioDataPinText= (EditText) dialogLayout.findViewById(R.id.editText),
                        gpioPulldownPinText= (EditText) dialogLayout.findViewById(R.id.editText2);

                gpioDataPinText.setText(String.format("%d", gpioDataPin));
                gpioPulldownPinText.setText(String.format("%d", gpioPulldownPin));
                extActiveText.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.simple_list_entry, R.id.list_entry_name, new String[]{"True", "False"}));
                extActiveText.setSelection(activeHigh ? 0 : 1);

                configValue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        int[] extThermResIds= new int[] {R.id.ext_thermistor_data_pin, R.id.editText,
                                R.id.ext_thermistor_pulldown_pin, R.id.editText2,
                                R.id.ext_thermistor_active, R.id.spinner
                        };

                        for(int resId: extThermResIds) {
                            if (availableSources.get(i) instanceof MultiChannelTemperature.ExtThermistor) {
                                dialogLayout.findViewById(resId).setVisibility(View.VISIBLE);
                            } else {
                                dialogLayout.findViewById(resId).setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                configValue.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.simple_list_entry, R.id.list_entry_name, spinnerEntries));
                configValue.setSelection(selectedSourceIndex);

                ((TextView) dialogLayout.findViewById(R.id.config_description)).setText(R.string.config_desc_temp_source);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.config_name_temp_source)
                        .setPositiveButton(R.string.label_commit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                selectedSourceIndex = configValue.getSelectedItemPosition();
                                configValueText.setText(spinnerEntries.get(selectedSourceIndex));

                                if (availableSources.get(selectedSourceIndex) instanceof MultiChannelTemperature.ExtThermistor) {
                                    gpioDataPin= Byte.valueOf(gpioDataPinText.getText().toString());
                                    gpioPulldownPin= Byte.valueOf(gpioPulldownPinText.getText().toString());
                                    activeHigh= Boolean.valueOf((String) extActiveText.getSelectedItem());

                                    ((MultiChannelTemperature.ExtThermistor) availableSources.get(selectedSourceIndex))
                                            .configure(gpioDataPin, gpioPulldownPin, activeHigh);
                                }
                            }
                        })
                        .setNegativeButton(R.string.label_cancel, null);

                builder.setView(dialogLayout);
                builder.show();
            }
        });
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        timerModule= mwBoard.getModule(Timer.class);
        multiChnlTempModule= mwBoard.getModule(MultiChannelTemperature.class);
        availableSources= multiChnlTempModule.getSources();

        spinnerEntries= new ArrayList<>();
        for(byte i= 0; i < availableSources.size(); i++) {
            spinnerEntries.add(availableSources.get(i).getName());
        }

        configValueText.setText(spinnerEntries.get(selectedSourceIndex));
    }

    @Override
    protected void setup() {
        multiChnlTempModule.routeData().fromSource(availableSources.get(selectedSourceIndex)).stream(STREAM_KEY).commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        streamRouteManager= result;
                        result.subscribe(STREAM_KEY, new RouteManager.MessageHandler() {
                            @Override
                            public void process(Message message) {
                                final Float celsius = message.getData(Float.class);

                                LineData data = chart.getData();

                                if (startTime == -1) {
                                    data.addXValue("0");
                                    startTime= System.currentTimeMillis();
                                } else {
                                    data.addXValue(String.format("%.2f", (System.currentTimeMillis() - startTime) / 1000.f));
                                }

                                data.addEntry(new Entry(celsius, sampleCount), 0);

                                sampleCount++;
                            }
                        });
                    }
                });
        timerModule.scheduleTask(new Timer.Task() {
            @Override
            public void commands() {
                multiChnlTempModule.readTemperature(availableSources.get(selectedSourceIndex));
            }
        }, TEMP_SAMPLE_PERIOD, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
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
