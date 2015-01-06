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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.Register;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.*;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.FullScaleRange;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.OutputDataRate;
import com.mbientlab.metawear.api.controller.DataProcessor;
import com.mbientlab.metawear.api.controller.Logging.Trigger;
import com.mbientlab.metawear.api.util.BytesInterpreter;
import com.mbientlab.metawear.api.util.FilterConfigBuilder;
import com.mbientlab.metawear.app.popup.AccelerometerSettings;
import com.mbientlab.metawear.app.popup.DataPlotFragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * @author etsai
 *
 */
public class AccelerometerFragment extends ModuleFragment {
    private class AxisData {
        /**
         * @param tick
         * @param data
         */
        public AxisData(long tick, short x, short y, short z) {
            this.tick = tick;
            this.data = new short[] {x, y, z};
        }
        public final long tick;
        public final short[] data;
    }
    
    private interface ProcessedAxisData {
        public Collection<GraphViewDataInterface> xData();
        public Collection<GraphViewDataInterface> yData();
        public Collection<GraphViewDataInterface> zData();
        public Collection<GraphViewDataInterface> rmsData();
    }
    
    private long start;
    private Vector<AxisData> polledData;
    private Vector<Short> rmsValues;
    private ProcessedAxisData processedData;
    private Accelerometer accelController;
    private DataProcessor dpController;
    private byte rmsFilterId= -1;
    
    private static final AlphaAnimation fadeOut= new AlphaAnimation(1.0f , 0.0f);
    static {
        fadeOut.setDuration(2000);
        fadeOut.setFillAfter(true);
    }
    
    private DeviceCallbacks dCallback= new MetaWearController.DeviceCallbacks() {
        @Override
        public void connected() {
            FilterConfigBuilder.RMSBuilder builder= new FilterConfigBuilder.RMSBuilder();
            builder.withInputCount((byte) 3).withSignedInput().withInputSize((byte) 2)
                    .withOutputSize((byte) 2);
            
            //dpController.addFilter(accelTrigger, builder.build());
        }
    };
    
