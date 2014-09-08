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

import java.util.Timer;
import java.util.TimerTask;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.MetaWearController.ModuleCallbacks;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.NeoPixel;
import com.mbientlab.metawear.api.controller.NeoPixel.ColorOrdering;
import com.mbientlab.metawear.api.controller.NeoPixel.RotationDirection;
import com.mbientlab.metawear.api.controller.NeoPixel.StrandSpeed;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author etsai
 *
 */
public class NeoPixelFragment extends ModuleFragment {
    private TimerTask[] pulsateTasks= {null, null, null};
    private Timer npTimer;
    private NeoPixel neoPixelController;
    
    private byte strand= 0;
    private int presetIndex;
    
    private interface Preset {
        public void setPattern();
    }

    private Preset[] patternPresets= {
        new Preset() {
            @Override
            public void setPattern() {
                byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString()), currStrand= strand;
                for(byte i= 0; i < nLEDs; i++) {
                    neoPixelController.setPixel(currStrand, i, (byte)0, (byte)-1, (byte)0);
                }
            }
        },
        new Preset() {
            @Override
            public void setPattern() {
                byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString()), currStrand= strand;
                double delta= 2 * Math.PI / nLEDs;
                for(byte i= 0; i < nLEDs; i++) {
                    double step= i * delta;
                    double rRatio= Math.cos(step),
                            gRatio= Math.cos(step + 2*Math.PI/3),
                            bRatio= Math.cos(step + 4*Math.PI/3);
                    neoPixelController.setPixel(currStrand, i, (byte)((rRatio < 0 ? -rRatio : rRatio) * 255), 
                            (byte)((gRatio < -gRatio ? 0 : gRatio) * 255), 
                            (byte)((bRatio < -bRatio ? 0 : bRatio) * 255));
                }
            }
        },
        new Preset() {
            private final long period= 20;
            private long time= -period;
            @Override
            public void setPattern() {
                final byte currStrand= strand;
                pulsateTasks[currStrand]= new TimerTask() {
                    @Override
                    public void run() {
                        time+= period;
                        double seconds= time / (double)1000;
                        double rRatio= Math.cos(seconds),
                                gRatio= Math.cos(seconds + 2*Math.PI/3),
                                bRatio= Math.cos(seconds + 4*Math.PI/3);
                        
                        neoPixelController.setPixel(currStrand, (byte)0, (byte)((rRatio < 0 ? 0 : rRatio) * 255), 
                                (byte)((gRatio < 0 ? 0 : gRatio) * 255), 
                                (byte)((bRatio < 0 ? 0 : bRatio) * 255));
                    }
                };
                npTimer.schedule(pulsateTasks[currStrand], 0, period);
            }
        },
        new Preset() {
            private byte index= 0, count= 0;
            private long period= 1000;
            @Override
            public void setPattern() {
                final byte nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString());
                final byte currStrand= strand;
                pulsateTasks[currStrand]= new TimerTask() {
                    @Override
                    public void run() {
                        neoPixelController.setPixel(currStrand, index, (byte)(count == 0 ? 255 : 0), 
                                (byte)(count == 1 ? 255 : 0), 
                                (byte)(count == 2 ? 255 : 0));
                        count++;
                        if (count >= 3) {
                            neoPixelController.clearStrand(currStrand, index, (byte)(index + 1));
                            index++;
                            if (index >= nLEDs) {
                                index= 0;
                            }
                            count= 0;
                        }
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
    
    private void readNeoState() {
        if (neoPixelController != null) {
            neoPixelController.readStrandState(strand);
            neoPixelController.readRotationState(strand);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_neopixel, container, false);
    }
    
    private EditText gpioPinText, nLEDsText, rotationPeriodText;
    private Spinner directionSpinner, speedSpinner;
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        
        final Spinner strandSpinner= (Spinner) view.findViewById(R.id.spinner4);
        strandSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                strand= (byte)position;
                if (mwController.isConnected()) {
                    readNeoState();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        strandSpinner.setAdapter(new ArrayAdapter<Integer>(getActivity(),
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
        presetSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), 
                R.layout.command_row, R.id.command_name, patternNames));
        
        final Button playButton= (Button) view.findViewById(R.id.button2);
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                patternPresets[presetIndex].setPattern();
            }
        });
        
        final Button clearButton= (Button) view.findViewById(R.id.button3);
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
                neoPixelController.clearStrand(strand, (byte)0, 
                        Byte.parseByte(nLEDsText.getEditableText().toString()));
            }
        });
        
        final Button initializeButton= (Button) view.findViewById(R.id.button1);
        initializeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwController.isConnected()) {
                    byte gpioPin= Byte.parseByte(gpioPinText.getEditableText().toString()), 
                            nLEDs= Byte.parseByte(nLEDsText.getEditableText().toString());
                    neoPixelController.initializeStrand(strand, ColorOrdering.MW_WS2811_RGB, 
                            StrandSpeed.values[speedSpinner.getSelectedItemPosition()], gpioPin, nLEDs);
                }
            }
        });
        final Button freeButton= (Button) view.findViewById(R.id.button6);
        freeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwController.isConnected()) {
                    neoPixelController.deinitializeStrand(strand);
                }
            }
        });
        
        gpioPinText= (EditText) view.findViewById(R.id.editText1);
        nLEDsText= (EditText) view.findViewById(R.id.editText2);
        
        //StrandSpeed
        speedSpinner= (Spinner) view.findViewById(R.id.spinner2);
        speedSpinner.setAdapter(new ArrayAdapter<StrandSpeed>(getActivity(), 
                R.layout.command_row, R.id.command_name, StrandSpeed.values));
        
        directionSpinner= (Spinner) view.findViewById(R.id.spinner3);
        directionSpinner.setAdapter(new ArrayAdapter<RotationDirection>(getActivity(), 
                R.layout.command_row, R.id.command_name, RotationDirection.values));
        
        final Button rotateButton= (Button) view.findViewById(R.id.button4);
        rotateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                short rotationPeriod= Short.parseShort(rotationPeriodText.getEditableText().toString());
                neoPixelController.rotateStrand(strand, RotationDirection.values[directionSpinner.getSelectedItemPosition()], 
                        (byte)-1, rotationPeriod);
            }
        });
        final Button stopButton= (Button) view.findViewById(R.id.button5);
        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                short rotationPeriod= Short.parseShort(rotationPeriodText.getEditableText().toString());
                neoPixelController.rotateStrand(strand, RotationDirection.values[directionSpinner.getSelectedItemPosition()], 
                        (byte)0, rotationPeriod);
            }
        });
        
        rotationPeriodText= (EditText) view.findViewById(R.id.editText3);
        
        npTimer= new Timer();
    }
    
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            readNeoState();
        }
    };
    private ModuleCallbacks mCallback= new NeoPixel.Callbacks() {

        @Override
        public void receivedStrandState(byte strandIndex, ColorOrdering order,
                StrandSpeed speed, byte pin, byte strandLength) {
            
            gpioPinText.setText(String.format("%d", pin));
            nLEDsText.setText(String.format("%d", strandLength));
            speedSpinner.setSelection(speed.ordinal());
        }

        @Override
        public void receivedRotatationState(byte strandIndex,
                RotationDirection direction, byte repetitions, short period) {
            rotationPeriodText.setText(String.format("%d", period));
            directionSpinner.setSelection(direction.ordinal());
        }
    };
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        neoPixelController= (NeoPixel)this.mwController.getModuleController(Module.NEO_PIXEL);
        mwController.addDeviceCallback(dCallback).addModuleCallback(mCallback);
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
    
    @Override
    public void onDestroy() {
        mwController.removeDeviceCallback(dCallback);
        mwController.removeModuleCallback(mCallback);
        super.onDestroy();
    }
}