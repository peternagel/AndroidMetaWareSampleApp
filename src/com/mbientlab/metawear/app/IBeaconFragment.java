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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.IBeacon;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author etsai
 *
 */
public class IBeaconFragment extends ModuleFragment {
    private IBeacon ibeaconController;
    private IBeacon.Callbacks mCallbacks= new IBeacon.Callbacks() {
        @Override
        public void receivedEnableState(byte state) {
            // TODO Auto-generated method stub
            super.receivedEnableState(state);
        }

        @Override
        public void receivedUUID(UUID uuid) {
            values.put(R.id.editText1, uuid.toString());
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText1)).setText(uuid.toString());
            }
        }

        @Override
        public void receivedMajor(short value) {
            values.put(R.id.editText2, String.format("%d", value));
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText2)).setText(String.format("%d", value));
            }
        }

        @Override
        public void receivedMinor(short value) {
            values.put(R.id.editText3, String.format("%d", value));
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText3)).setText(String.format("%d", value));
            }
        }

        @Override
        public void receivedRXPower(byte power) {
            values.put(R.id.editText4, String.format("%d", power));
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText4)).setText(String.format("%d", power));
            }
        }

        @Override
        public void receivedTXPower(byte power) {
            values.put(R.id.editText5, String.format("%d", power));
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText5)).setText(String.format("%d", power));
            }
        }

        @Override
        public void receivedPeriod(short period) {
            values.put(R.id.editText6, String.format("%d", period));
            if (isVisible()) {
                ((EditText) getView().findViewById(R.id.editText6)).setText(String.format("%d", period));
            }
        }
    
    };
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            for(IBeacon.Register it: IBeacon.Register.values()) {
                ibeaconController.readSetting(it);
            }
        }
        
        @Override
        public void disconnected() {
            final View view= getView();
            for(Integer it: editTextBoxes) {
                if (view != null) {
                    ((EditText) view.findViewById(it)).setText("");
                }
                values.put(it, "");
            }
        }
    };
    private HashMap<Integer, String> values= new HashMap<>();
    
    private static final HashSet<Integer> editTextBoxes;
    static {
        editTextBoxes= new HashSet<>();
        editTextBoxes.add(R.id.editText1);
        editTextBoxes.add(R.id.editText2);
        editTextBoxes.add(R.id.editText3);
        editTextBoxes.add(R.id.editText4);
        editTextBoxes.add(R.id.editText5);
        editTextBoxes.add(R.id.editText6); 
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ibeacon, container, false);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            values= (HashMap<Integer, String>) savedInstanceState.getSerializable("STATE_VALUES");
        }
        for(Entry<Integer, String> it: values.entrySet()) {
            ((EditText) view.findViewById(it.getKey())).setText(it.getValue());
        }
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ibeaconController.setUUID(UUID.fromString(values.get(R.id.editText1)))
                            .setMajor(Short.parseShort(values.get(R.id.editText2)))
                            .setMinor(Short.parseShort(values.get(R.id.editText3)))
                            .setCalibratedRXPower(Byte.parseByte(values.get(R.id.editText4)))
                            .setTXPower(Byte.parseByte(values.get(R.id.editText5)))
                            .setAdvertisingPeriod(Short.parseShort(values.get(R.id.editText6)))
                            .enableIBeacon();
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibeaconController.disableIBecon();
            }
        });
        
        int[] editTextIds= {R.id.editText1, R.id.editText2, R.id.editText3, R.id.editText4, R.id.editText5, R.id.editText6};
        for(final int id: editTextIds) {
            ((EditText) view.findViewById(id)).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start,
                        int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start,
                        int before, int count) {
                    values.put(id, s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
        }
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        ibeaconController= (IBeacon) mwController.getModuleController(Module.IBEACON);
        mwController.addModuleCallback(mCallbacks);
        mwController.addDeviceCallback(dCallback);
        
        if (mwController.isConnected()) {
            for(IBeacon.Register it: IBeacon.Register.values()) {
                ibeaconController.readSetting(it);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeModuleCallback(mCallbacks);
            mwController.removeDeviceCallback(dCallback);
        }
        
        super.onDestroy();
    }
    
    @Override
    public void onResume () {
        super.onResume();
        for(Entry<Integer, String> it: values.entrySet()) {
            ((EditText) getView().findViewById(it.getKey())).setText(it.getValue());
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("STATE_VALUES", values);
        
    }
}