    private final DataProcessor.Callbacks dpCallbacks= new DataProcessor.Callbacks() {
        @Override
        public void receivedFilterId(byte filterId) {
            rmsFilterId= filterId;
            dpController.enableFilterNotify(rmsFilterId);
        }
        
        @Override
        public void receivedFilterOutput(byte filterId, byte[] output) {
            rmsValues.add(ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN).getShort());
        }   
    };
    
    private Accelerometer.Callbacks mCallback= new Accelerometer.Callbacks() {
        
        @Override 
        public void movementDetected(MovementData moveData) {
            TextView shakeText= (TextView) getView().findViewById(R.id.textView8);
            if (ffMovement) {
                shakeText.setText("Falling Skies");
            } else {
                shakeText.setText("Move your body");
            }
            shakeText.startAnimation(fadeOut);
        }
        
        @Override 
        public void orientationChanged(Orientation accelOrientation) {
            TextView shakeText= (TextView) getView().findViewById(R.id.textView6);
            shakeText.setText(String.format(Locale.US, "%s", accelOrientation.toString()));
        }
        
        @Override
        public void shakeDetected(MovementData moveData) {
            TextView shakeText= (TextView) getView().findViewById(R.id.textView4);
            shakeText.setText("Shake it like a polariod picture");
            shakeText.startAnimation(fadeOut);
        }
        
        @Override
        public void doubleTapDetected(MovementData moveData) {
            TextView tapText= (TextView) getView().findViewById(R.id.textView2);
            tapText.setText("Double Beer Taps");
            tapText.startAnimation(fadeOut);
        }
        
        @Override
        public void singleTapDetected(MovementData moveData) {
            TextView tapText= (TextView) getView().findViewById(R.id.textView2);
            tapText.setText("Beer Tap");
            tapText.startAnimation(fadeOut);
        }

        @Override
        public void receivedDataValue(short x, short y, short z) {
            if (polledData != null) {
                if (start == 0) {
                    polledData.add(new AxisData(0, x, y, z));
                    start= System.currentTimeMillis();
                } else {
                    polledData.add(new AxisData(System.currentTimeMillis() - start, x, y, z));
                }
            }
        }
    };
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accelerometer, container, false);
    }
    
    @Override
    public void onDestroy() {
        final MetaWearController mwController= mwMnger.getCurrentController();
        if (mwMnger.hasController()) {
            mwController.removeModuleCallback(mCallback);
            mwController.removeModuleCallback(dpCallbacks);
            mwController.removeDeviceCallback(dCallback);
        }
        
        if (rmsFilterId != -1) {
            dpController.removeFilter(rmsFilterId);
        }
        super.onDestroy();
    }

    @Override
    public void controllerReady(MetaWearController mwController) {
        accelController= (Accelerometer) mwController.getModuleController(Module.ACCELEROMETER);
        dpController= (DataProcessor) mwController.getModuleController(Module.DATA_PROCESSOR);
        mwController.addModuleCallback(mCallback).addModuleCallback(dpCallbacks).addDeviceCallback(dCallback);
        
        if (mwController.isConnected()) {
            FilterConfigBuilder.RMSBuilder builder= new FilterConfigBuilder.RMSBuilder();
            builder.withInputCount((byte) 3).withSignedInput().withInputSize((byte) 2)
                    .withOutputSize((byte) 2);
            
            //dpController.addFilter(accelTrigger, builder.build());
        }
    }
    
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private static final String CSV_HEADER= String.format("time,xAxis,yAxis,zAxis,rms%n");
    private String dataFilename;
    
    private enum CheckBoxName {
        TAP, SHAKE, ORIENTATION, FREE_FALL, SAMPLING;
    }
    private HashMap<CheckBoxName, CheckBox> checkboxes;
    
    private Trigger accelTrigger= new Trigger() {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte index() { return (byte) 0xff; }
        @Override public byte offset() { return 0; }
        @Override public byte length() { return 6; }
    };
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        checkboxes= new HashMap<>();
        checkboxes.put(CheckBoxName.TAP, (CheckBox) view.findViewById(R.id.checkBox1));
        checkboxes.put(CheckBoxName.SHAKE, (CheckBox) view.findViewById(R.id.checkBox2));
        checkboxes.put(CheckBoxName.ORIENTATION, (CheckBox) view.findViewById(R.id.checkBox3));
        checkboxes.put(CheckBoxName.FREE_FALL, (CheckBox) view.findViewById(R.id.checkBox4));
        checkboxes.put(CheckBoxName.SAMPLING, (CheckBox) view.findViewById(R.id.checkBox5));
        
        ((ToggleButton) view.findViewById(R.id.toggleButton1)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    if (checkboxes.get(CheckBoxName.TAP).isChecked()) {
                        accelController.enableTapDetection(TapType.values()[tapType], 
                                Axis.values()[tapAxis]);
                        /*
                                .withDuration((float) 50.625)
                                .withLatency((float) 41.25).withWindow((float) 301.25)
                                .withThreshold((float) 31.98);
                                */
                                
                    }
                    if (checkboxes.get(CheckBoxName.SHAKE).isChecked()) {
                        accelController.enableShakeDetection(Axis.values()[shakeAxis]);
                    }
                    if (checkboxes.get(CheckBoxName.ORIENTATION).isChecked()) {
                        accelController.enableOrientationDetection();
                    }
                    if (checkboxes.get(CheckBoxName.FREE_FALL).isChecked()) {
                        if (ffMovement) {
                            accelController.enableFreeFallDetection();
                        } else {
                            accelController.enableMotionDetection(Axis.values());
                        }
                    }
                    if (checkboxes.get(CheckBoxName.SAMPLING).isChecked()) {
                        processedData= null;
                        polledData= new Vector<>();
                        rmsValues= new Vector<>();
                        samplingConfig= accelController.enableXYZSampling();
                        samplingConfig.withFullScaleRange(FullScaleRange.values()[dataRange])
                                .withOutputDataRate(OutputDataRate.values()[samplingRate]);
                        start= 0;
                    }
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(false);
                    }
                    accelController.startComponents();
                } else {
                    accelController.stopComponents();
                    accelController.disableAllDetection(true);
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(true);
                    }
                }
                
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentManager fm = getActivity().getSupportFragmentManager();
                final AccelerometerSettings dialog= new AccelerometerSettings();
                
                dialog.setConfigEditor(new Configuration(), new ConfigEditor());
                dialog.show(fm, "resistance_graph_fragment");
            }
        });
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentManager fm = getActivity().getSupportFragmentManager();
                final DataPlotFragment dialog= new DataPlotFragment();
                
                dataSampleToGs();
                
                dialog.addDataSeries("X Axis", processedData.xData());
                dialog.addDataSeries("Y Axis", processedData.yData());
                dialog.addDataSeries("Z Axis", processedData.zData());
                dialog.addDataSeries("RMS", processedData.rmsData());
                dialog.show(fm, "resistance_graph_fragment");
            }
        });
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dataSampleToGs();
                
                Intent intent= new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Logged Accelerometer Data");
                
                File file= getActivity().getFileStreamPath(dataFilename);
                Uri uri= FileProvider.getUriForFile(getActivity(), 
                        "com.mbientlab.metawear.app.fileprovider", file);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));
            }
        });
    }
    
    private void dataSampleToGs() {
        if (processedData != null) return;
        
        final Collection<GraphViewDataInterface> convertedX= new ArrayList<>(), 
                convertedY= new ArrayList<>(), convertedZ= new ArrayList<>(), 
                convertedRms= new ArrayList<>();
        byte[] config= samplingConfig.getBytes();

        dataFilename= String.format(Locale.US, "metawear_accelerometer_data-%s-%s.csv", 
                FullScaleRange.values()[dataRange].toString(), 
                OutputDataRate.values()[samplingRate].toString());
        try {
            FileOutputStream fos= getActivity().openFileOutput(dataFilename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());
            
            double index= 0;
            for(AxisData data: polledData) {
                double tickInS= (double) (data.tick / 1000.0);
                float xAccel= BytesInterpreter.bytesToGs(config, data.data[0]), 
                        yAccel= BytesInterpreter.bytesToGs(config, data.data[1]),
                        zAccel= BytesInterpreter.bytesToGs(config, data.data[2]),
                        rmsAccel= index < rmsValues.size() ? BytesInterpreter.bytesToGs(config, rmsValues.get((int) index))
                                : 0;
                
                convertedX.add(new GraphViewData(tickInS, xAccel));
                convertedY.add(new GraphViewData(tickInS, yAccel));
                convertedZ.add(new GraphViewData(tickInS, zAccel));
                convertedRms.add(new GraphViewData(tickInS, rmsAccel));
                
                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f,%.3f%n", tickInS, 
                        xAccel, yAccel, zAccel, rmsAccel).getBytes());
                
                index++;
            }
            
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        processedData= new ProcessedAxisData() {
            @Override
            public Collection<GraphViewDataInterface> xData() {
                return convertedX;
            }

            @Override
            public Collection<GraphViewDataInterface> yData() {
                return convertedY;
            }

            @Override
            public Collection<GraphViewDataInterface> zData() {
                return convertedZ;
            }

            @Override
            public Collection<GraphViewDataInterface> rmsData() {
                return convertedRms;
            }
        };
    }
    
    public class ConfigEditor {
        public ConfigEditor modifyTapType(int newIndex) {
            AccelerometerFragment.this.tapType= newIndex;
            return this;
        }
        public ConfigEditor modifyTapAxis(int newIndex) {
            AccelerometerFragment.this.tapAxis= newIndex;
            return this;
        }
        public ConfigEditor modifyShakeAxis(int newIndex) {
            AccelerometerFragment.this.shakeAxis= newIndex;
            return this;
        }
        public ConfigEditor modifyMovementType(int newIndex) {
            AccelerometerFragment.this.ffMovement= newIndex == 0;
            return this;
        }
        public ConfigEditor modifyDataRange(int newIndex) {
            AccelerometerFragment.this.dataRange= newIndex;
            return this;
        }
        public ConfigEditor modifySamplingRate(int newIndex) {
            AccelerometerFragment.this.samplingRate= newIndex;
            return this;
        }
    };
    
    public class Configuration {
        public int tapTypePos() { return tapType; }
        public int tapAxisPos() { return tapAxis; }
        public int movementPos() { return ffMovement ? 0 : 1; }
        public int shakeAxisPos() { return shakeAxis; }
        public int fsrPos() { return dataRange; }
        public int odrPos() { return samplingRate; }
    }
    
    private int tapType= 0, tapAxis= 2, shakeAxis= 0, dataRange= 2, samplingRate= 3;
    private boolean ffMovement= true;
    
    private SamplingConfig samplingConfig;
}
