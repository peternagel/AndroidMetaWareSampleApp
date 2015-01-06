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
package com.mbientlab.metawear.app.popup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.mbientlab.metawear.app.R;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * @author etsai
 *
 */
public class DataPlotFragment extends DialogFragment {
    private final int[] chartColors= new int[] { Color.rgb(255, 0, 0), 
            Color.rgb(0, 255, 0), Color.rgb(0, 0, 255), Color.rgb(0, 0, 0)};
    private final HashMap<String, GraphViewDataInterface[]> dataSeries= new HashMap<>();
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_plot, container);
    }
    
    //
    
    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {
        
        GraphView graph= new LineGraphView(getActivity(), "");
        graph.setShowHorizontalLabels(false);
        graph.setScrollable(true);
        graph.setScalable(true);
        graph.setShowLegend(true);
        
        int colorIndex= 0;
        for(Entry<String, GraphViewDataInterface[]> data: dataSeries.entrySet()) {
            graph.addSeries(new GraphViewSeries(data.getKey(), 
                    new GraphViewSeriesStyle(chartColors[colorIndex], 5), 
                    data.getValue()));
            colorIndex++;
        }
        ((LinearLayout) view.findViewById(R.id.data_plot)).addView(graph);  
    }
    
    public void addDataSeries(String legendTitle, final Collection<GraphViewDataInterface> graphData) {
        GraphViewDataInterface[] graphDataAsArray= new GraphViewDataInterface[graphData.size()];
        graphData.toArray(graphDataAsArray);
        
        dataSeries.put(legendTitle, graphDataAsArray);
    }
}
