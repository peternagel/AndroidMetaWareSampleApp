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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Haptic;

/**
 * Created by etsai on 8/24/2015.
 */
public class HapticFragment extends ModuleFragmentBase {
    private Haptic hapticModule;

    public HapticFragment() {
        super("Haptic");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        View v= inflater.inflate(R.layout.fragment_haptic, container, false);

        final EditText dutyCycleText= (EditText) v.findViewById(R.id.duty_cycle_value),
                pulseWidthText= (EditText) v.findViewById(R.id.pulse_width_value);

        v.findViewById(R.id.haptic_start_motor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    hapticModule.startMotor(Float.valueOf(dutyCycleText.getText().toString()),
                            Short.valueOf(pulseWidthText.getText().toString()));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        v.findViewById(R.id.haptic_start_buzzer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    hapticModule.startBuzzer(Short.valueOf(pulseWidthText.getText().toString()));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        v.findViewById(R.id.duty_cycle_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.simple_list_entry, container, false);
                ((TextView) dialogLayout.findViewById(R.id.list_entry_name)).setText(R.string.config_desc_haptic_duty_cycle);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.config_name_haptic_duty_cycle)
                        .setPositiveButton(R.string.label_ok, null)
                        .setView(dialogLayout);
                builder.show();
            }
        });
        v.findViewById(R.id.pulse_width_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.simple_list_entry, container, false);
                ((TextView) dialogLayout.findViewById(R.id.list_entry_name)).setText(R.string.config_desc_haptic_pulse_width);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.config_name_haptic_pulse_width)
                        .setPositiveButton(R.string.label_ok, null)
                        .setView(dialogLayout);
                builder.show();
            }
        });

        return v;
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        hapticModule= mwBoard.getModule(Haptic.class);
    }
}
