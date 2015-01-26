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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.MetaWearController.DeviceCallbacks;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.*;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.FullScaleRange;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.OutputDataRate;
import com.mbientlab.metawear.api.controller.DataProcessor;
import com.mbientlab.metawear.api.util.BytesInterpreter;
import com.mbientlab.metawear.api.util.FilterConfigBuilder;
import com.mbientlab.metawear.app.popup.AccelerometerSettings;
import com.mbientlab.metawear.app.popup.DataPlotFragment;

import android.app.Activity;
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
import android.widget.Toast;
import android.widget.TextView;

/**
 * @author etsai
 *
 */
public class AccelerometerFragment extends ModuleFragment {
    public interface Configuration {
        public int tapTypePos();
        public int tapAxisPos();
        public int movementPos();
        public int shakeAxisPos();
        public int fsrPos();
        public int odrPos();
        public int firmwarePos();
        
        public void modifyTapType(int newIndex);
        public void modifyTapAxis(int newIndex);
        public void modifyShakeAxis(int newIndex);
        public void modifyMovementType(int newIndex);
        public void modifyDataRange(int newIndex);
        public void modifySamplingRate(int newIndex);
        public void modifyFirmwareVersion(int newIndex);
        
        public Collection<byte[]> polledBytes();
        public byte[] getSamplingConfig();
        public void initialize(byte[] config);
    }
    
    private long start;
    private Vector<Short> rmsValues;
    private Accelerometer accelController;
    private DataProcessor dpController;
    private byte rmsFilterId= -1;
    private Configuration accelConfig;
    
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
            if (accelConfig.movementPos() == 0) {
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
            shakeText.startAnimation(fadeOut);
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
            if (accelConfig.polledBytes() != null) {
                ByteBuffer buffer= ByteBuffer.allocate(14)
                        .putShort(x).putShort(y).putShort(z);
                
                if (start == 0) {
                    buffer.putLong((long) 0);
                    start= System.currentTimeMillis();
                } else {
                    buffer.putLong(System.currentTimeMillis() - start);
                }
                
                accelConfig.polledBytes().add(buffer.array());
            }
        }
    };
    
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

    private static final String CSV_HEADER= String.format("time,xAxis,yAxis,zAxis%n");
    private String dataFilename;
    
    private enum CheckBoxName {
        TAP, SHAKE, ORIENTATION, FREE_FALL, SAMPLING;
    }
    private HashMap<CheckBoxName, CheckBox> checkboxes;
    /*
    private Trigger accelTrigger= new Trigger() {
        @Override public Register register() { return Accelerometer.Register.DATA_VALUE; }
        @Override public byte index() { return (byte) 0xff; }
        @Override public byte offset() { return 0; }
        @Override public byte length() { return 6; }
    };
    */
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        checkboxes= new HashMap<>();
        checkboxes.put(CheckBoxName.TAP, (CheckBox) view.findViewById(R.id.checkBox1));
        checkboxes.put(CheckBoxName.SHAKE, (CheckBox) view.findViewById(R.id.checkBox2));
        checkboxes.put(CheckBoxName.ORIENTATION, (CheckBox) view.findViewById(R.id.checkBox3));
        checkboxes.put(CheckBoxName.FREE_FALL, (CheckBox) view.findViewById(R.id.checkBox4));
        checkboxes.put(CheckBoxName.SAMPLING, (CheckBox) view.findViewById(R.id.checkBox5));
        
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mwMnger.controllerReady()) {
                    accelController.stopComponents();
                    accelController.disableAllDetection(true);
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(true);
                    }
                    
                } else{
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mwMnger.controllerReady()) {
                    if (checkboxes.get(CheckBoxName.TAP).isChecked()) {
                        accelController.enableTapDetection(TapType.values()[accelConfig.tapTypePos()], 
                                Axis.values()[accelConfig.tapAxisPos()]);
                    }
                    if (checkboxes.get(CheckBoxName.SHAKE).isChecked()) {
                        accelController.enableShakeDetection(Axis.values()[accelConfig.shakeAxisPos()]);
                    }
                    if (checkboxes.get(CheckBoxName.ORIENTATION).isChecked()) {
                        accelController.enableOrientationDetection();
                    }
                    if (checkboxes.get(CheckBoxName.FREE_FALL).isChecked()) {
                        if (accelConfig.movementPos() == 0) {
                            accelController.enableFreeFallDetection();
                        } else {
                            accelController.enableMotionDetection(Axis.values());
                        }
                    }
                    if (checkboxes.get(CheckBoxName.SAMPLING).isChecked()) {
                        rmsValues= new Vector<>();
                        SamplingConfig config= accelController.enableXYZSampling();
                        config.withFullScaleRange(FullScaleRange.values()[accelConfig.fsrPos()])
                                .withOutputDataRate(OutputDataRate.values()[accelConfig.odrPos()]);
                        accelConfig.initialize(config.getBytes());
                        start= 0;
                    }
                    accelController.startComponents();
                    
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(false);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentManager fm = getActivity().getSupportFragmentManager();
                final AccelerometerSettings dialog= new AccelerometerSettings();
            
                dialog.show(fm, "accelerometer_settings");
            }
        });
        ((Button) view.findViewById(R.id.textView10)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accelConfig.polledBytes() == null) {
                    Toast.makeText(getActivity(), R.string.error_no_accel_data, Toast.LENGTH_SHORT).show();
                } else {
                    final FragmentManager fm = getActivity().getSupportFragmentManager();
                    final DataPlotFragment dialog= new DataPlotFragment();
                    
                    dialog.show(fm, "resistance_graph_fragment");   
                }
            }
        });
        ((Button) view.findViewById(R.id.textView11)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accelConfig.polledBytes() == null) {
                    Toast.makeText(getActivity(), R.string.error_no_accel_data, Toast.LENGTH_SHORT).show();
                } else {                
                    writeDataToFile();
                    
                    Intent intent= new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Logged Accelerometer Data");
                    
                    File file= getActivity().getFileStreamPath(dataFilename);
                    Uri uri= FileProvider.getUriForFile(getActivity(), 
                            "com.mbientlab.metawear.app.fileprovider", file);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(intent, "Send email..."));
                }
            }
        });
    }
    
    private void writeDataToFile() {
        dataFilename= String.format(Locale.US, "metawear_accelerometer_data-%s-%s.csv", 
                FullScaleRange.values()[accelConfig.fsrPos()].toString(), 
                OutputDataRate.values()[accelConfig.odrPos()].toString());
        try {
            FileOutputStream fos= getActivity().openFileOutput(dataFilename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());
            
            for(byte[] dataBytes: accelConfig.polledBytes()) {
                ByteBuffer buffer= ByteBuffer.wrap(dataBytes);
                double tickInS= (double) (buffer.getLong(6) / 1000.0);
                float xAccel, yAccel, zAccel;
                
                if (accelConfig.firmwarePos() == 0) {
                    xAccel= buffer.getShort(0) / 1000.0f;
                    yAccel= buffer.getShort(2) / 1000.0f;
                    zAccel= buffer.getShort(4) / 1000.0f;
                } else {
                    xAccel= BytesInterpreter.bytesToGs(accelConfig.getSamplingConfig(), buffer.getShort(0));
                    yAccel= BytesInterpreter.bytesToGs(accelConfig.getSamplingConfig(), buffer.getShort(2));
                    zAccel= BytesInterpreter.bytesToGs(accelConfig.getSamplingConfig(), buffer.getShort(4));
                }

                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f%n", tickInS, 
                        xAccel, yAccel, zAccel).getBytes());
            }
            
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
