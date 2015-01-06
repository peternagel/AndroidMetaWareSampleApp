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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.GATT.GATTCharacteristic;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.characteristic.Battery;
import com.mbientlab.metawear.api.characteristic.DeviceInformation;
import com.mbientlab.metawear.api.controller.Debug;
import com.mbientlab.metawear.api.controller.MechanicalSwitch;
import com.mbientlab.metawear.api.controller.Temperature;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageButton;

/**
 * @author etsai
 *
 */
public class DeviceInfoFragment extends ModuleFragment {
    private Callbacks aCallbacks;
    public interface Callbacks {
        public void startDfu();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        aCallbacks= (Callbacks) activity;
    }
    
    private MechanicalSwitch switchController;
    private Temperature tempController;
    private Debug debugController;
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            mwMnger.getCurrentController().readDeviceInformation();
            switchController.enableNotification();
        }
        
        @Override
        public void receivedGATTCharacteristic(
                GATTCharacteristic characteristic, byte[] data) {
            if (characteristic == Battery.BATTERY_LEVEL) {
                values.put(characteristic, String.format(Locale.US, "%s", data[0]));
            } else {
                values.put(characteristic, new String(data));
            }
            final Integer viewId= views.get(characteristic);
            if (viewId != null && isVisible()) {
                ((TextView) getView().findViewById(viewId)).setText(values.get(characteristic));
            }
        }
        
        @Override
        public void receivedRemoteRSSI(int rssi) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.remote_rssi)).setText(String.format(Locale.US, "%d ", rssi));
            }
        }
    };
    private ModuleCallbacks mCallback= new MechanicalSwitch.Callbacks() {
        @Override
        public void pressed() {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.mechanical_switch)).setText("Pressed");
            }
        }

        @Override
        public void released() {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.mechanical_switch)).setText("Released");
            }
        }
    };
    private ModuleCallbacks tempCallback= new Temperature.Callbacks() {
        @Override
        public void receivedTemperature(float degrees) {
            if (isVisible()) {
                ((TextView) getView().findViewById(R.id.temperature)).setText(String.format(Locale.US, "%1$.2f C", degrees));
            }
        }
    };
    
    private HashMap<GATTCharacteristic, String> values= new HashMap<>();
    private final static HashMap<GATTCharacteristic, Integer> views= new HashMap<>();
    static {
        views.put(DeviceInformation.MANUFACTURER_NAME, R.id.manufacturer_name);
        views.put(DeviceInformation.SERIAL_NUMBER, R.id.serial_number);
        views.put(DeviceInformation.FIRMWARE_VERSION, R.id.firmware_version);
        views.put(DeviceInformation.HARDWARE_VERSION, R.id.hardware_version);
        views.put(Battery.BATTERY_LEVEL, R.id.battery_level);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_device_info, container, false);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            values= (HashMap<GATTCharacteristic, String>) savedInstanceState.getSerializable("STATE_VALUES");
        }
        for(Entry<GATTCharacteristic, String> it: values.entrySet()) {
            Integer viewId= views.get((GATTCharacteristic) it.getKey());
            if (viewId != null) {
                ((TextView) view.findViewById(viewId)).setText(it.getValue());
            }
        }
        ((ImageButton) view.findViewById(R.id.imageButton1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                aCallbacks.startDfu();
            }
        });
        
        ((TextView) view.findViewById(R.id.temperature_action)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempController.readTemperature();
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugController.resetDevice();
            }
        });
        ((TextView) view.findViewById(R.id.textView1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mwMnger.getCurrentController().readBatteryLevel();
            }
        });
        ((TextView) view.findViewById(R.id.textView2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                mwMnger.getCurrentController().readRemoteRSSI();
            }
        });
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeDeviceCallback(dCallback);
            mwController.removeModuleCallback(mCallback);
            mwController.removeModuleCallback(tempCallback);
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("STATE_VALUES", values);
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        switchController= (MechanicalSwitch) mwController.getModuleController(Module.MECHANICAL_SWITCH);
        tempController= (Temperature) mwController.getModuleController(Module.TEMPERATURE);
        debugController= (Debug) mwController.getModuleController(Module.DEBUG);
        
        mwController.addDeviceCallback(dCallback);
        mwController.addModuleCallback(mCallback).addModuleCallback(tempCallback);
        
        if (mwController.isConnected()) {
            mwController.readDeviceInformation();
            switchController.enableNotification();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ble_disconnect:
            for(Entry<GATTCharacteristic, Integer> it: views.entrySet()) {
                values.remove(it.getKey());
                ((TextView) getView().findViewById(it.getValue())).setText("");
            }
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
