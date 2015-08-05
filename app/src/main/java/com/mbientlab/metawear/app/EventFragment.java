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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.DataSignal;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.DataProcessor;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.processor.Accumulator;
import com.mbientlab.metawear.processor.Comparison;
import com.mbientlab.metawear.processor.Delta;
import com.mbientlab.metawear.processor.Maths;

import java.util.Map;

/**
 * @author etsai
 *
 */
public class EventFragment extends ModuleFragment {
    private MetaWearBoard currBoard;

    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
    }

    @Override
    public void disconnected() {

    }

    private abstract class EventMacro {
        public abstract void program();
        public abstract void cleanup();
        public abstract String name();
        public abstract String description();
        
        @Override public String toString() { return name(); }
    }
    
    private final EventMacro macros[]= new EventMacro[] {
        new EventMacro() {
            @Override
            public void program() {
                try {
                    final Mma8452qAccelerometer mma8452qAccelerometer= currBoard.getModule(Mma8452qAccelerometer.class);
                    Switch switchModule= currBoard.getModule(Switch.class);
                    final Led ledModule= currBoard.getModule(Led.class);

                    switchModule.routeData().fromSensor()
                            .process(new Accumulator())
                            .process(new Maths(Maths.Operation.MODULUS, 3))
                            .process(new Comparison(Comparison.Operation.EQ, 0))
                            .monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                    ledModule.configureColorChannel(Led.ColorChannel.GREEN).setRiseTime((short) 0)
                                            .setPulseDuration((short) 1000).setRepeatCount((byte) -1)
                                            .setHighTime((short) 500).setHighIntensity((byte) 16)
                                            .setLowIntensity((byte) 16).commit();
                                    ledModule.play(false);
                                }
                            }).commit();
                    mma8452qAccelerometer.routeData().fromTap().monitor(new DataSignal.ActivityHandler() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                            ledModule.stop(true);
                        }
                    }).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            mma8452qAccelerometer.enableTapDetection(Mma8452qAccelerometer.TapType.DOUBLE);
                            mma8452qAccelerometer.start();
                        }
                    });

                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void cleanup() {
                try {
                    final Mma8452qAccelerometer mma8452qAccelerometer= currBoard.getModule(Mma8452qAccelerometer.class);
                    mma8452qAccelerometer.stop();
                    mma8452qAccelerometer.disableTapDetection();

                    currBoard.removeRoutes();
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
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
            @Override
            public void program() {
                try {
                    Switch switchModule = currBoard.getModule(Switch.class);
                    final Led ledModule = currBoard.getModule(Led.class);

                    switchModule.routeData().fromSensor().process(new Accumulator())
                            .process(new Maths(Maths.Operation.MODULUS, 2))
                            .split()
                                .branch().process(new Comparison(Comparison.Operation.EQ, 1)).monitor(new DataSignal.ActivityHandler() {
                                    @Override
                                    public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                        ledModule.configureColorChannel(Led.ColorChannel.BLUE).setRiseTime((short) 0)
                                                .setPulseDuration((short) 1000).setRepeatCount((byte) -1)
                                                .setHighTime((short) 500).setHighIntensity((byte) 16)
                                                .setLowIntensity((byte) 16).commit();
                                        ledModule.play(false);
                                    }
                                })
                                .branch().process(new Comparison(Comparison.Operation.EQ, 0)).monitor(new DataSignal.ActivityHandler() {
                                    @Override
                                    public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                        ledModule.stop(true);
                                    }
                                })
                            .end().commit();
                } catch(UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void cleanup() {
                currBoard.removeRoutes();
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
            @Override
            public void program() {
                try {
                    Switch switchModule = currBoard.getModule(Switch.class);
                    final Led ledModule = currBoard.getModule(Led.class);

                    switchModule.routeData().fromSensor().split()
                            .branch().process(new Comparison(Comparison.Operation.EQ, 1)).monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                    ledModule.configureColorChannel(Led.ColorChannel.BLUE).setRiseTime((short) 0)
                                            .setPulseDuration((short) 1000).setRepeatCount((byte) -1)
                                            .setHighTime((short) 500).setHighIntensity((byte) 16)
                                            .setLowIntensity((byte) 16).commit();
                                    ledModule.play(false);
                                }
                            })
                            .branch().process(new Comparison(Comparison.Operation.EQ, 0)).monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                    ledModule.stop(true);
                                }
                            })
                        .end().commit();
                } catch(UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void cleanup() {
                currBoard.removeRoutes();
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
                try {
                    final Mma8452qAccelerometer mma8452qAccelerometer= currBoard.getModule(Mma8452qAccelerometer.class);
                    final Led ledModule = currBoard.getModule(Led.class);

                    mma8452qAccelerometer.routeData().fromMovement()
                            .monitor(new DataSignal.ActivityHandler() {
                                @Override
                                public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                                    ledModule.configureColorChannel(Led.ColorChannel.RED).setRiseTime((short) 0)
                                            .setPulseDuration((short) 200).setRepeatCount((byte) 10)
                                            .setHighTime((short) 100).setHighIntensity((byte) 16)
                                            .setLowIntensity((byte) 0).commit();
                                    ledModule.play(false);
                                }
                            }).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    mma8452qAccelerometer.enableMovementDetection(Mma8452qAccelerometer.MovementType.MOTION);
                                    mma8452qAccelerometer.start();
                                }
                            });
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
            
            @Override public void cleanup() {
                try {
                    final Mma8452qAccelerometer mma8452qAccelerometer= currBoard.getModule(Mma8452qAccelerometer.class);
                    mma8452qAccelerometer.stop();
                    mma8452qAccelerometer.disableMovementDetection();
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }

                currBoard.removeRoutes();
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
                try {
                    final Temperature tempModule= currBoard.getModule(Temperature.class);
                    final Led ledModule= currBoard.getModule(Led.class);
                    final Switch switchModule= currBoard.getModule(Switch.class);
                    final Timer timerModule= currBoard.getModule(Timer.class);

                    tempModule.routeData().fromSensor().stream("event_5").process(new Delta(Delta.OutputMode.ABSOLUTE, 2.f)).monitor(new DataSignal.ActivityHandler() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                            ledModule.configureColorChannel(Led.ColorChannel.BLUE).setRiseTime((short) 0)
                                    .setPulseDuration((short) 200).setRepeatCount((byte) -1)
                                    .setHighTime((short) 100).setHighIntensity((byte) 16)
                                    .setLowIntensity((byte) 0).commit();
                            ledModule.play(false);
                        }
                    }).commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe("event_5", new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message message) {
                                    Log.i("test", String.format("%.3f", message.getData(Float.class)));
                                }
                            });
                        }
                    });
                    switchModule.routeData().fromSensor().monitor(new DataSignal.ActivityHandler() {
                        @Override
                        public void onSignalActive(Map<String, DataProcessor> map, DataSignal.DataToken dataToken) {
                            ledModule.stop(true);
                        }
                    }).commit();
                    timerModule.scheduleTask(new Timer.Task() {
                        @Override
                        public void commands() {
                            tempModule.readTemperature();
                        }
                    }, 500, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                        @Override
                        public void success(Timer.Controller result) {
                            result.start();
                        }
                    });
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
            
            @Override public void cleanup() {
                try {
                    final Timer timerModule = currBoard.getModule(Timer.class);
                    timerModule.removeTimers();
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    currBoard.removeRoutes();
                }
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
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event, container, false);
    }
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        final Spinner macroSpinner= ((Spinner) view.findViewById(R.id.spinner1));
        macroSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
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

        view.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    macros[macroSpinner.getSelectedItemPosition()].program();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    macros[macroSpinner.getSelectedItemPosition()].cleanup();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
