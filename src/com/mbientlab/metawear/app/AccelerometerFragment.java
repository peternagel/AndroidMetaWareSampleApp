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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.Component;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * @author etsai
 *
 */
public class AccelerometerFragment extends ModuleFragment {
    private Accelerometer accelController;
    private Accelerometer.Callbacks mCallback= new Accelerometer.Callbacks() {
        public void receivedDataValue(short x, short y, short z) {
            if (isVisible()) {
                String motion= String.format(Locale.US, "(%d, %d, %d)", x, y, z);
                ((TextView) getView().findViewById(R.id.motion_data)).setText(motion);
            }
        }
    };
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            for(final Entry<Integer, Component> it: switches.entrySet()) {
                if (!values.containsKey(it.getKey()) || !values.get(it.getKey())) {
                    accelController.disableNotification(switches.get(it.getKey()));
                } else {
                    accelController.enableNotification(switches.get(it.getKey()));
                }
            }
        }
    };
    private HashMap<Integer, Boolean> values= new HashMap<>();
    private static final HashMap<Integer, Component> switches= new HashMap<>();
    static {
        switches.put(R.id.switch1, Component.DATA);
        switches.put(R.id.switch2, Component.FREE_FALL);
        switches.put(R.id.switch3, Component.ORIENTATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accelerometer, container, false);
    }
    
    @Override
    public void onDestroy() {
        mwController.removeDeviceCallback(dCallback);
        mwController.removeModuleCallback(mCallback);
        super.onDestroy();
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        accelController= (Accelerometer)this.mwController.getModuleController(Module.ACCELEROMETER);
        this.mwController.addModuleCallback(mCallback);
        this.mwController.addDeviceCallback(dCallback);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("STATE_VALUES", values);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            values= (HashMap<Integer, Boolean>) savedInstanceState.getSerializable("STATE_VALUES");
        }
        for(final Entry<Integer, Component> it: switches.entrySet()) {
            Switch componentSwitch= (Switch) view.findViewById(it.getKey());
            if (values.containsKey(it.getKey())) {
                componentSwitch.setChecked(values.get(it.getKey()));
            }
            componentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    values.put(it.getKey(), isChecked);
                    if (accelController != null) {
                        if (isChecked) accelController.enableNotification(it.getValue());
                        else accelController.disableNotification(it.getValue());
                    }
                }
            });
        }
    }
}
