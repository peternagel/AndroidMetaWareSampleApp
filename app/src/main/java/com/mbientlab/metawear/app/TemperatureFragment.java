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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.MultiChannelTemperature;
import com.mbientlab.metawear.module.SingleChannelTemperature;
import com.mbientlab.metawear.module.Temperature;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * @author etsai
 *
 */
public class TemperatureFragment extends ModuleFragment {
    private static final AlphaAnimation fadeOut= new AlphaAnimation(1.0f , 0.0f);
    static {
        fadeOut.setDuration(5000);
        fadeOut.setFillAfter(true);
    }

    private MetaWearBoard currBoard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_temperature, container, false);
    }

    private final RouteManager.MessageHandler tempHandler= new RouteManager.MessageHandler() {
        @Override
        public void process(Message message) {
            ((TextView) getView().findViewById(R.id.textView2)).setText(String.format(Locale.US, "%1$.3f C",
                    message.getData(Float.class)));
        }
    };

    private EditText analogPin, pulldownPin;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        view.findViewById(R.id.textView1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        Temperature tempController= currBoard.getModule(Temperature.class);
                        if (tempController instanceof SingleChannelTemperature) {
                            tempController.readTemperature();
                        } else {
                            MultiChannelTemperature multiTempModule= ((MultiChannelTemperature) tempController);
                            multiTempModule.readTemperature(multiTempModule.getSources().get(0));
                            multiTempModule.readTemperature(multiTempModule.getSources().get(1));
                        }
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });

        analogPin= (EditText) view.findViewById(R.id.editText5);
        pulldownPin= (EditText) view.findViewById(R.id.editText6);
        
        view.findViewById(R.id.button3).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        Temperature tempController= currBoard.getModule(Temperature.class);
                        if (tempController instanceof SingleChannelTemperature) {
                            ((SingleChannelTemperature) tempController).enableThermistorMode(Byte.valueOf(analogPin.getEditableText().toString()),
                                    Byte.valueOf(pulldownPin.getEditableText().toString()));
                        } else {
                            MultiChannelTemperature multiTempModule= ((MultiChannelTemperature) tempController);
                            ((MultiChannelTemperature.ExtThermistor) multiTempModule.getSources().get(1)).configure(
                                    Byte.valueOf(analogPin.getEditableText().toString()),
                                    Byte.valueOf(pulldownPin.getEditableText().toString()),
                                    false);
                            currBoard.removeRoutes();
                            addThermistorRoute(tempController);
                        }
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), R.string.error_thermistor_pins, Toast.LENGTH_SHORT).show();
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        Temperature tempController = currBoard.getModule(Temperature.class);
                        if (tempController instanceof SingleChannelTemperature) {
                            ((SingleChannelTemperature) tempController).disableThermistorMode();
                        }

                        currBoard.removeRoutes();
                        addRoute(tempController);
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addRoute(final Temperature tempController) {
        tempController.routeData().fromSensor().stream("temperature").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                result.subscribe("temperature", tempHandler);
            }
        });
    }

    private void addThermistorRoute(final Temperature tempController) {
        if (tempController instanceof MultiChannelTemperature) {
            MultiChannelTemperature multiTemp= (MultiChannelTemperature) tempController;
            multiTemp.routeData().fromSource(multiTemp.getSources().get(1)).stream("ext_thermistor_temp").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("ext_thermistor_temp", tempHandler);
                }
            });
        } else {
            addRoute(tempController);
        }
    }

    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
        try {
            Temperature tempController= currBoard.getModule(Temperature.class);
            addRoute(tempController);
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void disconnected() {

    }
}
