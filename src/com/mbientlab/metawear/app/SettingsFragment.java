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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.Macro;
import com.mbientlab.metawear.api.controller.Settings;

/**
 * @author etsai
 *
 */
public class SettingsFragment extends ModuleFragment {
    private final String KEY_DEVICE_NAME= "DEVICE_NAME", KEY_AD_INTERVAL= "AD_INTERVAL", 
            KEY_AD_TIMEOUT= "AD_TIMEOUT", KEY_TX_POWER= "TX_POWER";
    
    private final Object lock= new Object();
    private boolean macroReady, readSettings= true;
    
    private Macro macroController;
    private Settings settingsController;
    private EditText deviceName, adInterval, adTimeout, txPower;
    
    private final DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            settingsController.readDeviceName();
            settingsController.readAdvertisingParams();
            settingsController.readTxPower();
        }
        
        @Override
        public void disconnected() {
            deviceName.setText("");
            adInterval.setText("");
            adTimeout.setText("");
            txPower.setText("");
        }
    };
    private final Settings.Callbacks settingsCallbackFns= new Settings.Callbacks() {
        @Override
        public void receivedDeviceName(String name) {
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText1)).setText(name);
            }
        }

        @Override
        public void receivedAdvertisementParams(int interval, short timeout) {
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText2)).setText(String.format("%d", interval));
                ((EditText) getView().findViewById(R.id.editText4)).setText(String.format("%d", timeout));
            }
        }

        @Override
        public void receivedTXPower(byte power) {
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText3)).setText(String.format("%d", power));
            }
        }
    };
    private final Macro.Callbacks macroCallbackFns= new Macro.Callbacks() {
        @Override
        public void receivedMacroId(byte id) {
            try {
                synchronized(lock) {
                    while(!macroReady) {
                        lock.wait();
                    }
                    macroController.executeMacro(id);
                }
            } catch (final InterruptedException e) {
                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        deviceName= (EditText) view.findViewById(R.id.editText1);
        adInterval= (EditText) view.findViewById(R.id.editText2);
        adTimeout= (EditText) view.findViewById(R.id.editText4);
        txPower= (EditText) view.findViewById(R.id.editText3);
        
        if (savedInstanceState != null) {
            readSettings= false;
            deviceName.setText(savedInstanceState.getString(KEY_DEVICE_NAME));
            adInterval.setText(savedInstanceState.getString(KEY_AD_INTERVAL));
            adTimeout.setText(savedInstanceState.getString(KEY_AD_TIMEOUT));
            txPower.setText(savedInstanceState.getString(KEY_TX_POWER));
        }
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        int interval= Integer.valueOf(adInterval.getEditableText().toString());
                        short timeout= Short.valueOf(adTimeout.getEditableText().toString());
                
                        macroReady= false;
                        macroController.recordMacro(true);
                        settingsController.setDeviceName(deviceName.getEditableText().toString());
                        settingsController.setAdvertisingInterval((short) (interval & 0xffff), (byte) (timeout & 0xff));
                        settingsController.setTXPower(Byte.valueOf(txPower.getEditableText().toString()));
                        macroController.stopRecord();
                        macroReady= true;
                    
                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    macroController.eraseMacros();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeDeviceCallback(dCallback);
            mwController.removeModuleCallback(settingsCallbackFns);
            mwController.removeModuleCallback(macroCallbackFns);
        }
        
        super.onDestroy();
    }
    
    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.ModuleFragment#controllerReady(com.mbientlab.metawear.api.MetaWearController)
     */
    @Override
    public void controllerReady(MetaWearController mwController) {
        settingsController= (Settings) mwController.getModuleController(Module.SETTINGS);
        macroController= (Macro) mwController.getModuleController(Module.MACRO);
        mwController.addDeviceCallback(dCallback).addModuleCallback(settingsCallbackFns).addModuleCallback(macroCallbackFns);
        
        if (readSettings && mwController.isConnected()) {
            dCallback.connected();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putString(KEY_DEVICE_NAME, deviceName.getEditableText().toString());
        outState.putString(KEY_AD_INTERVAL, adInterval.getEditableText().toString());
        outState.putString(KEY_AD_TIMEOUT, adTimeout.getEditableText().toString());
        outState.putString(KEY_TX_POWER, txPower.getEditableText().toString());
    }
}
