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
import java.io.FileOutputStream;
import java.io.IOException;
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
    private long start;
    private Accelerometer accelController;
    private Accelerometer.Callbacks mCallback= new Accelerometer.Callbacks() {
        public void receivedDataValue(short x, short y, short z) {
            if (polledData != null) {
                polledData.add(new AxisData(System.currentTimeMillis() - start, x, y, z));
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
        outState.putBoolean("RECORDING", recording);
        outState.putBoolean("PLOT_READY", plotReady);
    }

    private boolean recording= false, plotReady= false;
    private XYPlot accelHistoryPlot;
    private SimpleXYSeries xAxis, yAxis, zAxis;
    private ConcurrentLinkedQueue<AxisData> polledData;

    private FileOutputStream fos= null;
    private static final String FILENAME= "metawear_accelerometer_data_50hz_2g.csv", 
            CSV_HEADER= String.format("time,xAxis,yAxis,zAxis%n");
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            recording= savedInstanceState.getBoolean("RECORDING");
            plotReady= savedInstanceState.getBoolean("PLOT_READY");
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
        
        final Button recordButton= (Button) view.findViewById(R.id.button1);
        recordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    accelController.setComponentConfiguration(Component.DATA, 
                            new byte[] {0, 0, 0x20, 0, 0});
                    
                    polledData= new ConcurrentLinkedQueue<AxisData>();
                    start= System.currentTimeMillis();
                    accelController.enableNotification(Component.DATA);
                    recording= true;
                    recordButton.setText(R.string.label_accelerometer_data_stop);
                } else {
                    accelController.disableNotification(Component.DATA);
                    plotReady= false;
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                ArrayList<Double> xVals= new ArrayList<>(), 
                                        yVals= new ArrayList<>(), zVals= new ArrayList<>();
                                        
                                fos= AccelerometerFragment.this.getActivity().openFileOutput(FILENAME, Context.MODE_WORLD_READABLE);
                                fos.write(CSV_HEADER.getBytes());
                                for(AxisData it: polledData) {
                                    double xG= it.data[0] / 1024.0, yG= it.data[1] / 1024.0, 
                                            zG= it.data[2] / 1024.0;
                                    
                                    fos.write(String.format("%.3f,%.3f,%.3f,%.3f%n", it.tick / 1000.0, xG, yG, zG).getBytes());
                                    
                                    xVals.add(xG);
                                    yVals.add(yG);
                                    zVals.add(zG);
                                }
                                xAxis.setModel(xVals, ArrayFormat.Y_VALS_ONLY);
                                yAxis.setModel(yVals, ArrayFormat.Y_VALS_ONLY);
                                zAxis.setModel(zVals, ArrayFormat.Y_VALS_ONLY);
                                
                                fos.close();
                                fos= null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            plotReady= true;
                        }
                    }).start();
                    recordButton.setText(R.string.label_accelerometer_data_record);
                    recording= false;
                }
            }
        });
        if (recording) {
            recordButton.setText(R.string.label_accelerometer_data_stop);
        } else {
            recordButton.setText(R.string.label_accelerometer_data_record);
        }
        
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording) {
                    Toast.makeText(AccelerometerFragment.this.getActivity(), "Stop data recording before plotting", 
                            Toast.LENGTH_SHORT).show();
                } else if (!plotReady) {
                    Toast.makeText(AccelerometerFragment.this.getActivity(), "Plot data not ready yet, please wait a few more seconds", 
                            Toast.LENGTH_SHORT).show();
                } else if (accelHistoryPlot != null) {
                    accelHistoryPlot.redraw();
                }
            }
        });
        ((Button) view.findViewById(R.id.button3)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
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
