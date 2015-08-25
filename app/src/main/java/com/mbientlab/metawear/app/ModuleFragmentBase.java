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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;

import java.util.Locale;

/**
 * Created by etsai on 8/22/2015.
 */
public abstract class ModuleFragmentBase extends Fragment implements ServiceConnection {
    public interface FragmentBus {
        BluetoothDevice getBtDevice();
        void resetConnectionStateHandler();
    }

    protected MetaWearBoard mwBoard;
    protected FragmentBus fragBus;
    private boolean isSupported= true;
    protected String sensor;

    protected abstract void boardReady() throws UnsupportedModuleException;

    public ModuleFragmentBase(String sensor) {
        this.sensor= sensor;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof FragmentBus)) {
            throw new ClassCastException(String.format(Locale.US, "%s %s", activity.toString(),
                    activity.getString(R.string.error_fragment_bus)));
        }

        fragBus= (FragmentBus) activity;
        activity.getApplicationContext().bindService(new Intent(activity, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isSupported) {
            enableDisableViewGroup((ViewGroup) getView(), false);
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mwBoard= ((MetaWearBleService.LocalBinder) iBinder).getMetaWearBoard(fragBus.getBtDevice());
        try {
            boardReady();
        } catch (UnsupportedModuleException e) {
            unsupportedModule();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public void reconnected() {

    }

    private void unsupportedModule() {
        isSupported= false;
        new AlertDialog.Builder(getActivity()).setTitle(R.string.title_error)
                .setMessage(String.format("%s %s", sensor, getActivity().getString(R.string.error_unsupported_module)))
                .setCancelable(false)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        enableDisableViewGroup((ViewGroup) getView(), false);
                    }
                })
                .create()
                .show();
    }

    ///<  Taken from: http://stackoverflow.com/a/20742032/4872841
    protected void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }
}
