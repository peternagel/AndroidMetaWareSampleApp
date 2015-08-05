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

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.NeoPixel;

/**
 * @author etsai
 *
 */
public class NeoPixelFragment extends ModuleFragment {
    private TimerTask[] pulsateTasks= {null, null, null};
    private Timer npTimer;
    
    private byte strand= 0;
    private int presetIndex;
    private MetaWearBoard currBoard;

    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
    }

    @Override
    public void disconnected() {

    }

    private interface Preset {
        void setPattern(NeoPixel neoPixelModule);
    }

    private Preset[] patternPresets= {
        new Preset() {
            @Override
            public void setPattern(NeoPixel neoPixelModule) {
                byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString()), currStrand= strand;

                neoPixelModule.holdStrand(currStrand);
                for(byte i= 0; i < nLEDs; i++) {
                    neoPixelModule.setPixel(currStrand, i, (byte)0, (byte)-1, (byte)0);
                }
                neoPixelModule.releaseHold(currStrand);
            }
        },
        new Preset() {
            @Override
            public void setPattern(NeoPixel neoPixelModule) {
                byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString()), currStrand= strand;
                double delta= 2 * Math.PI / nLEDs;

                neoPixelModule.holdStrand(currStrand);
                for(byte i= 0; i < nLEDs; i++) {
                    double step= i * delta;
                    double rRatio= Math.cos(step),
                            gRatio= Math.cos(step + 2*Math.PI/3),
                            bRatio= Math.cos(step + 4*Math.PI/3);
                    neoPixelModule.setPixel(currStrand, i, (byte)((rRatio < 0 ? 0 : rRatio) * 255),
                            (byte)((gRatio < 0 ? 0 : gRatio) * 255), 
                            (byte)((bRatio < 0 ? 0 : bRatio) * 255));
                }
                neoPixelModule.releaseHold(currStrand);
            }
        },
        new Preset() {
            private final long period= 20;
            private long time= -period;
            @Override
            public void setPattern(final NeoPixel neoPixelModule) {
                final byte currStrand= strand;
                pulsateTasks[currStrand]= new TimerTask() {
                    @Override
                    public void run() {
                        time+= period;
                        double seconds= time / (double)1000;
                        double rRatio= Math.cos(seconds),
                                gRatio= Math.cos(seconds + 2*Math.PI/3),
                                bRatio= Math.cos(seconds + 4*Math.PI/3);

                        neoPixelModule.setPixel(currStrand, (byte)0, (byte)((rRatio < 0 ? 0 : rRatio) * 255),
                                (byte)((gRatio < 0 ? 0 : gRatio) * 255), 
                                (byte)((bRatio < 0 ? 0 : bRatio) * 255));
                    }
                };
                npTimer.schedule(pulsateTasks[currStrand], 0, period);
            }
        },
        new Preset() {
            private byte index= 0, count= -1;
            private long period= 1000;
            @Override
            public void setPattern(final NeoPixel neoPixelModule) {
                final byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString());
                final byte currStrand= strand;
                pulsateTasks[currStrand]= new TimerTask() {
                    @Override
                    public void run() {
                        count++;
                        if (count >= 3) {
                            neoPixelModule.clearStrand(currStrand, index, index);
                            index++;
                            if (index >= nLEDs) {
                                index= 0;
                            }
                            count= 0;
                        }
                        neoPixelModule.setPixel(currStrand, index, (byte) (count == 0 ? 255 : 0),
                                (byte) (count == 1 ? 255 : 0),
                                (byte) (count == 2 ? 255 : 0));
                    }
                };
                npTimer.schedule(pulsateTasks[currStrand], 0, period);
            }
        }
    };
    private String[] patternNames= {"All Green", "Rainbow", "Pulsate", "RGB Check"};
    
    private void resetTimer() {
        if (pulsateTasks[strand] != null) {
            pulsateTasks[strand].cancel();
            pulsateTasks[strand]= null;
            npTimer.purge();
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_neopixel, container, false);
    }
    
    private EditText gpioPinText, nLEDsText, rotationPeriodText;
    private Spinner directionSpinner, speedSpinner, orderingSpinner;
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        
        final Spinner strandSpinner= (Spinner) view.findViewById(R.id.spinner4);
        strandSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                strand= (byte)position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        strandSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, new Integer[] {0, 1, 2}));
        
        final Spinner presetSpinner= (Spinner) view.findViewById(R.id.spinner1);
        presetSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                presetIndex= position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        presetSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, patternNames));
        
        final Button playButton= (Button) view.findViewById(R.id.button2);
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        NeoPixel neoPixelController = currBoard.getModule(NeoPixel.class);
                        patternPresets[presetIndex].setPattern(neoPixelController);
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        final Button clearButton= (Button) view.findViewById(R.id.button3);
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        NeoPixel neoPixelController= currBoard.getModule(NeoPixel.class);
                        neoPixelController.clearStrand(strand, (byte)0, Byte.parseByte(nLEDsText.getEditableText().toString()));
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        final Button initializeButton= (Button) view.findViewById(R.id.button1);
        initializeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        try {
                            NeoPixel neoPixelController= currBoard.getModule(NeoPixel.class);
                            byte gpioPin= Byte.parseByte(gpioPinText.getEditableText().toString()),
                                    nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString());
                            neoPixelController.initializeStrand(strand, NeoPixel.ColorOrdering.values()[orderingSpinner.getSelectedItemPosition()],
                                    NeoPixel.StrandSpeed.values()[speedSpinner.getSelectedItemPosition()], gpioPin, nLEDs);
                        } catch (UnsupportedModuleException e) {
                            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }


                    } catch (Exception ex) {
                        Toast.makeText(getActivity(), ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        final Button freeButton= (Button) view.findViewById(R.id.button6);
        freeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        NeoPixel neoPixelController = currBoard.getModule(NeoPixel.class);
                        neoPixelController.deinitializeStrand(strand);
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        gpioPinText= (EditText) view.findViewById(R.id.editText1);
        nLEDsText= (EditText) view.findViewById(R.id.editText2);
        
        //StrandSpeed
        speedSpinner= (Spinner) view.findViewById(R.id.spinner2);
        speedSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, NeoPixel.StrandSpeed.values()));
        
        orderingSpinner= (Spinner) view.findViewById(R.id.spinner5);
        orderingSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, NeoPixel.ColorOrdering.values()));
        
        directionSpinner= (Spinner) view.findViewById(R.id.spinner3);
        directionSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, NeoPixel.RotationDirection.values()));
        
        final Button rotateButton= (Button) view.findViewById(R.id.button4);
        rotateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        NeoPixel neoPixelController = currBoard.getModule(NeoPixel.class);
                        short rotationPeriod = Short.parseShort(rotationPeriodText.getEditableText().toString());
                        neoPixelController.rotate(strand, NeoPixel.RotationDirection.values()[directionSpinner.getSelectedItemPosition()],
                                rotationPeriod);
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        final Button stopButton= (Button) view.findViewById(R.id.button5);
        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        NeoPixel neoPixelController = currBoard.getModule(NeoPixel.class);
                        short rotationPeriod = Short.parseShort(rotationPeriodText.getEditableText().toString());
                        neoPixelController.stopRotation(strand);
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        rotationPeriodText= (EditText) view.findViewById(R.id.editText3);
        
        npTimer= new Timer();
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ble_disconnect:
            resetTimer();
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }
}