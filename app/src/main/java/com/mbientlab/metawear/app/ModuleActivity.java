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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBleService;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.dfu.DfuActivity;
import no.nordicsemi.android.nrftoolbox.dfu.DfuService;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * @author etsai
 *
 */
public class ModuleActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener, 
        ModuleFragment.MetaWearManager, ServiceConnection, AccelerometerFragment.Configuration {
    private final static UUID METAWEAR_SERVICE= UUID.fromString("326A9000-85CB-9195-D9DD-464CFBBAE75A");
    public static final String EXTRA_BLE_DEVICE= "com.mbientlab.metawear.app.ModuleActivity.EXTRA_BLE_DEVICE";
    public static final String EXTRA_MODEL_NUMBER= "com.mbientlab.metawear.app.ModuleActivity.EXTRA_MODEL_NUMBER";
    protected static final String ARG_ITEM_ID = "item_id";

    private static final int DFU = 0;
    private static final int REQUEST_ENABLE_BT= 1;
    protected static final int START_MODULE_DETAIL= 2;
    protected static BluetoothDevice device;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (!bluetoothManager.getAdapter().isEnabled()) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        
        getApplicationContext().bindService(new Intent(this,MetaWearBleService.class), 
                this, Context.BIND_AUTO_CREATE);
        
        if (savedInstanceState != null) {
            device= savedInstanceState.getParcelable(EXTRA_BLE_DEVICE);
            moduleFragment= (ModuleFragment) getSupportFragmentManager().getFragment(savedInstanceState, "mContent");
            
            tapType= savedInstanceState.getInt(Extra.TAP_TYPE);
            tapAxis= savedInstanceState.getInt(Extra.TAP_AXIS);
            shakeAxis= savedInstanceState.getInt(Extra.SHAKE_AXIS);
            ffMovement= savedInstanceState.getBoolean(Extra.FF_MOVEMENT);
            polledData= (ArrayList<float[]>) savedInstanceState.getSerializable(Extra.POLLED_DATA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case START_MODULE_DETAIL:
        case DFU:
            device= data.getParcelableExtra(EXTRA_BLE_DEVICE);
            if (device != null) {
                currentBoard= serviceBinder.getMetaWearBoard(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
            break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* (non-Javadoc)
     * @see no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment.OnDeviceSelectedListener#onDeviceSelected(android.bluetooth.BluetoothDevice, java.lang.String)
     */
    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {
        if (currentBoard != null && currentBoard.isConnected()) {
            currentBoard.disconnect();
            currentBoard= null;
        }
        
        ModuleActivity.device= device;

        currentBoard= serviceBinder.getMetaWearBoard(device);
        currentBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Toast.makeText(ModuleActivity.this, R.string.text_connected, Toast.LENGTH_SHORT).show();

                if (moduleFragment != null) {
                    moduleFragment.connected(currentBoard);
                }
            }

            @Override
            public void disconnected() {
                Toast.makeText(ModuleActivity.this, R.string.text_lost_connection, Toast.LENGTH_SHORT).show();

                if (moduleFragment != null) {
                    moduleFragment.disconnected();
                }

                if (ModuleActivity.device != null && currentBoard != null) {
                    currentBoard.connect();
                }
            }
        });
        currentBoard.connect();
    }

    /* (non-Javadoc)
     * @see no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment.OnDeviceSelectedListener#onDialogCanceled()
     */
    @Override
    public void onDialogCanceled() {
        // TODO Auto-generated method stub
        
    }
    
    /* (non-Javadoc)
     * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder= (MetaWearBleService.LocalBinder) service;
        serviceBinder.executeOnUiThread();
        if (device != null) {
            currentBoard= serviceBinder.getMetaWearBoard(device);
            if (moduleFragment != null && currentBoard.isConnected()) {
                moduleFragment.connected(currentBoard);
            }
        }
    }

    /* (non-Javadoc)
     * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }
    

    protected MetaWearBleService.LocalBinder serviceBinder;
    protected MetaWearBoard currentBoard;
    protected ModuleFragment moduleFragment;
    protected static HashMap<String, Fragment.SavedState> fragStates= new HashMap<>();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ble_connect:
            final FragmentManager fm = getSupportFragmentManager();
            final ScannerFragment dialog = ScannerFragment.getInstance(ModuleActivity.this, 
                    new UUID[] {METAWEAR_SERVICE, DfuService.DFU_SERVICE_UUID}, true);
            dialog.show(fm, "scan_fragment");
            break;
        case R.id.ble_disconnect:
            if (currentBoard != null) {
                device= null;
                currentBoard.disconnect();
                currentBoard= null;
            }
            break;
        case R.id.metawear_dfu:
            final Intent dfu= new Intent(this, DfuActivity.class);
            dfu.putExtra(EXTRA_BLE_DEVICE, device);
            startActivityForResult(dfu, DFU);
            break;
        case R.id.action_about:
            final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.mw_about_text);
            fragment.show(getSupportFragmentManager(), "help_fragment");
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.bledevice, menu);
        return true;
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (device != null) {
            outState.putParcelable(EXTRA_BLE_DEVICE, device);
        }
        if (moduleFragment != null) {
            getSupportFragmentManager().putFragment(outState, "mContent", moduleFragment);
        }
        
        outState.putInt(Extra.TAP_TYPE, tapType);
        outState.putInt(Extra.TAP_AXIS, tapAxis);
        outState.putInt(Extra.SHAKE_AXIS, shakeAxis);
        outState.putBoolean(Extra.FF_MOVEMENT, ffMovement);
        outState.putSerializable(Extra.POLLED_DATA, polledData);
    }
    @Override
    public MetaWearBoard getCurrentController() {
        return currentBoard;
    }
    
    @Override
    public boolean hasController() {
        return currentBoard != null;
    }
    

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.ModuleFragment.MetaWearManager#controllerReady()
     */
    @Override
    public boolean controllerReady() {
        return hasController() && currentBoard.isConnected();
    }

    private class Extra {
        public static final String TAP_TYPE= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.TAP_TYPE";
        public static final String TAP_AXIS= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.TAP_AXIS";
        public static final String SHAKE_AXIS= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.SHAKE_AXIS";
        public static final String FF_MOVEMENT= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.FF_MOVEMENT";
        public static final String POLLED_DATA= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.POLLED_DATA";
    }

    private ArrayList<float[]> polledData;
    private int tapType= 0, tapAxis= 2, shakeAxis= 0;
    private boolean ffMovement= true;

    @Override
    public int tapTypePos() { return tapType; }
    @Override
    public int tapAxisPos() { return tapAxis; }
    @Override
    public int movementPos() { return ffMovement ? 0 : 1; }
    @Override
    public int shakeAxisPos() { return shakeAxis; }

    @Override
    public void modifyTapType(int newIndex) {
        tapType= newIndex;
    }
    @Override
    public void modifyTapAxis(int newIndex) {
        tapAxis= newIndex;
    }
    @Override
    public void modifyShakeAxis(int newIndex) {
        shakeAxis= newIndex;
    }
    @Override
    public void modifyMovementType(int newIndex) {
        ffMovement= newIndex == 0;
    }

    @Override
    public Collection<float[]> polledBytes() {
        return polledData;
    }


    @Override
    public void initialize() {
        polledData= new ArrayList<>();
    }
}
