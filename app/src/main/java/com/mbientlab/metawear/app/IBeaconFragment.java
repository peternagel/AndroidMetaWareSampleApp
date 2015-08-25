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
import android.widget.EditText;
import android.widget.ListView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.config.SensorConfigAdapter;
import com.mbientlab.metawear.app.config.SensorConfig;
import com.mbientlab.metawear.module.IBeacon;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by etsai on 8/22/2015.
 */
public class IBeaconFragment extends ModuleFragmentBase {
    final ArrayList<SensorConfig> configSettings= new ArrayList<>();

    private boolean isReady;
    private IBeacon ibeaconModule;
    private SensorConfigAdapter configAdapter;

    private UUID uuid;
    private short major, minor, period;
    private byte rxPower, txPower;

    public IBeaconFragment() {
        super("IBeacon");
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        isReady= true;
        ibeaconModule= mwBoard.getModule(IBeacon.class);

        addConfigOptions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        configAdapter= new SensorConfigAdapter(getActivity(), R.id.sensor_config_entry_layout);
        configAdapter.setNotifyOnChange(true);
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_ibeacon, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ListView) view.findViewById(R.id.ibeacon_settings)).setAdapter(configAdapter);

        view.findViewById(R.id.ibeacon_enable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ibeaconModule.configure()
                        .setUUID(uuid)
                        .setMajor(major)
                        .setMinor(minor)
                        .setAdPeriod(period)
                        .setRxPower(rxPower)
                        .setTxPower(txPower)
                        .commit();
                ibeaconModule.enable();
            }
        });
        view.findViewById(R.id.ibeacon_disable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ibeaconModule.disable();
            }
        });

        if (isReady) {
            addConfigOptions();
        }
    }

    private void addConfigOptions() {
        ibeaconModule.readConfiguration().onComplete(new AsyncOperation.CompletionHandler<IBeacon.Configuration>() {
            @Override
            public void success(IBeacon.Configuration result) {
                uuid = result.adUuid();
                major = result.major();
                minor = result.minor();
                rxPower = result.rxPower();
                txPower = result.txPower();
                period = result.adPeriod();

                configSettings.clear();
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_uuid, R.string.config_desc_ibeacon_uuid,
                        uuid, R.layout.popup_config_string) {

                    private EditText uuidText;

                    @Override
                    public void setup(View v) {
                        uuidText = (EditText) v.findViewById(R.id.config_value);
                        uuidText.setText(uuid.toString());
                    }

                    @Override
                    public void changeCommitted() {
                        uuid = UUID.fromString(uuidText.getText().toString());
                        value = uuid;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_major, R.string.config_desc_ibeacon_major,
                        major, R.layout.popup_gpio_pin_config) {

                    private EditText majorText;

                    @Override
                    public void setup(View v) {
                        majorText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        majorText.setText(String.format("%d", major));
                    }

                    @Override
                    public void changeCommitted() {
                        major = Short.valueOf(majorText.getText().toString());
                        value = major;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_minor, R.string.config_desc_ibeacon_minor,
                        minor, R.layout.popup_gpio_pin_config) {

                    private EditText minorText;

                    @Override
                    public void setup(View v) {
                        minorText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        minorText.setText(String.format("%d", minor));
                    }

                    @Override
                    public void changeCommitted() {
                        minor = Short.valueOf(minorText.getText().toString());
                        value = minor;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_rx, R.string.config_desc_ibeacon_rx,
                        rxPower, R.layout.popup_gpio_pin_config) {
                    private EditText rxPowerText;

                    @Override
                    public void setup(View v) {
                        rxPowerText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        rxPowerText.setText(String.format("%d", rxPower));
                    }

                    @Override
                    public void changeCommitted() {
                        rxPower = Byte.valueOf(rxPowerText.getText().toString());
                        value = rxPower;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_tx, R.string.config_desc_ibeacon_tx,
                        txPower, R.layout.popup_gpio_pin_config) {
                    private EditText txPowerText;

                    @Override
                    public void setup(View v) {
                        txPowerText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        txPowerText.setText(String.format("%d", txPower));
                    }

                    @Override
                    public void changeCommitted() {
                        txPower = Byte.valueOf(txPowerText.getText().toString());
                        value = txPower;
                    }
                });
                configSettings.add(new SensorConfig(R.string.config_name_ibeacon_period, R.string.config_desc_ibeacon_period,
                        period, R.layout.popup_gpio_pin_config) {

                    private EditText periodText;

                    @Override
                    public void setup(View v) {
                        periodText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                        periodText.setText(String.format("%d", period));
                    }

                    @Override
                    public void changeCommitted() {
                        period = Short.valueOf(periodText.getText().toString());
                        value = period;
                    }
                });

                configAdapter.addAll(configSettings);
            }
        });
    }
}
