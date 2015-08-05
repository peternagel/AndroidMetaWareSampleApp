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

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.IBeacon;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

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
    private IBeacon ibeaconController= null;
    private HashMap<Integer, String> values= new HashMap<>();
    
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
        if (values != null) {
            for(Entry<Integer, String> it: values.entrySet()) {
                ((EditText) view.findViewById(it.getKey())).setText(it.getValue());
            }
        }
        view.findViewById(R.id.button1).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        IBeacon.ConfigEditor editor = ibeaconController.configure();
                        editor.setUUID(UUID.fromString(values.get(R.id.editText1)))
                                .setMajor(Short.parseShort(values.get(R.id.editText2)))
                                .setMinor(Short.parseShort(values.get(R.id.editText3)))
                                .setRxPower(Byte.parseByte(values.get(R.id.editText4)))
                                .setTxPower(Byte.parseByte(values.get(R.id.editText5)))
                                .setAdPeriod(Short.parseShort(values.get(R.id.editText6)))
                                .commit();
                        ibeaconController.enable();
                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.button2).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    ibeaconController.disable();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
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

    private MetaWearBoard currBoard;
    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;

        try {
            ibeaconController= currBoard.getModule(IBeacon.class);
            ibeaconController.readConfiguration().onComplete(new AsyncOperation.CompletionHandler<IBeacon.Configuration>() {
                @Override
                public void success(IBeacon.Configuration result) {
                    values.put(R.id.editText1, result.adUuid().toString());
                    values.put(R.id.editText2, String.format("%d", result.major()));
                    values.put(R.id.editText3, String.format("%d", result.minor()));
                    values.put(R.id.editText4, String.format("%d", result.rxPower()));
                    values.put(R.id.editText5, String.format("%d", result.txPower()));
                    values.put(R.id.editText6, String.format("%d", result.adPeriod()));

                    if (isVisible()) {
                        for(Entry<Integer, String> it: values.entrySet()) {
                            ((EditText) getView().findViewById(it.getKey())).setText(it.getValue());
                        }
                    }
                }
            });
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void disconnected() {

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
