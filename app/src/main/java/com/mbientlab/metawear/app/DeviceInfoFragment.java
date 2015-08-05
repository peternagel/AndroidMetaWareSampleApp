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

import com.mbientlab.metawear.AsyncOperation.CompletionHandler;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.DeviceInformation;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Switch;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author etsai
 *
 */
public class DeviceInfoFragment extends ModuleFragment {
    private Debug debugController= null;
    private TextView switchView;

    private final CompletionHandler<DeviceInformation> deviceInfoHandler= new CompletionHandler<DeviceInformation>() {
        @Override
        public void success(DeviceInformation result) {
            ((TextView) getView().findViewById(R.id.manufacturer_name)).setText(result.manufacturer());
            ((TextView) getView().findViewById(R.id.serial_number)).setText(result.serialNumber());
            ((TextView) getView().findViewById(R.id.firmware_version)).setText(result.firmwareRevision());
            ((TextView) getView().findViewById(R.id.hardware_version)).setText(result.hardwareRevision());
        }

        @Override
        public void failure(Throwable error) {
            Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_info, container, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        switchView= ((TextView) view.findViewById(R.id.mechanical_switch));
        view.findViewById(R.id.textView9).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    if (debugController != null) {
                        debugController.resetDevice();
                        Toast.makeText(getActivity(), R.string.text_reset, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(), "Debug module not supported", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.textView1).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    mwMnger.getCurrentController().readBatteryLevel().onComplete(new CompletionHandler<Byte>() {
                        @Override
                        public void success(final Byte result) {
                            ((TextView) view.findViewById(R.id.battery_level)).setText(String.format("%d", result));
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        view.findViewById(R.id.textView2).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    mwMnger.getCurrentController().readRssi().onComplete(new CompletionHandler<Integer>() {
                        @Override
                        public void success(final Integer result) {
                            ((TextView) view.findViewById(R.id.remote_rssi)).setText(String.format("%d", result));
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addRoute(final Switch switchController) {
        switchController.routeData().fromSensor().stream("switch_stream").commit().onComplete(new CompletionHandler<RouteManager>() {
            @Override
            public void success(RouteManager result) {
                result.subscribe("switch_stream", new RouteManager.MessageHandler() {
                    @Override
                    public void process(final Message message) {
                        if (isVisible()) {
                            if (message.getData(Byte.class) != 0) {
                                switchView.setText("Released");
                            } else {
                                switchView.setText("Pressed");
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void connected(MetaWearBoard currBoard) {
        currBoard.readDeviceInformation().onComplete(deviceInfoHandler);
        currBoard.removeRoutes();

        try {
            debugController = currBoard.getModule(Debug.class);

            addRoute(currBoard.getModule(Switch.class));
        } catch (UnsupportedModuleException e) {
            Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void disconnected() {

    }
}
