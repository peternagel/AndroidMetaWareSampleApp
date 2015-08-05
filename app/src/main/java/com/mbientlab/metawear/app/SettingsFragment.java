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
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.Settings;

/**
 * @author etsai
 *
 */
public class SettingsFragment extends ModuleFragment {
    private final String KEY_DEVICE_NAME= "DEVICE_NAME", KEY_AD_INTERVAL= "AD_INTERVAL", 
            KEY_AD_TIMEOUT= "AD_TIMEOUT", KEY_TX_POWER= "TX_POWER";
    
    private Macro macroController;
    private Settings settingsController;
    private EditText deviceName, adInterval, adTimeout, txPower;
    private MetaWearBoard currBoard;

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
            deviceName.setText(savedInstanceState.getString(KEY_DEVICE_NAME));
            adInterval.setText(savedInstanceState.getString(KEY_AD_INTERVAL));
            adTimeout.setText(savedInstanceState.getString(KEY_AD_TIMEOUT));
            txPower.setText(savedInstanceState.getString(KEY_TX_POWER));
        }
        
        view.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        final int interval = Integer.valueOf(adInterval.getEditableText().toString());
                        final short timeout = Short.valueOf(adTimeout.getEditableText().toString());

                        macroController.record(new Macro.CodeBlock() {
                            @Override
                            public void commands() {
                                settingsController.configure().setDeviceName(deviceName.getEditableText().toString())
                                        .setAdInterval((short) (interval & 0xffff), (byte) (timeout & 0xff))
                                        .setTxPower(Byte.valueOf(txPower.getEditableText().toString()))
                                        .commit();
                            }
                        });
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    macroController.eraseMacros();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putString(KEY_DEVICE_NAME, deviceName.getEditableText().toString());
        outState.putString(KEY_AD_INTERVAL, adInterval.getEditableText().toString());
        outState.putString(KEY_AD_TIMEOUT, adTimeout.getEditableText().toString());
        outState.putString(KEY_TX_POWER, txPower.getEditableText().toString());
    }

    private void readSettingsConfig() {
        settingsController.readAdConfig().onComplete(new AsyncOperation.CompletionHandler<Settings.AdvertisementConfig>() {
            @Override
            public void success(Settings.AdvertisementConfig result) {
                if (isVisible()) {
                    deviceName.setText(result.deviceName());
                    adInterval.setText(String.format("%d", result.interval()));
                    adTimeout.setText(String.format("%d", result.timeout()));
                    txPower.setText(String.format("%d", result.txPower()));
                }
            }
        });
    }
    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
        try {
            settingsController= currBoard.getModule(Settings.class);
            macroController= currBoard.getModule(Macro.class);
            readSettingsConfig();
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void disconnected() {

    }
}
