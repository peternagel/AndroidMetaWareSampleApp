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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.Axis;
import com.mbientlab.metawear.api.controller.Accelerometer.TapType;
import com.mbientlab.metawear.api.controller.DataProcessor;
import com.mbientlab.metawear.api.controller.Event;
import com.mbientlab.metawear.api.controller.LED;
import com.mbientlab.metawear.api.controller.MechanicalSwitch;
import com.mbientlab.metawear.api.controller.Temperature;
import com.mbientlab.metawear.api.controller.LED.ColorChannel;
import com.mbientlab.metawear.api.util.FilterConfigBuilder;
import com.mbientlab.metawear.api.util.FilterConfigBuilder.ComparatorBuilder;
import com.mbientlab.metawear.api.util.FilterConfigBuilder.ComparatorBuilder.Operation;
import com.mbientlab.metawear.api.util.FilterConfigBuilder.MathBuilder;
import com.mbientlab.metawear.api.util.LoggingTrigger;

/**
 * @author etsai
 *
 */
public class EventFragment extends ModuleFragment {
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            eventController.enableModule();
            dpController.enableModule();
        }
    };
    
    private abstract class EventMacro {
        public abstract void program();
        public abstract void cleanup();
        public abstract String name();
        public abstract String description();
        
        @Override public String toString() { return name(); }
    }
    
    private final EventMacro macros[]= new EventMacro[] {
        new EventMacro() {
            private byte countFilterUid= -1, compFilterUid= -1;
            private final DataProcessor.Callbacks dpCallbacks= new DataProcessor.Callbacks() {
                @Override
                public void receivedFilterId(byte filterId) {
                    if (countFilterUid == -1) {
                        countFilterUid= filterId;
                        
                        FilterConfigBuilder.ComparatorBuilder builder= new FilterConfigBuilder.ComparatorBuilder();
                        builder.withOperation(Operation.EQ).withReference(3);
                        dpController.chainFilters(filterId, (byte) 1, builder.build());
                    } else {
                        compFilterUid= filterId;
                        
                        eventController.recordMacro(DataProcessor.Register.FILTER_NOTIFICATION, compFilterUid);
                        LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                        ledController.setColorChannel(ColorChannel.GREEN).withRiseTime((short) 0)
                                .withPulseDuration((short) 1000).withRepeatCount((byte) -1)
                                .withHighTime((short) 500).withHighIntensity((byte) 16)
                                .withLowIntensity((byte) 16).commit();
                        ledController.play(false);
                        eventController.stopRecord();
                        
                        eventController.recordMacro(Accelerometer.Register.PULSE_STATUS);
                        dpController.resetFilterState(countFilterUid);
                        ledController.stop(true);
                        eventController.stopRecord();
                        
                        Accelerometer accelController= (Accelerometer) mwMnger.getCurrentController().getModuleController(Module.ACCELEROMETER);
                        accelController.enableTapDetection(TapType.DOUBLE_TAP, Axis.Z);
                        accelController.startComponents();
                        
                        mwMnger.getCurrentController().removeModuleCallback(this);
                    }
                }
            };
            
            @Override
            public void program() {
                mwMnger.getCurrentController().addModuleCallback(dpCallbacks);
                
                FilterConfigBuilder.AccumulatorBuilder builder= new FilterConfigBuilder.AccumulatorBuilder();
                
                builder.withInputSize((byte) 1).withOutputSize((byte) 1);
                dpController.addFilter(LoggingTrigger.SWITCH, builder.build());
            }

            @Override
            public void cleanup() {
                dpController.removeFilter(countFilterUid);
                dpController.removeFilter(compFilterUid);
                
                compFilterUid= -1;
                countFilterUid= -1;
                
                ((Accelerometer) mwMnger.getCurrentController().getModuleController(Module.ACCELEROMETER)).stopComponents();
            }

            @Override
            public String name() {
                return "LED Switch #1";
            }

            @Override
            public String description() {
                return "Press the button 3 times to turn on the LED, double tap to turn it off";
            }
        },
        new EventMacro() {
            private byte onFid= -1, dataSize= 1;
            
            private DataProcessor.Callbacks accum= new DataProcessor.Callbacks() {
                @Override
                public void receivedFilterId(byte filterId) {
                    mwMnger.getCurrentController().removeModuleCallback(this);
                    mwMnger.getCurrentController().addModuleCallback(math);
                  
                    dpController.chainFilters(filterId, (byte) 1, new MathBuilder()
                            .withOperation(MathBuilder.Operation.MODULUS)
                            .withOperand(2)
                            .withInputSize(dataSize)
                            .withOutputSize(dataSize).build());
                }
            }, math= new DataProcessor.Callbacks() {
                @Override
                public void receivedFilterId(byte filterId) {
                    mwMnger.getCurrentController().removeModuleCallback(this);
                    mwMnger.getCurrentController().addModuleCallback(comp);
                  
                    ComparatorBuilder compBuilder= new ComparatorBuilder()
                            .withOperation(ComparatorBuilder.Operation.EQ);
                    
                    dpController.chainFilters(filterId, dataSize, compBuilder.withReference(1).build());
                    dpController.chainFilters(filterId, dataSize, compBuilder.withReference(0).build());
                }
            }, comp= new DataProcessor.Callbacks() {
                @Override
                public void receivedFilterId(byte filterId) {
                    LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                    eventController.recordMacro(DataProcessor.Register.FILTER_NOTIFICATION, filterId);
                  
                    if (onFid == -1) {
                        onFid= filterId;
                        ledController.setColorChannel(ColorChannel.BLUE).withRiseTime((short) 0)
                                .withPulseDuration((short) 1000).withRepeatCount((byte) -1)
                                .withHighTime((short) 500).withHighIntensity((byte) 16)
                                .withLowIntensity((byte) 16).commit();
                        ledController.play(false);
                    } else {
                        ledController.stop(false);
                    }
                  
                    eventController.stopRecord();
                }
            };
            
            @Override
            public void program() {
                mwMnger.getCurrentController().addModuleCallback(accum);
                
                FilterConfigBuilder.AccumulatorBuilder builder= new FilterConfigBuilder.AccumulatorBuilder();
                
                builder.withInputSize((byte) 1).withOutputSize((byte) 1);
                dpController.addFilter(LoggingTrigger.SWITCH, builder.build());
            }

            @Override
            public void cleanup() {
                dpController.removeAllFilters();
                
                onFid= -1;
                
                ((Accelerometer) mwMnger.getCurrentController().getModuleController(Module.ACCELEROMETER)).stopComponents();
            }

            @Override
            public String name() {
                return "LED Switch #2";
            }

            @Override
            public String description() {
                return "Press the button to turn on LED, press again to turn off";
            }
        },
        new EventMacro() {
            private byte lowFilterUid= -1, highFilterUid= -1;
            private final DataProcessor.Callbacks dpCallbacks= new DataProcessor.Callbacks() {
                @Override
                public void receivedFilterId(byte filterId) {
                    eventController.recordMacro(DataProcessor.Register.FILTER_NOTIFICATION, filterId);
                    LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                    
                    if (highFilterUid == -1) {
                        highFilterUid= filterId;
                        ledController.play(true);
                    } else {
                        lowFilterUid= filterId;
                        ledController.stop(false);
                        
                        mwMnger.getCurrentController().removeModuleCallback(this);
                    }
                    eventController.stopRecord();
                }
            };
            
            @Override
            public void program() {
                mwMnger.getCurrentController().addModuleCallback(dpCallbacks);
                
                LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                ledController.setColorChannel(ColorChannel.RED)
                        .withPulseDuration((short) 100).withRepeatCount((byte) -1)
                        .withHighTime((short) 100).withHighIntensity((byte) 16).commit();
                
                ledController.setColorChannel(ColorChannel.BLUE)
                        .withPulseDuration((short) 100).withRepeatCount((byte) -1)
                        .withHighTime((short) 100).withHighIntensity((byte) 16).commit();
        
                dpController.addFilter(LoggingTrigger.SWITCH, new FilterConfigBuilder.ComparatorBuilder()
                        .withOperation(Operation.EQ).withReference(1).build());
                dpController.addFilter(LoggingTrigger.SWITCH, new FilterConfigBuilder.ComparatorBuilder()
                        .withOperation(Operation.EQ).withReference(0).build());
            }

            @Override
            public void cleanup() {
                dpController.removeFilter(lowFilterUid);
                dpController.removeFilter(highFilterUid);
                
                lowFilterUid= -1;
                highFilterUid= -1;
            }

            @Override
            public String name() {
                return "LED Switch #3";
            }

            @Override
            public String description() {
                return "Hold button to keep LED on, release to turn it off";
            }            
        },
        new EventMacro() {
            @Override public void program() {
                LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                
                eventController.recordMacro(Accelerometer.Register.FREE_FALL_VALUE);
                ledController.setColorChannel(ColorChannel.RED).withRiseTime((short) 0)
                        .withPulseDuration((short) 200).withRepeatCount((byte) 10)
                        .withHighTime((short) 100).withHighIntensity((byte) 16)
                        .withLowIntensity((byte) 0).commit();
                ledController.play(false);
                eventController.stopRecord();
                
                Accelerometer accelController= (Accelerometer) mwMnger.getCurrentController().getModuleController(Module.ACCELEROMETER);
                accelController.enableMotionDetection(Axis.values());
                accelController.startComponents();
            }
            
            @Override public void cleanup() {
                ((Accelerometer) mwMnger.getCurrentController().getModuleController(Module.ACCELEROMETER)).stopComponents();
            }

            @Override
            public String name() {
                return "Motion Alert";
            }

            @Override
            public String description() {
                return "Move the MetaWear to turn on the LED";
            }
        },
        new EventMacro() {
            @Override public void program() {
                LED ledController= (LED) mwMnger.getCurrentController().getModuleController(Module.LED);
                
                eventController.recordMacro(Temperature.Register.DELTA_TEMP);
                ledController.setColorChannel(ColorChannel.BLUE).withRiseTime((short) 0)
                    .withPulseDuration((short) 200).withRepeatCount((byte) -1)
                    .withHighTime((short) 100).withHighIntensity((byte) 16)
                    .withLowIntensity((byte) 0).commit();
                ledController.play(false);
                eventController.stopRecord();
                
                eventController.recordMacro(MechanicalSwitch.Register.SWITCH_STATE);
                ledController.stop(true);
                eventController.stopRecord();
                
                Temperature tempController= (Temperature) mwMnger.getCurrentController().getModuleController(Module.TEMPERATURE);
                tempController.enableSampling().withSamplingPeriod(500)
                    .withTemperatureDelta(2).commit();
            }
            
            @Override public void cleanup() {
                ((Temperature) mwMnger.getCurrentController().getModuleController(Module.TEMPERATURE)).disableSampling();
            }

            @Override
            public String name() {
                return "Temperature Monitor";
            }

            @Override
            public String description() {
                return "Change the temperature by 2C to blink LED, press button to turn it off";
            }
        }
    };
    private Event eventController;
    private DataProcessor dpController;

    @Override
    public void controllerReady(MetaWearController mwController) {
        eventController= (Event) mwController.getModuleController(Module.EVENT);
        dpController= (DataProcessor) mwController.getModuleController(Module.DATA_PROCESSOR);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event, container, false);
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeDeviceCallback(dCallback);
        }
        
        super.onDestroy();
    }
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        final Spinner macroSpinner= ((Spinner) view.findViewById(R.id.spinner1));
        macroSpinner.setAdapter(new ArrayAdapter<EventMacro>(getActivity(),
                R.layout.command_row, R.id.command_name, macros));
        
        macroSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View innerView,
                    int position, long id) {
                TextView description= (TextView) view.findViewById(R.id.textView2);
                description.setText(macros[position].description());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    macros[macroSpinner.getSelectedItemPosition()].program();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    macros[macroSpinner.getSelectedItemPosition()].cleanup();
                    eventController.removeMacros();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
