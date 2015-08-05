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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.MetaWearBoard;

import com.mbientlab.metawear.app.popup.AccelerometerSettings;
import com.mbientlab.metawear.app.popup.DataPlotFragment;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Mma8452qAccelerometer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author etsai
 */
public class AccelerometerFragment extends ModuleFragment {
    public interface Configuration {
        int tapTypePos();
        int tapAxisPos();
        int movementPos();
        int shakeAxisPos();
        
        void modifyTapType(int newIndex);
        void modifyTapAxis(int newIndex);
        void modifyShakeAxis(int newIndex);
        void modifyMovementType(int newIndex);
        
        Collection<float[]> polledBytes();
        void initialize();
    }

    private Mma8452qAccelerometer mma8452qModule= null;
    private Bmi160Accelerometer bmi160AccelModule= null;
    private Accelerometer accelModule;
    private long start;
    private Configuration accelConfig;
    
    private static final AlphaAnimation fadeOut= new AlphaAnimation(1.0f , 0.0f);
    static {
        fadeOut.setDuration(2000);
        fadeOut.setFillAfter(true);
    }

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

    private void addRoutes() {
        accelModule.routeData().fromAxes().stream("axis_stream").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                result.subscribe("axis_stream", new RouteManager.MessageHandler() {
                    @Override
                    public void process(Message message) {
                        if (accelConfig.polledBytes() != null) {
                            float offset;
                            CartesianFloat axisGs = message.getData(CartesianFloat.class);
                            if (start == 0) {
                                start = System.currentTimeMillis();
                                offset = 0.f;
                            } else {
                                offset = (System.currentTimeMillis() - start) / 1000.f;
                            }

                            accelConfig.polledBytes().add(new float[]{offset, axisGs.x(), axisGs.y(), axisGs.z()});
                        }
                    }
                });
            }
        });

        if (mma8452qModule != null) {
            mma8452qModule.routeData().fromOrientation().stream("orientation_stream").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("orientation_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message message) {
                            TextView responseText= (TextView) getView().findViewById(R.id.textView6);
                            responseText.setText(String.format(Locale.US, "%s", message.getData(Mma8452qAccelerometer.Orientation.class).toString()));
                            responseText.startAnimation(fadeOut);
                        }
                    });
                }
            });
            mma8452qModule.routeData().fromMovement().stream("movement_stream").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("movement_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message message) {
                            TextView responseText= (TextView) getView().findViewById(R.id.textView8);
                            if (accelConfig.movementPos() == 0) {
                                responseText.setText("Falling Skies");
                            } else {
                                responseText.setText("Move your body");
                            }
                            responseText.startAnimation(fadeOut);
                        }
                    });
                }
            });
            mma8452qModule.routeData().fromShake().stream("shake_stream").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("shake_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message message) {
                            TextView responseText= (TextView) getView().findViewById(R.id.textView4);
                            responseText.setText("Shake it like a polariod picture");
                            responseText.startAnimation(fadeOut);
                        }
                    });
                }
            });
            mma8452qModule.routeData().fromTap().stream("tap_stream").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                @Override
                public void success(RouteManager result) {
                    result.subscribe("tap_stream", new RouteManager.MessageHandler() {
                        @Override
                        public void process(Message message) {
                            TextView tapText= (TextView) getView().findViewById(R.id.textView2);

                            Mma8452qAccelerometer.TapData data= message.getData(Mma8452qAccelerometer.TapData.class);
                            switch(data.type()) {
                                case DOUBLE:
                                    tapText.setText("Double Taps");
                                    break;
                                case SINGLE:
                                    tapText.setText("Single Taps");
                                    break;
                                default:
                                    tapText.setText("Unkown");
                                    break;
                            }
                            tapText.startAnimation(fadeOut);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void connected(MetaWearBoard currBoard) {
        currBoard.removeRoutes();

        try {
            accelModule = currBoard.getModule(Accelerometer.class);
            if (accelModule instanceof Mma8452qAccelerometer) {
                mma8452qModule = (Mma8452qAccelerometer) accelModule;
            } else if (accelModule instanceof  Bmi160Accelerometer) {
                bmi160AccelModule= (Bmi160Accelerometer) accelModule;
            }

            addRoutes();
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void disconnected() {

    }

    private static final String CSV_HEADER= String.format("time,xAxis,yAxis,zAxis%n");
    private String dataFilename;
    
    private enum CheckBoxName {
        TAP, SHAKE, ORIENTATION, FREE_FALL, SAMPLING;
    }
    private HashMap<CheckBoxName, CheckBox> checkboxes;
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        checkboxes= new HashMap<>();
        checkboxes.put(CheckBoxName.TAP, (CheckBox) view.findViewById(R.id.checkBox1));
        checkboxes.put(CheckBoxName.SHAKE, (CheckBox) view.findViewById(R.id.checkBox2));
        checkboxes.put(CheckBoxName.ORIENTATION, (CheckBox) view.findViewById(R.id.checkBox3));
        checkboxes.put(CheckBoxName.FREE_FALL, (CheckBox) view.findViewById(R.id.checkBox4));
        checkboxes.put(CheckBoxName.SAMPLING, (CheckBox) view.findViewById(R.id.checkBox5));
        
        view.findViewById(R.id.button3).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mwMnger.controllerReady()) {
                    accelModule.stop();

                    if (mma8452qModule != null) {
                        mma8452qModule.disableMovementDetection();
                        mma8452qModule.disableShakeDetection();
                        mma8452qModule.disableTapDetection();
                        mma8452qModule.disableOrientationDetection();
                    }

                    accelModule.disableAxisSampling();
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(true);
                    }

                    ((TextView) view.findViewById(R.id.textView2)).setText("");
                    ((TextView) view.findViewById(R.id.textView4)).setText("");
                    ((TextView) view.findViewById(R.id.textView6)).setText("");
                    ((TextView) view.findViewById(R.id.textView8)).setText("");
                } else{
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mwMnger.controllerReady()) {
                    if (checkboxes.get(CheckBoxName.TAP).isChecked()) {
                        if (mma8452qModule != null) {
                            mma8452qModule.configureTapDetection()
                                    .setAxis(Mma8452qAccelerometer.Axis.values()[accelConfig.tapAxisPos()]).commit();
                            mma8452qModule.enableTapDetection(Mma8452qAccelerometer.TapType.values()[accelConfig.tapTypePos()]);
                        } else {
                            ((TextView) view.findViewById(R.id.textView2)).setText("Not yet supported");
                        }
                    }
                    if (checkboxes.get(CheckBoxName.SHAKE).isChecked()) {
                        if (mma8452qModule != null) {
                            mma8452qModule.configureShakeDetection()
                                    .setAxis(Mma8452qAccelerometer.Axis.values()[accelConfig.shakeAxisPos()]).commit();
                            mma8452qModule.enableShakeDetection();
                        } else {
                            ((TextView) view.findViewById(R.id.textView4)).setText("Not yet supported");
                        }
                    }
                    if (checkboxes.get(CheckBoxName.ORIENTATION).isChecked()) {
                        if (mma8452qModule != null) {
                            mma8452qModule.enableOrientationDetection();
                        } else {
                            ((TextView) view.findViewById(R.id.textView6)).setText("Not yet supported");
                        }
                    }
                    if (checkboxes.get(CheckBoxName.FREE_FALL).isChecked()) {
                        if (mma8452qModule != null) {
                            if (accelConfig.movementPos() == 0) {
                                mma8452qModule.configureFreeFallDetection().commit();
                                mma8452qModule.enableMovementDetection(Mma8452qAccelerometer.MovementType.FREE_FALL);
                            } else {
                                mma8452qModule.configureMotionDetection().setAxes(Mma8452qAccelerometer.Axis.values()).commit();
                                mma8452qModule.enableMovementDetection(Mma8452qAccelerometer.MovementType.MOTION);
                            }
                        } else {
                            ((TextView) view.findViewById(R.id.textView8)).setText("Not yet supported");
                        }
                    }
                    if (checkboxes.get(CheckBoxName.SAMPLING).isChecked()) {
                        accelModule.setOutputDataRate(50.f);
                        accelModule.setAxisSamplingRange(4.f);
                        accelModule.enableAxisSampling();
                        accelConfig.initialize();
                        start= 0;
                    }
                    accelModule.start();
                    
                    for(CheckBox box: checkboxes.values()) {
                        box.setEnabled(false);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentManager fm = getActivity().getSupportFragmentManager();
                final AccelerometerSettings dialog= new AccelerometerSettings();
            
                dialog.show(fm, "accelerometer_settings");
            }
        });
        view.findViewById(R.id.textView10).setOnClickListener(new OnClickListener() {
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
        view.findViewById(R.id.textView11).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accelConfig.polledBytes() == null) {
                    Toast.makeText(getActivity(), R.string.error_no_accel_data, Toast.LENGTH_SHORT).show();
                } else {
                    writeDataToFile();

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Logged Accelerometer Data");

                    File file = getActivity().getFileStreamPath(dataFilename);
                    Uri uri = FileProvider.getUriForFile(getActivity(),
                            "com.mbientlab.metawear.app.fileprovider", file);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(intent, "Send email..."));
                }
            }
        });
    }
    
    private void writeDataToFile() {
        dataFilename= String.format(Locale.US, "metawear_accelerometer_data-%s-%s.csv", "50Hz", "4g");
        try {
            FileOutputStream fos= getActivity().openFileOutput(dataFilename, Context.MODE_PRIVATE);
            fos.write(CSV_HEADER.getBytes());

            for (float[] dataBytes : accelConfig.polledBytes()) {
                fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f%n", dataBytes[0],
                        dataBytes[1], dataBytes[2], dataBytes[3]).getBytes());
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
