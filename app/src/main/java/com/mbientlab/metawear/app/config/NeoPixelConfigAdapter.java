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

package com.mbientlab.metawear.app.config;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.mbientlab.metawear.app.R;

/**
 * Created by etsai on 8/23/2015.
 */
public class NeoPixelConfigAdapter extends ArrayAdapter<NeoPixelConfig> {
    public NeoPixelConfigAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        final NeoPixelConfig current= getItem(position);


            if (current.isControl) {
                convertView= LayoutInflater.from(getContext()).inflate(R.layout.layout_two_button, parent, false);

                viewHolder= new ViewHolder();
                viewHolder.leftButton= (Button) convertView.findViewById(R.id.left_button);
                viewHolder.leftButton.setText(current.name);
                viewHolder.rightButton= (Button) convertView.findViewById(R.id.right_button);
                viewHolder.rightButton.setText(current.rightButton);
            } else {
                convertView= LayoutInflater.from(getContext()).inflate(R.layout.sensor_config_entry, parent, false);

                viewHolder= new ViewHolder();
                viewHolder.configName= (TextView) convertView.findViewById(R.id.config_name);
                viewHolder.configValue= (TextView) convertView.findViewById(R.id.config_value);
                viewHolder.configEdit= (TextView) convertView.findViewById(R.id.edit_config_value);
            }



        if (current.isControl) {
            current.setup(convertView);
        } else {
            final TextView configEditValue = viewHolder.configValue;

            viewHolder.configName.setText(current.name);
            viewHolder.configValue.setText(current.value.toString());
            viewHolder.configEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View dialogLayout = LayoutInflater.from(getContext()).inflate(current.layoutResId, parent, false);
                    ((TextView) dialogLayout.findViewById(R.id.config_description)).setText(current.description);
                    current.setup(dialogLayout);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(current.name)
                            .setPositiveButton(R.string.label_commit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    current.changeCommitted();
                                    configEditValue.setText(current.value.toString());
                                }
                            })
                            .setNegativeButton(R.string.label_cancel, null);

                    builder.setView(dialogLayout);
                    builder.show();
                }
            });
        }

        return convertView;
    }

    private class ViewHolder {
        public TextView configName, configValue, configEdit;
        public Button leftButton, rightButton;
    }
}
