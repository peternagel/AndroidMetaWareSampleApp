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

import com.mbientlab.metawear.api.GATT;
import com.mbientlab.metawear.api.MetaWearBleService;
import com.mbientlab.metawear.api.MetaWearController;

import no.nordicsemi.android.nrftoolbox.AppHelpFragment;
import no.nordicsemi.android.nrftoolbox.dfu.DfuActivity;
import no.nordicsemi.android.nrftoolbox.dfu.DfuService;
import no.nordicsemi.android.nrftoolbox.scanner.ScannerFragment;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
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
    public static final String EXTRA_BLE_DEVICE= 
            "com.mbientlab.metawear.app.ModuleActivity.EXTRA_BLE_DEVICE";
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
            device= (BluetoothDevice) savedInstanceState.getParcelable(EXTRA_BLE_DEVICE);
            moduleFragment= (ModuleFragment) getSupportFragmentManager().getFragment(savedInstanceState, "mContent");
            
            tapType= savedInstanceState.getInt(Extra.TAP_TYPE);
            tapAxis= savedInstanceState.getInt(Extra.TAP_AXIS);
            shakeAxis= savedInstanceState.getInt(Extra.SHAKE_AXIS);
            dataRange= savedInstanceState.getInt(Extra.DATA_RANGE);
            samplingRate= savedInstanceState.getInt(Extra.SAMPLING_RATE);
            ffMovement= savedInstanceState.getBoolean(Extra.FF_MOVEMENT);
            newFirmware= savedInstanceState.getBoolean(Extra.NEW_FIRMWARE);
            samplingConfigBytes= savedInstanceState.getByteArray(Extra.SAMPLING_CONFIG_BYTES);
            polledData= (ArrayList<byte []>) savedInstanceState.getSerializable(Extra.POLLED_DATA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case START_MODULE_DETAIL:
        case DFU:
            device= data.getParcelableExtra(EXTRA_BLE_DEVICE);
            if (device != null) {
                mwController= mwService.getMetaWearController(device);
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
        if (mwController != null && mwController.isConnected()) {
            mwController.close(true);
            mwController= null;
        }
        
        ModuleActivity.device= device;
        
        mwController= mwService.getMetaWearController(device);
        mwController.addDeviceCallback(new MetaWearController.DeviceCallbacks() {
            @Override
            public void connected() {
                Toast.makeText(ModuleActivity.this, R.string.text_connected, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void disconnected() {
                Toast.makeText(ModuleActivity.this, R.string.text_lost_connection, Toast.LENGTH_SHORT).show();
                if (ModuleActivity.device != null && mwController != null) {
                    mwController.reconnect(false);
                }
            }
        });
        
        if (moduleFragment != null) {
            moduleFragment.controllerReady(mwController);
        }
        mwController.connect();
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
        mwService= ((MetaWearBleService.LocalBinder) service).getService();
        if (device != null) {
            mwController= mwService.getMetaWearController(device);
            if (moduleFragment != null) {
                moduleFragment.controllerReady(mwController);
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
    
    
    private final BroadcastReceiver metaWearUpdateReceiver= MetaWearBleService.getMetaWearBroadcastReceiver();
    protected MetaWearBleService mwService;
    protected MetaWearController mwController;
    protected ModuleFragment moduleFragment;
    protected static HashMap<String, Fragment.SavedState> fragStates= new HashMap<>();
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(metaWearUpdateReceiver, MetaWearBleService.getMetaWearIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(metaWearUpdateReceiver);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.ble_connect:
            final FragmentManager fm = getSupportFragmentManager();
            final ScannerFragment dialog = ScannerFragment.getInstance(ModuleActivity.this, 
                    new UUID[] {GATT.GATTService.METAWEAR.uuid(), DfuService.DFU_SERVICE_UUID}, true);
            dialog.show(fm, "scan_fragment");
            break;
        case R.id.ble_disconnect:
            if (mwController != null) {
                device= null;
                mwController.setRetainState(false);
                mwController.close(true);
                mwController= null;
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
        outState.putInt(Extra.DATA_RANGE, dataRange);
        outState.putInt(Extra.SAMPLING_RATE, samplingRate);
        outState.putBoolean(Extra.FF_MOVEMENT, ffMovement);
        outState.putBoolean(Extra.NEW_FIRMWARE, newFirmware);
        outState.putByteArray(Extra.SAMPLING_CONFIG_BYTES, samplingConfigBytes);
        outState.putSerializable(Extra.POLLED_DATA, polledData);
    }
    @Override
    public MetaWearController getCurrentController() {
        return mwController;
    }
    
    @Override
    public boolean hasController() {
        return mwController != null;
    }
    

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.ModuleFragment.MetaWearManager#controllerReady()
     */
    @Override
    public boolean controllerReady() {
        return hasController() && mwController.isConnected();
    }

    private class Extra {
        public static final String TAP_TYPE= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.TAP_TYPE";
        public static final String TAP_AXIS= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.TAP_AXIS";
        public static final String SHAKE_AXIS= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.SHAKE_AXIS";
        public static final String DATA_RANGE= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.DATA_RANGE";
        public static final String SAMPLING_RATE= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.SAMPLING_RATE";
        public static final String FF_MOVEMENT= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.FF_MOVEMENT";
        public static final String NEW_FIRMWARE= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.NEW_FIRMWARE";
        public static final String SAMPLING_CONFIG_BYTES= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.SAMPLING_CONFIG_BYTES";
        public static final String POLLED_DATA= 
                "com.mbientlab.metawear.app.ModuleActivity.Extra.POLLED_DATA";
    }
    
    private ArrayList<byte []> polledData;
    private byte[] samplingConfigBytes;
    private int tapType= 0, tapAxis= 2, shakeAxis= 0, dataRange= 2, samplingRate= 3;
    private boolean ffMovement= true, newFirmware= true;
    
    public int tapTypePos() { return tapType; }
    public int tapAxisPos() { return tapAxis; }
    public int movementPos() { return ffMovement ? 0 : 1; }
    public int shakeAxisPos() { return shakeAxis; }
    public int fsrPos() { return dataRange; }
    public int odrPos() { return samplingRate; }
    public int firmwarePos() { return newFirmware ? 0 : 1; }

    public void modifyTapType(int newIndex) {
        tapType= newIndex;
    }
    public void modifyTapAxis(int newIndex) {
        tapAxis= newIndex;
    }
    public void modifyShakeAxis(int newIndex) {
        shakeAxis= newIndex;
    }
    public void modifyMovementType(int newIndex) {
        ffMovement= newIndex == 0;
    }
    public void modifyDataRange(int newIndex) {
        dataRange= newIndex;
    }
    public void modifySamplingRate(int newIndex) {
        samplingRate= newIndex;
    }
    public void modifyFirmwareVersion(int newIndex) {
        newFirmware= newIndex == 0;
    }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.AccelerometerFragment.SamplingData#polledBytes()
     */
    @Override
    public Collection<byte[]> polledBytes() {
        return polledData;
    }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.AccelerometerFragment.SamplingData#getSamplingConfig()
     */
    @Override
    public byte[] getSamplingConfig() {
        return samplingConfigBytes;
    }

    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.AccelerometerFragment.SamplingData#setSamplingConfig(byte[])
     */
    @Override
    public void initialize(byte[] config) {
        samplingConfigBytes= config;
        polledData= new ArrayList<>();
    }
}
