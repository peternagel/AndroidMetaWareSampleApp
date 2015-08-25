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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.config.NeoPixelConfig;
import com.mbientlab.metawear.app.config.NeoPixelConfigAdapter;
import com.mbientlab.metawear.module.NeoPixel;
import com.mbientlab.metawear.module.NeoPixel.*;

import java.util.ArrayList;

/**
 * Created by etsai on 8/23/2015.
 */
public class NeoPixelFragment extends ModuleFragmentBase {
    private interface PatternProgrammer {
        void program();
    }

    private NeoPixel neoPixelModule;
    private NeoPixelConfigAdapter configAdapter;

    private PatternProgrammer[] programmer= new PatternProgrammer[] {
            new PatternProgrammer() {
                @Override
                public void program() {
                    for(byte i= 0; i < nLeds; i++) {
                        neoPixelModule.setPixel(npStrand, i, (byte)0, (byte)-1, (byte)0);
                    }
                }
            },
            new PatternProgrammer() {
                @Override
                public void program() {
                    double delta= 2 * Math.PI / nLeds;

                    for(byte i= 0; i < nLeds; i++) {
                        double step= i * delta;
                        double rRatio= Math.cos(step),
                                gRatio= Math.cos(step + 2*Math.PI/3),
                                bRatio= Math.cos(step + 4*Math.PI/3);
                        neoPixelModule.setPixel(npStrand, i, (byte)((rRatio < 0 ? 0 : rRatio) * 255),
                                (byte)((gRatio < 0 ? 0 : gRatio) * 255),
                                (byte)((bRatio < 0 ? 0 : bRatio) * 255));
                    }
                }
            }
    };
    private RotationDirection direction= RotationDirection.TOWARDS;
    private ColorOrdering ordering= ColorOrdering.MW_WS2811_RGB;
    private StrandSpeed speed= StrandSpeed.SLOW;
    private byte npStrand= 0, dataPin= 0, nLeds= 0;
    private int npStrandIndex= 0, strandSpeedIndex= 0, orderingIndex= 0, directionIndex= 0, patternIndex= 0;
    private short period;

