/*
 * Copyright 2014 MbientLab Inc. All rights reserved.
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

import java.util.Locale;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Gpio;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author etsai
 *
 */
public class GPIOFragment extends ModuleFragment {
    private Gpio gpioController;
    private Gpio.PullMode pullMode;
    private byte gpioPin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gpio, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Spinner pullModeSpinner= (Spinner) view.findViewById(R.id.spinner2);
        pullModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                pullMode = Gpio.PullMode.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        pullModeSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, Gpio.PullMode.values()));
        
        view.findViewById(R.id.button1).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    gpioController.readAnalogIn(gpioPin, Gpio.AnalogReadMode.ABS_REFERENCE);
                    gpioController.readAnalogIn(gpioPin, Gpio.AnalogReadMode.ADC);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button2).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    gpioController.setPinPullMode(gpioPin, pullMode);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button3).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    gpioController.readDigitalIn(gpioPin);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button4).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    gpioController.setDigitalOut(gpioPin);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button5).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    gpioController.clearDigitalOut(gpioPin);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });

        gpioPin= 0;
        ((EditText) view.findViewById(R.id.editText1)).setText("0");
        ((EditText) view.findViewById(R.id.editText1)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                    case EditorInfo.IME_ACTION_NEXT:
                        try {
                            gpioPin = Byte.valueOf(v.getEditableText().toString());
                            addRoute(mwMnger.getCurrentController());
                        } catch (NumberFormatException ex) {
                            Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                return false;
            }
        });
    }

    private RouteManager.MessageHandler analogAbsHandler= new RouteManager.MessageHandler() {
        @Override
        public void process(Message message) {
            ((TextView) getView().findViewById(R.id.textView4)).setText(String.format(Locale.US, "%d mV", message.getData(Short.class)));
        }
    }, analogAdcHandler= new RouteManager.MessageHandler() {
        @Override
        public void process(Message message) {
            ((TextView) getView().findViewById(R.id.textView5)).setText(String.format(Locale.US, "%d", message.getData(Short.class)));
        }
    }, digitalHandler= new RouteManager.MessageHandler() {
        @Override
        public void process(Message message) {
            ((TextView) getView().findViewById(R.id.textView8)).setText(String.format(Locale.US, "%d", message.getData(Byte.class)));
        }
    };
    private void addRoute(final MetaWearBoard mwController) {
        mwController.removeRoutes();

        gpioController.routeData().fromAnalogGpio(gpioPin, Gpio.AnalogReadMode.ABS_REFERENCE).stream("analog_abs")
                .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("analog_abs", analogAbsHandler);
                    }
                });
        gpioController.routeData().fromAnalogGpio(gpioPin, Gpio.AnalogReadMode.ADC).stream("analog_adc")
                .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("analog_adc", analogAdcHandler);
                    }
                });
        gpioController.routeData().fromDigitalIn(gpioPin).stream("digital_in").commit()
                .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                    @Override
                    public void success(RouteManager result) {
                        result.subscribe("digital_in", digitalHandler);
                    }
                });
    }

    private MetaWearBoard currBoard;

    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
        currBoard.removeRoutes();

        try {
            gpioController= currBoard.getModule(Gpio.class);
            addRoute(currBoard);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void disconnected() {

    }
}
