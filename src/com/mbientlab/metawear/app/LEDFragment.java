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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.LED;
import com.mbientlab.metawear.api.controller.LED.ColorChannel;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;

/**
 * @author etsai
 *
 */
public class LEDFragment extends ModuleFragment {
    private LED ledController;
    
    /*
     * .withLowIntensity((byte)seekBarRefs.get(R.id.seekBar2).getProgress())
                        .withRiseTime(extractShort(R.id.editText3))
                        .withHighTime(extractShort(R.id.editText4)).withFallTime(extractShort(R.id.editText5))
                        .withPulseDuration(extractShort(R.id.editText6)).withPulseOffset(extractShort(R.id.editText7))
                        .withRepeatCount(extractByte(R.id.editText8)).commit();
     */
    
    private  final short RISE_TIME= 500, HIGH_TIME= 500, FALL_TIME= 500, DURATION= 2000;
    private static final byte REPEAT_COUNT= 10;
    
    private HashMap<ColorChannel, HashMap<Integer, Integer>> values= new HashMap<>();
    private HashMap<Integer, SeekBar> seekBarRefs= new HashMap<>();
    private ColorChannel currentChannel= ColorChannel.GREEN;

    private static final HashSet<Integer> seekBars;
    static {        
        seekBars= new HashSet<>();
        seekBars.add(R.id.seekBar1);
        seekBars.add(R.id.seekBar2);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_led, container, false);
        
        ((ImageButton)rootView.findViewById(R.id.imageButton3)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                ledController.stop(false);
            }
        });
        ((ImageButton)rootView.findViewById(R.id.imageButton1)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                ledController.play(false);
            }
        });
        ((ImageButton)rootView.findViewById(R.id.imageButton2)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                ledController.pause();
            }
        });
        
        return rootView;
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        ledController= (LED)this.mwController.getModuleController(Module.LED);
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("STATE_VALUES", values);
        outState.putSerializable("STATE_COLOR", currentChannel);
    }
    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            values= (HashMap<ColorChannel, HashMap<Integer, Integer>>) savedInstanceState.getSerializable("STATE_VALUES");
            currentChannel= (ColorChannel) savedInstanceState.getSerializable("STATE_COLOR");
        }
        
        
        for(final Integer it: seekBars) {
            seekBarRefs.put(it, (SeekBar) view.findViewById(it));
            seekBarRefs.get(it).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                        boolean fromUser) { 
                    if (!values.containsKey(currentChannel)) {
                        values.put(currentChannel, new HashMap<Integer, Integer>());
                    }
                    values.get(currentChannel).put(it, progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }
            });
        }
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ledController.setColorChannel(currentChannel).withHighIntensity((byte)seekBarRefs.get(R.id.seekBar1).getProgress())
                        .withLowIntensity((byte)seekBarRefs.get(R.id.seekBar2).getProgress())
                        .withRiseTime(RISE_TIME).withHighTime(HIGH_TIME).withFallTime(FALL_TIME)
                        .withPulseDuration(DURATION).withRepeatCount(REPEAT_COUNT).commit();
            }
        });
        Spinner colorSpinner= (Spinner) view.findViewById(R.id.spinner1);
        colorSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                currentChannel= ColorChannel.values[position];
                for(final Integer it: seekBars) {
                    if (values.get(currentChannel) != null && values.get(currentChannel).containsKey(it)) {
                        seekBarRefs.get(it).setProgress(values.get(currentChannel).get(it));
                    } else {
                        seekBarRefs.get(it).setProgress(0);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        colorSpinner.setAdapter(new ArrayAdapter<ColorChannel>(getActivity(), 
                R.layout.command_row, R.id.command_name, ColorChannel.values));
    }
}
