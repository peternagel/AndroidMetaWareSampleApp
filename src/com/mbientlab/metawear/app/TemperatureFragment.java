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

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Temperature;
import com.mbientlab.metawear.api.controller.Temperature.SamplingConfig;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

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
    
    private Temperature.Callbacks mCallback= new Temperature.Callbacks() {

        @Override
        public void receivedTemperature(float degrees) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.textView2)).setText(String.format(Locale.US, "%1$.2f C", degrees));
            }
        }

        @Override
        public void receivedSamplingConfig(SamplingConfig config) {
            
        }

        @Override
        public void temperatureDeltaExceeded(float reference, float current) {
            if (isVisible()) {
                TextView deltaText= (TextView) getView().findViewById(R.id.textView4);
                deltaText.setText(String.format(Locale.US, "Reference= %.2f, Current= %.2f", 
                        reference, current));
                deltaText.startAnimation(fadeOut);
            }
        }

        @Override
        public void boundaryCrossed(float threshold, float current) {
            if (isVisible()) {
                TextView thsText= (TextView) getView().findViewById(R.id.textView6);
                thsText.setText(String.format(Locale.US, "Threshold= %.2f, Current= %.2f", 
                        threshold, current));
                thsText.startAnimation(fadeOut);
            }
        }
        
    };
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_temperature, container, false);
    }
    
    private EditText tempDelta, tempLower, tempUpper, tempPolling,
            analogPin, pulldownPin;
    private Temperature tempController;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ((TextView) view.findViewById(R.id.textView1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tempController.readTemperature();
            }
        });
        
        tempDelta= (EditText) view.findViewById(R.id.editText1);
        tempLower= (EditText) view.findViewById(R.id.editText2);
        tempUpper= (EditText) view.findViewById(R.id.editText3);
        tempPolling= (EditText) view.findViewById(R.id.editText4);
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tempController.enableSampling().withSampingPeriod(Integer.parseInt(tempPolling.getEditableText().toString()))
                    .withTemperatureDelta(Float.parseFloat(tempDelta.getEditableText().toString()))
                    .withTemperatureBoundary(Float.parseFloat(tempLower.getEditableText().toString()), 
                            Float.parseFloat(tempUpper.getEditableText().toString()))
                    .commit();
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tempController.disableSampling();
            }
        });
        
        analogPin= (EditText) view.findViewById(R.id.editText5);
        pulldownPin= (EditText) view.findViewById(R.id.editText6);
        
        ((ToggleButton) view.findViewById(R.id.toggleButton1)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    tempController.enableThermistorMode(Byte.valueOf(analogPin.getEditableText().toString()), 
                            Byte.valueOf(pulldownPin.getEditableText().toString()));
                } else {
                    tempController.disableThermistorMode();
                }
            }
        });
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        tempController= (Temperature) mwController.getModuleController(Module.TEMPERATURE);
        mwController.addModuleCallback(mCallback);
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeModuleCallback(mCallback);
        }
        
        super.onDestroy();
    }
}
