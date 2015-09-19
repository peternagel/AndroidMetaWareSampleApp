/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.mbientlab.metawear.RouteManager;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by etsai on 8/19/2015.
 */
public abstract class SensorFragment extends ModuleFragmentBase {
    private static final float FPS= 30.f;
    private static final long UPDATE_PERIOD= (long) ((1 / FPS) * 1000L);

    protected final ArrayList<String> chartXValues= new ArrayList<>();
    protected ViewGroup fragContainer;
    protected LineChart chart;
    protected int sampleCount;

    protected String chartDescription;
    protected float min, max;

    protected RouteManager streamRouteManager= null;
    private final int layoutId;

    private final Runnable updateChartTask= new Runnable() {
        @Override
        public void run() {
            chart.notifyDataSetChanged();

            moveViewToLast();

            chartHandler.postDelayed(updateChartTask, UPDATE_PERIOD);
        }
    };
    private final Handler chartHandler= new Handler();

    protected SensorFragment(String sensor, int layoutId, float min, float max) {
        super(sensor);
        this.layoutId= layoutId;
        this.min= min;
        this.max= max;
    }

    private void moveViewToLast() {
        chart.setVisibleXRangeMaximum(120);
        if (sampleCount > 120) {
            chart.moveViewToX(sampleCount - 121);
        } else {
            chart.moveViewToX(sampleCount);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragContainer= container;
        setRetainInstance(true);
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = (LineChart) view.findViewById(R.id.data_chart);

        initializeChart();
        resetData(false);
        chart.invalidate();

        view.findViewById(R.id.data_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chart.resetTracking();
                chart.clear();
                resetData(true);
                chart.invalidate();
            }
        });
        ((Switch) view.findViewById(R.id.sample_control)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    moveViewToLast();
                    setup();
                    chartHandler.postDelayed(updateChartTask, UPDATE_PERIOD);
                } else {
                    chart.setVisibleXRangeMaximum(sampleCount);
                    clean();
                    if (streamRouteManager != null) {
                        streamRouteManager.remove();
                        streamRouteManager= null;
                    }
                    chartHandler.removeCallbacks(updateChartTask);
                }
            }
        });

        view.findViewById(R.id.data_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = saveData();

                if (filename != null) {
                    File dataFile = getActivity().getFileStreamPath(filename);
                    Uri contentUri = FileProvider.getUriForFile(getActivity(), "com.mbientlab.metawear.app.fileprovider", dataFile);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, filename);
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(intent, "Saving Data"));
                }
            }
        });
    }

    protected void initializeChart() {
        ///< configure axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setStartAtZero(false);
        leftAxis.setAxisMaxValue(max);
        leftAxis.setAxisMinValue(min);
        chart.getAxisRight().setEnabled(false);

        chart.setDescription(chartDescription);
    }

    protected abstract void setup();
    protected abstract void clean();
    protected abstract String saveData();
    protected abstract void resetData(boolean clearData);
}
