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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.SimpleXYSeries.ArrayFormat;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.Component;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

/**
 * @author etsai
 *
 */
public class AccelerometerFragment extends ModuleFragment {
    private Accelerometer accelController;
    private Accelerometer.Callbacks mCallback= new Accelerometer.Callbacks() {
        public void receivedDataValue(short x, short y, short z) {
            final float xG= (float)x / 1024, yG= (float)y / 1024, zG= (float)z / 1024;
            if (fos != null) {
                try {
                    fos.write(String.format("%1$.3f,%1$.3f,%1$.3f%n", xG, yG, zG).getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (xValues != null) {
                xValues.add(xG);
                yValues.add(yG);
                zValues.add(zG);
            }
        }
    };
    private HashMap<Integer, Boolean> values= new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accelerometer, container, false);
    }
    
    @Override
    public void onDestroy() {
        mwController.removeModuleCallback(mCallback);
        super.onDestroy();
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        accelController= (Accelerometer)this.mwController.getModuleController(Module.ACCELEROMETER);
        this.mwController.addModuleCallback(mCallback);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("STATE_VALUES", values);
    }

    private boolean recording= false;
    private XYPlot accelHistoryPlot;
    private SimpleXYSeries xAxis, yAxis, zAxis;
    private ConcurrentLinkedQueue<Float> xValues, yValues, zValues;

    private FileOutputStream fos= null;
    private static final String FILENAME= "metawear_accelerometer_data_50hz_2g.csv";
    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            values= (HashMap<Integer, Boolean>) savedInstanceState.getSerializable("STATE_VALUES");
        }
        
        xAxis= new SimpleXYSeries("X Axis");
        xAxis.useImplicitXVals();
        yAxis= new SimpleXYSeries("Y Axis");
        yAxis.useImplicitXVals();
        zAxis= new SimpleXYSeries("Z Axis");
        zAxis.useImplicitXVals();
        
        accelHistoryPlot= (XYPlot) view.findViewById(R.id.aprHistoryPlot);
        accelHistoryPlot.addSeries(xAxis, new LineAndPointFormatter(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, null));
        accelHistoryPlot.addSeries(yAxis, new LineAndPointFormatter(Color.GREEN, Color.TRANSPARENT, Color.TRANSPARENT, null));
        accelHistoryPlot.addSeries(zAxis, new LineAndPointFormatter(Color.BLUE, Color.TRANSPARENT, Color.TRANSPARENT, null));
        accelHistoryPlot.setTicksPerRangeLabel(3);
        accelHistoryPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
        accelHistoryPlot.setRangeLabel("Measured g's");
        accelHistoryPlot.getRangeLabelWidget().pack();
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    accelController.setComponentConfiguration(Component.DATA, 
                            new byte[] {0, 0, 0x20, 0, 0});
                    accelController.enableNotification(Component.DATA);
                    xValues= new ConcurrentLinkedQueue<Float>();
                    yValues= new ConcurrentLinkedQueue<Float>();
                    zValues= new ConcurrentLinkedQueue<Float>();
                    try {
                        fos = AccelerometerFragment.this.getActivity().openFileOutput(FILENAME, Context.MODE_WORLD_READABLE);
                    } catch (FileNotFoundException e) {
                    //TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    recording= true;
                    ((Button)v).setText(R.string.label_accelerometer_data_stop);
                } else {
                    accelController.disableNotification(Component.DATA);
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                        fos= null;
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    ((Button)v).setText(R.string.label_accelerometer_data_record);
                    recording= false;
                }
            }
        });
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    Toast.makeText(AccelerometerFragment.this.getActivity(), "Stop data recording before plotting", 
                            Toast.LENGTH_SHORT).show();
                } else if (accelHistoryPlot != null) {
                    xAxis.setModel(new ArrayList<Float>(xValues), ArrayFormat.Y_VALS_ONLY);
                    yAxis.setModel(new ArrayList<Float>(yValues), ArrayFormat.Y_VALS_ONLY);
                    zAxis.setModel(new ArrayList<Float>(zValues), ArrayFormat.Y_VALS_ONLY);
                    accelHistoryPlot.redraw();
                }
            }
        });
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"eric@mbientlab.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "subject here");
                intent.putExtra(Intent.EXTRA_TEXT, "body text");
                File file = AccelerometerFragment.this.getActivity().getFileStreamPath(FILENAME);
                
                if (!file.exists()) {
                    Toast.makeText(AccelerometerFragment.this.getActivity(), "Record data before emailing", 
                            Toast.LENGTH_SHORT).show();
                } else if (recording) {
                    Toast.makeText(AccelerometerFragment.this.getActivity(), "Stop recording before emailing the data", 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Uri uri = Uri.fromFile(file);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(intent, "Send email..."));
                }
            }
        });
    }
}