    public NeoPixelFragment() {
        super("NeoPixel");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        configAdapter= new NeoPixelConfigAdapter(getActivity(), R.id.sensor_config_entry_layout);
        configAdapter.setNotifyOnChange(true);
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_neopixel, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ListView) view.findViewById(R.id.neo_pixel_config)).setAdapter(configAdapter);
        final ArrayList<NeoPixelConfig> configSettings= new ArrayList<>();

        final String[] strandIdStringValues= getActivity().getResources().getStringArray(R.array.values_neopixel_strand_id);
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_strand, R.string.config_desc_neopixel_strand,
                npStrand, R.layout.popup_config_spinner) {

            private Spinner npStrandText;

            @Override
            public void setup(View v) {
                npStrandText = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_neopixel_strand_id, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                npStrandText.setAdapter(readModeAdapter);
                npStrandText.setSelection(npStrandIndex);
            }

            @Override
            public void changeCommitted() {
                npStrandIndex = npStrandText.getSelectedItemPosition();
                npStrand = Byte.valueOf(strandIdStringValues[npStrandIndex]);
                value= npStrand;
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_data_pin, R.string.config_desc_neopixel_data_pin,
                dataPin, R.layout.popup_gpio_pin_config) {

            private EditText dataPinText;

            @Override
            public void setup(View v) {
                dataPinText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                dataPinText.setText(String.format("%d", dataPin));
            }

            @Override
            public void changeCommitted() {
                dataPin = Byte.valueOf(dataPinText.getText().toString());
                value= dataPin;
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_n_leds, R.string.config_desc_neopixel_n_leds,
                nLeds, R.layout.popup_gpio_pin_config) {

            private EditText nLedsText;

            @Override
            public void setup(View v) {
                nLedsText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                nLedsText.setText(String.format("%d", nLeds));
            }

            @Override
            public void changeCommitted() {
                nLeds = Byte.valueOf(nLedsText.getText().toString());
                value= nLeds;
            }
        });
        final String[] strandSpeedStringValues= getActivity().getResources().getStringArray(R.array.values_neopixel_strand_speed);
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_strand_speed, R.string.config_desc_neopixel_strand_speed,
                strandSpeedStringValues[strandSpeedIndex], R.layout.popup_config_spinner) {

            private Spinner strandSpeedText;

            @Override
            public void setup(View v) {
                strandSpeedText = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_neopixel_strand_speed, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                strandSpeedText.setAdapter(readModeAdapter);
                strandSpeedText.setSelection(strandSpeedIndex);
            }

            @Override
            public void changeCommitted() {
                strandSpeedIndex = strandSpeedText.getSelectedItemPosition();
                speed= StrandSpeed.values()[strandSpeedIndex];
                value= strandSpeedStringValues[strandSpeedIndex];
            }
        });
        final String[] orderingStringValues= getActivity().getResources().getStringArray(R.array.values_neopixel_color_ordering);
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_color_ordering, R.string.config_desc_neopixel_color_ordering,
                orderingStringValues[orderingIndex], R.layout.popup_config_spinner) {

            private Spinner colorOrderingText;

            @Override
            public void setup(View v) {
                colorOrderingText = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_neopixel_color_ordering, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                colorOrderingText.setAdapter(readModeAdapter);
                colorOrderingText.setSelection(strandSpeedIndex);
            }

            @Override
            public void changeCommitted() {
                orderingIndex = colorOrderingText.getSelectedItemPosition();
                ordering= ColorOrdering.values()[orderingIndex];
                value= orderingStringValues[orderingIndex];
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.label_neopixel_initialize, R.string.label_neopixel_free) {
            @Override
            public void setup(View v) {
                v.findViewById(R.id.left_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.initializeStrand(npStrand, ordering, speed, dataPin, nLeds);
                    }
                });
                v.findViewById(R.id.right_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.deinitializeStrand(npStrand);
                    }
                });
            }

            @Override
            public void changeCommitted() {
            }
        });
        final String[] patternStringValues= getActivity().getResources().getStringArray(R.array.values_neopixel_patterns);
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_color_pattern, R.string.config_desc_neopixel_color_pattern,
                patternStringValues[patternIndex], R.layout.popup_config_spinner) {

            private Spinner patternText;

            @Override
            public void setup(View v) {
                patternText = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_neopixel_patterns, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                patternText.setAdapter(readModeAdapter);
                patternText.setSelection(patternIndex);
            }

            @Override
            public void changeCommitted() {
                patternIndex = patternText.getSelectedItemPosition();
                value= patternStringValues[patternIndex];
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.label_gpio_set_output, R.string.label_gpio_clear_output) {
            @Override
            public void setup(View v) {
                v.findViewById(R.id.left_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.holdStrand(npStrand);
                        programmer[patternIndex].program();
                        neoPixelModule.releaseHold(npStrand);
                    }
                });
                v.findViewById(R.id.right_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.clearStrand(npStrand, (byte) 0, nLeds);
                    }
                });
            }

            @Override
            public void changeCommitted() {
            }
        });
        final String[] directionStringValues= getActivity().getResources().getStringArray(R.array.values_neopixel_rotation_direction);
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_rotation_direction, R.string.config_desc_neopixel_rotation_direction,
                directionStringValues[directionIndex], R.layout.popup_config_spinner) {

            private Spinner rotationDirectionText;

            @Override
            public void setup(View v) {
                rotationDirectionText = (Spinner) v.findViewById(R.id.config_value_list);
                final ArrayAdapter<CharSequence> readModeAdapter = ArrayAdapter.createFromResource(getActivity(),
                        R.array.values_neopixel_rotation_direction, android.R.layout.simple_spinner_item);
                readModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                rotationDirectionText.setAdapter(readModeAdapter);
                rotationDirectionText.setSelection(strandSpeedIndex);
            }

            @Override
            public void changeCommitted() {
                directionIndex = rotationDirectionText.getSelectedItemPosition();
                direction= RotationDirection.values()[directionIndex];
                value= directionStringValues[directionIndex];
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.config_name_neopixel_rotation_period, R.string.config_desc_neopixel_rotation_period,
                period, R.layout.popup_gpio_pin_config) {

            private EditText periodText;

            @Override
            public void setup(View v) {
                periodText = (EditText) v.findViewById(R.id.gpio_pin_edit);
                periodText.setText(String.format("%d", period));
            }

            @Override
            public void changeCommitted() {
                period = Short.valueOf(periodText.getText().toString());
                value= period;
            }
        });
        configSettings.add(new NeoPixelConfig(R.string.label_neopixel_rotate, R.string.label_neopixel_stop) {
            @Override
            public void setup(View v) {
                v.findViewById(R.id.left_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.rotate(npStrand, direction, period);
                    }
                });
                v.findViewById(R.id.right_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        neoPixelModule.stopRotation(npStrand);
                    }
                });
            }

            @Override
            public void changeCommitted() {
            }
        });
        configAdapter.addAll(configSettings);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException{
        neoPixelModule= mwBoard.getModule(NeoPixel.class);
    }
}
