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
package com.mbientlab.metawear.app.popup;

import com.mbientlab.metawear.app.R;
import com.mbientlab.metawear.app.AccelerometerFragment.Configuration;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * @author etsai
 *
 */
public class AccelerometerSettings extends DialogFragment {
    private Configuration accelConfig;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if (!(activity instanceof Configuration)) {
            throw new IllegalStateException(
                    "Activity must implement AccelerometerFragment.Configuration interface.");
        }
        
        accelConfig= (Configuration) activity;
    }
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accelerometer_settings, container);
    }
    
    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        getDialog().setTitle("Accelerometer Settings");
        
        Spinner spinnerObj;
        
        spinnerObj= (Spinner) view.findViewById(R.id.spinner1);
        spinnerObj.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, Mma8452qAccelerometer.TapType.values()));
        spinnerObj.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                accelConfig.modifyTapType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        spinnerObj.setSelection(accelConfig.tapTypePos());
        
        spinnerObj= (Spinner) view.findViewById(R.id.spinner2);
        spinnerObj.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, Mma8452qAccelerometer.Axis.values()));
        spinnerObj.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                accelConfig.modifyTapAxis(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        spinnerObj.setSelection(accelConfig.tapAxisPos());
        
        spinnerObj= (Spinner) view.findViewById(R.id.spinner3);
        spinnerObj.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, Mma8452qAccelerometer.Axis.values()));
        spinnerObj.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                accelConfig.modifyShakeAxis(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        spinnerObj.setSelection(accelConfig.shakeAxisPos());
        
        spinnerObj= (Spinner) view.findViewById(R.id.spinner6);
        spinnerObj.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, new String[] {"Free Fall", "Motion"}));
        spinnerObj.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                accelConfig.modifyMovementType(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        spinnerObj.setSelection(accelConfig.movementPos());
    }
}
