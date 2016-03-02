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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard.DeviceInformation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.help.HelpOptionAdapter;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.Switch;

/**
 * Created by etsai on 8/22/2015.
 */
public class HomeFragment extends ModuleFragmentBase {
    private Led ledModule;

    public static class MetaBootWarningFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.title_warning)
                    .setPositiveButton(R.string.label_ok, null)
                    .setCancelable(false)
                    .setMessage(R.string.message_metaboot)
                    .create();
        }
    }

    public HomeFragment() {
        super(R.string.navigation_fragment_home);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.led_red_on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configureChannel(ledModule.configureColorChannel(Led.ColorChannel.RED));
                ledModule.play(true);
            }
        });
        view.findViewById(R.id.led_green_on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configureChannel(ledModule.configureColorChannel(Led.ColorChannel.GREEN));
                ledModule.play(true);
            }
        });
        view.findViewById(R.id.led_blue_on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configureChannel(ledModule.configureColorChannel(Led.ColorChannel.BLUE));
                ledModule.play(true);
            }
        });
        view.findViewById(R.id.led_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ledModule.stop(true);
            }
        });
        view.findViewById(R.id.board_rssi_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mwBoard.readRssi().onComplete(new AsyncOperation.CompletionHandler<Integer>() {
                    @Override
                    public void success(Integer result) {
                        ((TextView) view.findViewById(R.id.board_rssi_value)).setText(String.format("%d dBm", result));
                    }
                });
            }
        });
        view.findViewById(R.id.board_battery_level_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mwBoard.readBatteryLevel().onComplete(new AsyncOperation.CompletionHandler<Byte>() {
                    @Override
                    public void success(Byte result) {
                        ((TextView) view.findViewById(R.id.board_battery_level_value)).setText(String.format("%d", result));
                    }

                    @Override
                    public void failure(Throwable error) {
                        ((TextView) view.findViewById(R.id.board_battery_level_value)).setText(R.string.label_sodium);
                    }
                });
            }
        });
        view.findViewById(R.id.update_firmware).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mwBoard.checkForFirmwareUpdate().onComplete(new AsyncOperation.CompletionHandler<Boolean>() {
                    @Override
                    public void success(Boolean result) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        if (!result) {
                            setupDfuDialog(builder, R.string.message_dfu_latest);
                        } else {
                            setupDfuDialog(builder, R.string.message_dfu_accept);
                        }

                        builder.show();
                    }
                });

            }
        });
    }

    private void setupDfuDialog(AlertDialog.Builder builder, int msgResId) {
        builder.setTitle(R.string.title_firmware_update)
                .setPositiveButton(R.string.label_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        fragBus.initiateDfu(null);
                    }
                })
                .setNegativeButton(R.string.label_no, null)
                .setCancelable(false)
                .setMessage(msgResId);
    }

    @Override
    protected void boardReady() throws UnsupportedModuleException {
        setupFragment(getView());
    }

    @Override
    protected void fillHelpOptionAdapter(HelpOptionAdapter adapter) {

    }

    @Override
    public void reconnected() {
        setupFragment(getView());
    }

    private void configureChannel(Led.ColorChannelEditor editor) {
        final short PULSE_WIDTH= 1000;
        editor.setHighIntensity((byte) 31).setLowIntensity((byte) 31)
                .setHighTime((short) (PULSE_WIDTH >> 1)).setPulseDuration(PULSE_WIDTH)
                .setRepeatCount((byte) -1).commit();
    }

    private void setupFragment(final View v) {
        final String METABOOT_WARNING_TAG= "metaboot_warning_tag", SWITCH_STREAM= "switch_stream";

        if (!mwBoard.isConnected()) {
            return;
        }

        if (mwBoard.inMetaBootMode()) {
            if (getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG) == null) {
                new MetaBootWarningFragment().show(getFragmentManager(), METABOOT_WARNING_TAG);
            }
        } else {
            DialogFragment metabootWarning= (DialogFragment) getFragmentManager().findFragmentByTag(METABOOT_WARNING_TAG);
            if (metabootWarning != null) {
                metabootWarning.dismiss();
            }
        }

        mwBoard.readDeviceInformation().onComplete(new AsyncOperation.CompletionHandler<DeviceInformation>() {
            @Override
            public void success(DeviceInformation result) {
                ((TextView) v.findViewById(R.id.manufacturer_value)).setText(result.manufacturer());
                ((TextView) v.findViewById(R.id.model_number_value)).setText(result.modelNumber());
                ((TextView) v.findViewById(R.id.serial_number_value)).setText(result.serialNumber());
                ((TextView) v.findViewById(R.id.firmware_revision_value)).setText(result.firmwareRevision());
                ((TextView) v.findViewById(R.id.hardware_revision_value)).setText(result.hardwareRevision());
                ((TextView) v.findViewById(R.id.device_mac_address_value)).setText(mwBoard.getMacAddress());
            }
        });

        try {
            Switch switchModule = mwBoard.getModule(Switch.class);
            switchModule.routeData().fromSensor().stream(SWITCH_STREAM).commit()
                    .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                        @Override
                        public void success(RouteManager result) {
                            result.subscribe(SWITCH_STREAM, new RouteManager.MessageHandler() {
                                @Override
                                public void process(Message msg) {
                                    RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.switch_radio_group);

                                    if (msg.getData(Boolean.class)) {
                                        radioGroup.check(R.id.switch_radio_pressed);
                                        v.findViewById(R.id.switch_radio_pressed).setEnabled(true);
                                        v.findViewById(R.id.switch_radio_released).setEnabled(false);
                                    } else {
                                        radioGroup.check(R.id.switch_radio_released);
                                        v.findViewById(R.id.switch_radio_released).setEnabled(true);
                                        v.findViewById(R.id.switch_radio_pressed).setEnabled(false);
                                    }
                                }
                            });
                        }
                    });
        } catch (UnsupportedModuleException ignored) {
        }

        int[] ledResIds= new int[] {R.id.led_stop, R.id.led_red_on, R.id.led_green_on, R.id.led_blue_on};

        try {
            ledModule = mwBoard.getModule(Led.class);

            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(true);
            }
        } catch (UnsupportedModuleException e) {
            for(int id: ledResIds) {
                v.findViewById(id).setEnabled(false);
            }
        }
    }
}
