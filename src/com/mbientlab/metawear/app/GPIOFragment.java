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
 * PROVIDED “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, 
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

import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.GPIO;
import com.mbientlab.metawear.api.controller.GPIO.AnalogMode;
import com.mbientlab.metawear.api.controller.GPIO.PullMode;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author etsai
 *
 */
public class GPIOFragment extends ModuleFragment {
    private byte pin;
    private GPIO gpioController;
    private PullMode pullMode;
    
    private ModuleCallbacks mCallbacks= new GPIO.Callbacks() {
        @Override
        public void receivedAnalogInputAsAbsValue(short value) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.textView6)).setText(String.format(Locale.US, "%d mV", value));
            }
        }

        @Override
        public void receivedAnalogInputAsSupplyRatio(short value) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.textView7)).setText(String.format(Locale.US, "%d", value));
            }
        }

        @Override
        public void receivedDigitalInput(byte value) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.textView8)).setText(String.format(Locale.US, "%d", value));
            }
        }
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gpio, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Spinner pullModeSpinner= (Spinner) view.findViewById(R.id.spinner1);
        pullModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                pullMode= PullMode.values[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        pullModeSpinner.setAdapter(new ArrayAdapter<PullMode>(getActivity(), 
                R.layout.command_row, R.id.command_name, PullMode.values));
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.setDigitalOutput(pin);
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.clearDigitalOutput(pin);
            }
        });
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.setDigitalInput(pin, pullMode);
            }
        });
        ((Button) view.findViewById(R.id.button4)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.readAnalogInput(pin, AnalogMode.ABSOLUTE_VALUE);
            }
        });
        ((Button) view.findViewById(R.id.button5)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.readAnalogInput(pin, AnalogMode.SUPPLY_RATIO);
            }
        });
        ((Button) view.findViewById(R.id.button6)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpioController.readDigitalInput(pin);
            }
        });
        
        EditText pinText= ((EditText) view.findViewById(R.id.editText1));
        pinText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    pin= Byte.valueOf(((EditText) v).getEditableText().toString());
                }
            }
        });
        
        pinText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                switch (actionId) {
                case EditorInfo.IME_ACTION_DONE:
                case EditorInfo.IME_ACTION_NEXT:
                    pin= Byte.valueOf(((EditText) v).getEditableText().toString());
                    break;
                }
                return false;
            }
        });
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        gpioController= (GPIO) this.mwController.getModuleController(Module.GPIO);
        this.mwController.addModuleCallback(mCallbacks);
    }
    
    @Override
    public void onDestroy() {
        mwController.removeModuleCallback(mCallbacks);
        super.onDestroy();
    }
}
