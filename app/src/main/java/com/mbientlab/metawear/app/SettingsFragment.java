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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.config.SensorConfig;
import com.mbientlab.metawear.app.config.SensorConfigAdapter;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Macro;
import com.mbientlab.metawear.module.Settings;

import java.util.ArrayList;

/**
 * Created by etsai on 8/22/2015.
 */
public class SettingsFragment extends ModuleFragmentBase {
    final ArrayList<SensorConfig> configSettings= new ArrayList<>();
    private SensorConfigAdapter configAdapter;
    private Settings settingsModule;
    private Macro macroModule;
    private Debug debugModule;
    private boolean isReady= false;

    private String deviceName;
    private short timeout;
    private byte txPower;
    private int txPowerIndex= 0, adInterval;

    public SettingsFragment() {
        super("Settings");
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        isReady= true;

        debugModule= mwBoard.getModule(Debug.class);
        settingsModule= mwBoard.getModule(Settings.class);
        macroModule= mwBoard.getModule(Macro.class);

        addConfigOptions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        configAdapter= new SensorConfigAdapter(getActivity(), R.id.sensor_config_entry_layout);
        configAdapter.setNotifyOnChange(true);
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ListView) view.findViewById(R.id.ad_setting_parameters)).setAdapter(configAdapter);

        view.findViewById(R.id.settings_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                macroModule.record(new Macro.CodeBlock() {
                    @Override
                    public void commands() {
                        settingsModule.configure()
                                .setDeviceName(deviceName)
                                .setAdInterval((short) (adInterval & 0xffff), (byte) (timeout & 0xff))
                                .setTxPower(txPower)
                                .commit();
                    }
                });
            }
        });
        view.findViewById(R.id.settings_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                macroModule.eraseMacros();
                debugModule.resetAfterGarbageCollect();
            }
        });

        if (isReady) {
            addConfigOptions();
        }
    }

    private void addConfigOptions() {
        settingsModule.readAdConfig().onComplete(new AsyncOperation.CompletionHandler<Settings.AdvertisementConfig>() {
            @Override
            public void success(Settings.AdvertisementConfig result) {
                deviceName= result.deviceName();
                adInterval= result.interval();
                timeout= result.timeout();
                txPower= result.txPower();

                configSettings.clear();
                configSettings.add(new SensorConfig(R.string.config_name_ad_device_name, R.string.config_desc_ad_device_name,
                        deviceName, R.layout.popup_config_string) {

                    private EditText deviceNameText;

                    @Override
                    public void setup(View v) {
                        deviceNameText = (EditText) v.findViewById(R.id.config_value);
                        deviceNameText.setText(deviceName);
                    }

                    @Override
                    public void changeCommitted() {
                        deviceName = deviceNameText.getText().toString();
                        value= deviceName;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ad_interval, R.string.config_desc_ad_interval,
                        adInterval, R.layout.popup_gpio_pin_config) {

                    private EditText adIntervalText;

                    @Override
                    public void setup(View v) {
                        adIntervalText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        adIntervalText.setText(String.format("%d", adInterval));
                    }

                    @Override
                    public void changeCommitted() {
                        adInterval = Short.valueOf(adIntervalText.getText().toString());
                        value= adIntervalText;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ad_timeout, R.string.config_desc_ad_timeout,
                        timeout, R.layout.popup_gpio_pin_config) {

                    private EditText adTimeoutText;

                    @Override
                    public void setup(View v) {
                        adTimeoutText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        adTimeoutText.setText(String.format("%d", timeout));
                    }

                    @Override
                    public void changeCommitted() {
                        timeout = Byte.valueOf(adTimeoutText.getText().toString());
                        value= timeout;
                    }
                });
                final String[] txPowerStringValues= getActivity().getResources().getStringArray(R.array.values_ad_settings_tx_power);
                configSettings.add(new SensorConfig(R.string.config_name_ad_tx, R.string.config_desc_ad_tx,
                        txPower, R.layout.popup_config_spinner) {

                    private Spinner txPowerValues;

                    @Override
                    public void setup(View v) {
                        txPowerValues = (Spinner) v.findViewById(R.id.config_value_list);
                        final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                                R.array.values_ad_settings_tx_power, android.R.layout.simple_spinner_item);
                        readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        txPowerValues.setAdapter(readModeAdapter);
                        txPowerValues.setSelection(txPowerIndex);
                    }

                    @Override
                    public void changeCommitted() {
                        txPowerIndex = txPowerValues.getSelectedItemPosition();
                        txPower = Byte.valueOf(txPowerStringValues[txPowerIndex]);
                        value= txPower;
                    }
                });

                configAdapter.addAll(configSettings);
            }
        });
    }
}
