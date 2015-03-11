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
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.GPIO;
import com.mbientlab.metawear.api.controller.GPIO.AnalogMode;
import com.mbientlab.metawear.api.controller.GPIO.PullMode;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * @author etsai
 *
 */
public class GPIOFragment extends ModuleFragment {
    private EditText pinText;
    private GPIO gpioController;
    private PullMode pullMode;
    private boolean olderFirmware;
    
    private ModuleCallbacks mCallbacks= new GPIO.Callbacks() {
        @Override
        public void receivedAnalogInputAsAbsValue(short value) {
            if (olderFirmware && isVisible()) {
                ((TextView) getView().findViewById(R.id.textView4)).setText(String.format(Locale.US, "%d mV", value));
            }
        }

        @Override
        public void receivedAnalogInputAsSupplyRatio(short value) {
            if (olderFirmware && isVisible()) {
                ((TextView) getView().findViewById(R.id.textView5)).setText(String.format(Locale.US, "%d", value));
            }
        }

        @Override
        public void receivedDigitalInput(byte value) {
            if (olderFirmware && isVisible()) {
                ((TextView) getView().findViewById(R.id.textView8)).setText(String.format(Locale.US, "%d", value));
            }
        }
        
        @Override
        public void receivedAnalogInputAsAbsValue(byte pin, short value) {
            if (!olderFirmware && isVisible()) {
                ((TextView) getView().findViewById(R.id.textView4)).setText(String.format(Locale.US, "%d mV", value));
            }
        }

        @Override
        public void receivedAnalogInputAsSupplyRatio(byte pin, short value) {
            if (!olderFirmware && isVisible()) {
                ((TextView) getView().findViewById(R.id.textView5)).setText(String.format(Locale.US, "%d", value));
            }
        }

        @Override
        public void receivedDigitalInput(byte pin, byte value) {
            if (!olderFirmware && isVisible()) {
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
        Spinner pullModeSpinner= (Spinner) view.findViewById(R.id.spinner2);
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
        
        ((CheckBox) view.findViewById(R.id.checkBox1)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                olderFirmware= isChecked;
            }
        });
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        byte gpioPin= Byte.valueOf(pinText.getEditableText().toString());
                        
                        gpioController.readAnalogInput(gpioPin, AnalogMode.ABSOLUTE_VALUE);
                        gpioController.readAnalogInput(gpioPin, AnalogMode.SUPPLY_RATIO);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        byte gpioPin= Byte.valueOf(pinText.getEditableText().toString());
                    
                        gpioController.setDigitalInput(gpioPin, pullMode);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        byte gpioPin= Byte.valueOf(pinText.getEditableText().toString());
                        
                        gpioController.readDigitalInput(gpioPin);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button4)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        byte gpioPin= Byte.valueOf(pinText.getEditableText().toString());
                        
                        gpioController.setDigitalOutput(gpioPin);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button5)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        byte gpioPin= Byte.valueOf(pinText.getEditableText().toString());
                        
                        gpioController.clearDigitalOutput(gpioPin);
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        pinText= ((EditText) view.findViewById(R.id.editText1));
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        gpioController= (GPIO) mwController.getModuleController(Module.GPIO);
        mwController.addModuleCallback(mCallbacks);
    }
    
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeModuleCallback(mCallbacks);
        }
        
        super.onDestroy();
    }
}
