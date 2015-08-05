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
package com.mbientlab.metawear.app.popup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.LegendRenderer;
import com.mbientlab.metawear.app.AccelerometerFragment.Configuration;
import com.mbientlab.metawear.app.R;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * @author etsai
 *
 */
public class DataPlotFragment extends DialogFragment {
    private final int[] chartColors= new int[] { Color.rgb(255, 0, 0), 
            Color.rgb(0, 255, 0), Color.rgb(0, 0, 255)};
    private final HashMap<String, LineGraphSeries<DataPoint>> dataSeries= new HashMap<>();
    private Configuration accelConfig;
    
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        ///< used suggestion from http://www.techrepublic.com/article/pro-tip-unravel-the-mystery-of-androids-full-screen-dialog-fragments/
        Dialog dialog= getDialog();
        if (dialog != null){
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ///< No title window code from: http://stackoverflow.com/a/15279400
        
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_plot, container);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        final Collection<DataPoint> convertedX= new ArrayList<>(),
                convertedY= new ArrayList<>(), convertedZ= new ArrayList<>();
                
        GraphView graph= (GraphView) view.findViewById(R.id.data_plot);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxY(8.0);
        graph.getViewport().setMinY(-8.0);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.MIDDLE);

        
        for(float[] dataBytes: accelConfig.polledBytes()) {
            convertedX.add(new DataPoint(dataBytes[0], dataBytes[1]));
            convertedY.add(new DataPoint(dataBytes[0], dataBytes[2]));
            convertedZ.add(new DataPoint(dataBytes[0], dataBytes[3]));
            
        }
        
        addDataSeries("X-Axis", convertedX);
        addDataSeries("Y-Axis", convertedY);
        addDataSeries("Z-Axis", convertedZ);
        
        int colorIndex= 0;
        for(Entry<String, LineGraphSeries<DataPoint>> data: dataSeries.entrySet()) {
            data.getValue().setColor(chartColors[colorIndex]);
            data.getValue().setTitle(data.getKey());
            graph.addSeries(data.getValue());
            colorIndex++;
        }
    }
    
    private void addDataSeries(String legendTitle, final Collection<DataPoint> graphData) {
        DataPoint[] graphDataAsArray= new DataPoint[graphData.size()];
        graphData.toArray(graphDataAsArray);
        
        dataSeries.put(legendTitle, new LineGraphSeries(graphDataAsArray));
    }
}
