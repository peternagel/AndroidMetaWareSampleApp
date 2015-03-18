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
import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.LED;
import com.mbientlab.metawear.api.controller.LED.ChannelDataWriter;
import com.mbientlab.metawear.api.controller.LED.ColorChannel;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * @author etsai
 *
 */
public class LEDFragment extends ModuleFragment {
    private interface Pattern {
        public void program(ChannelDataWriter writer);
    }
    
    private final Pattern[] ledPatterns= {
        new Pattern() {
            @Override
            public void program(ChannelDataWriter writer) {
                writer.withHighTime((short) 50).withPulseDuration((short) 500)
                        .withRepeatCount((byte) -1).commit();
            }
            @Override
            public String toString() {
                return "Blink";
            }
        },
        new Pattern() {
            @Override
            public void program(ChannelDataWriter writer) {
                writer.withHighTime((short) 500).withPulseDuration((short) 500)
                        .withRepeatCount((byte) -1).commit();
            }
            @Override
            public String toString() {
                return "Flashlight";
            }
        },
        new Pattern() {
            @Override
            public void program(ChannelDataWriter writer) {
                writer.withRiseTime((short) 750).withFallTime((short) 750)
                        .withHighTime((short) 500).withPulseDuration((short) 2000)
                        .withRepeatCount((byte) -1).commit();
            }
            @Override
            public String toString() {
                return "Pulse";
            }
        }
    };
    
    private LED ledController;
    private HashMap<ColorChannel, HashMap<Integer, Integer>> values= new HashMap<>();
    private HashMap<Integer, SeekBar> seekBarRefs= new HashMap<>();
    private ColorChannel currentChannel= ColorChannel.GREEN;
    private Pattern currentPattern= ledPatterns[0];

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
                if (mwMnger.controllerReady()) {
                    ledController.stop(false);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((ImageButton)rootView.findViewById(R.id.imageButton1)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    ledController.play(false);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((ImageButton)rootView.findViewById(R.id.imageButton2)).setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    ledController.pause();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        return rootView;
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        ledController= (LED) mwController.getModuleController(Module.LED);
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
                @SuppressLint("UseSparseArrays")
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
                if (mwMnger.controllerReady()) {
                    ChannelDataWriter writer= ledController.setColorChannel(currentChannel)
                            .withHighIntensity((byte)seekBarRefs.get(R.id.seekBar1).getProgress());
                    currentPattern.program(writer);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    ledController.stop(true);
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        Spinner tempSpinner= (Spinner) view.findViewById(R.id.spinner1);
        tempSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
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
        tempSpinner.setAdapter(new ArrayAdapter<ColorChannel>(getActivity(), 
                R.layout.command_row, R.id.command_name, ColorChannel.values));
        
        tempSpinner= (Spinner) view.findViewById(R.id.spinner2);
        tempSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                currentPattern= ledPatterns[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) { }
            
        });
        tempSpinner.setAdapter(new ArrayAdapter<Pattern>(getActivity(),
                R.layout.command_row, R.id.command_name, ledPatterns));
    }
}
