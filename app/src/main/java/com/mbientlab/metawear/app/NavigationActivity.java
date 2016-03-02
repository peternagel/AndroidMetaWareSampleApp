/*
 * Copyright 2014-2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights granted under the terms of a software
 * license agreement between the user who downloaded the software, his/her employer (which must be your
 * employer) and MbientLab Inc, (the "License").  You may not use this Software unless you agree to abide by the
 * terms of the License which can be found at www.mbientlab.com/terms.  The License limits your use, and you
 * acknowledge, that the Software may be modified, copied, and distributed when used in conjunction with an
 * MbientLab Inc, product.  Other than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this Software and/or its documentation for any
 * purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL MBIENTLAB OR ITS LICENSORS BE LIABLE OR
 * OBLIGATED UNDER CONTRACT, NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software, contact MbientLab via email:
 * hello@mbientlab.com.
 */

package com.mbientlab.metawear.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.MetaWearBoard.DfuProgressHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, FragmentBus, LoaderManager.LoaderCallbacks<Cursor> {
    public final static String EXTRA_BT_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.EXTRA_BT_DEVICE";

    private static final int SELECT_FILE_REQ = 1, PERMISSION_REQUEST_READ_STORAGE= 2;
    private static final String EXTRA_URI = "uri";
    private final static String FRAGMENT_KEY= "com.mbientlab.metawear.app.NavigationActivity.FRAGMENT_KEY";
    private final static Map<Integer, Class<? extends ModuleFragmentBase>> FRAGMENT_CLASSES;

    static {
        Map<Integer, Class<? extends ModuleFragmentBase>> tempMap= new LinkedHashMap<>();
        tempMap.put(R.id.nav_home, HomeFragment.class);
        tempMap.put(R.id.nav_accelerometer, AccelerometerFragment.class);
        tempMap.put(R.id.nav_barometer, BarometerFragment.class);
        tempMap.put(R.id.nav_color_detector, ColorDetectorFragment.class);
        tempMap.put(R.id.nav_gpio, GpioFragment.class);
        tempMap.put(R.id.nav_gyro, GyroFragment.class);
        tempMap.put(R.id.nav_haptic, HapticFragment.class);
        tempMap.put(R.id.nav_humidity, HumidityFragment.class);
        tempMap.put(R.id.nav_ibeacon, IBeaconFragment.class);
        tempMap.put(R.id.nav_i2c, I2CFragment.class);
        tempMap.put(R.id.nav_light, AmbientLightFragment.class);
        tempMap.put(R.id.nav_magnetometer, MagnetometerFragment.class);
        tempMap.put(R.id.nav_neopixel, NeoPixelFragment.class);
        tempMap.put(R.id.nav_proximity, ProximityFragment.class);
        tempMap.put(R.id.nav_settings, SettingsFragment.class);
        tempMap.put(R.id.nav_temperature, TemperatureFragment.class);
        FRAGMENT_CLASSES= Collections.unmodifiableMap(tempMap);
    }

    public static class ReconnectDialogFragment extends DialogFragment implements  ServiceConnection {
        private static final String KEY_BLUETOOTH_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.ReconnectDialogFragment.KEY_BLUETOOTH_DEVICE";

        private ProgressDialog reconnectDialog = null;
        private BluetoothDevice btDevice= null;
        private MetaWearBoard currentMwBoard= null;

        public static ReconnectDialogFragment newInstance(BluetoothDevice btDevice) {
            Bundle args= new Bundle();
            args.putParcelable(KEY_BLUETOOTH_DEVICE, btDevice);

            ReconnectDialogFragment newFragment= new ReconnectDialogFragment();
            newFragment.setArguments(args);

            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            btDevice= getArguments().getParcelable(KEY_BLUETOOTH_DEVICE);
            getActivity().getApplicationContext().bindService(new Intent(getActivity(), MetaWearBleService.class), this, BIND_AUTO_CREATE);

            reconnectDialog = new ProgressDialog(getActivity());
            reconnectDialog.setTitle(getString(R.string.title_reconnect_attempt));
            reconnectDialog.setMessage(getString(R.string.message_wait));
            reconnectDialog.setCancelable(false);
            reconnectDialog.setCanceledOnTouchOutside(false);
            reconnectDialog.setIndeterminate(true);
            reconnectDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    currentMwBoard.disconnect();
                    getActivity().finish();
                }
            });

            return reconnectDialog;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            currentMwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }
    public static class DfuProgressFragment extends DialogFragment {
        private ProgressDialog dfuProgress= null;

        public static DfuProgressFragment newInstance(int messageStringId) {
            Bundle bundle= new Bundle();
            bundle.putInt("message_string_id", messageStringId);

            DfuProgressFragment newFragment= new DfuProgressFragment();
            newFragment.setArguments(bundle);
            return newFragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            dfuProgress= new ProgressDialog(getActivity());
            dfuProgress.setTitle(getString(R.string.title_firmware_update));
            dfuProgress.setCancelable(false);
            dfuProgress.setCanceledOnTouchOutside(false);
            dfuProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dfuProgress.setProgress(0);
            dfuProgress.setMax(100);
            dfuProgress.setMessage(getString(getArguments().getInt("message_string_id")));
            return dfuProgress;
        }

        public void updateProgress(int newProgress) {
            if (dfuProgress != null) {
                dfuProgress.setProgress(newProgress);
            }
        }
    }

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    private final Handler taskScheduler = new Handler();
    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;
    private Fragment currentFragment= null;
    private Uri fileStreamUri;

    private final ConnectionStateHandler connectionHandler= new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            ((DialogFragment) getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG)).dismiss();
            ((ModuleFragmentBase) currentFragment).reconnected();
        }

        @Override
        public void disconnected() {
            attemptReconnect();
        }

        @Override
        public void failure(int status, Throwable error) {
            Fragment reconnectFragment= getSupportFragmentManager().findFragmentByTag(RECONNECT_DIALOG_TAG);
            if (reconnectFragment != null) {
                mwBoard.connect();
            } else {
                attemptReconnect();
            }
        }
    };

    private void attemptReconnect() {
        attemptReconnect(0);
    }

    private void attemptReconnect(long delay) {
        ReconnectDialogFragment dialogFragment= ReconnectDialogFragment.newInstance(btDevice);
        dialogFragment.show(getSupportFragmentManager(), RECONNECT_DIALOG_TAG);

        if (delay != 0) {
            taskScheduler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mwBoard.connect();
                }
            }, delay);
        } else {
            mwBoard.connect();
        }
    }

    @Override
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    @Override
    public void resetConnectionStateHandler(long delay) {
        mwBoard.setConnectionStateHandler(connectionHandler);
        attemptReconnect(delay);
    }

    @Override
    public void initiateDfu(final InputStream stream) {
        final String DFU_PROGRESS_FRAGMENT_TAG= "dfu_progress_popup";
        final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final Notification.Builder checkpointNotifyBuilder = new Notification.Builder(NavigationActivity.this).setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOnlyAlertOnce(true).setOngoing(true).setProgress(0, 0, true);
        final Notification.Builder progressNotifyBuilder = new Notification.Builder(NavigationActivity.this).setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOnlyAlertOnce(true).setOngoing(true).setContentTitle(getString(R.string.notify_dfu_uploading));
        final int NOTIFICATION_ID = 1024;

        final DfuProgressHandler handler = new DfuProgressHandler() {
            @Override
            public void reachedCheckpoint(State dfuState) {
                switch (dfuState) {
                    case INITIALIZING:
                        checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_bootloader));
                        break;
                    case STARTING:
                        checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_starting));
                        break;
                    case VALIDATING:
                        checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_validating));
                        break;
                    case DISCONNECTING:
                        checkpointNotifyBuilder.setContentTitle(getString(R.string.notify_dfu_disconnecting));
                        break;
                }

                manager.notify(NOTIFICATION_ID, checkpointNotifyBuilder.build());
            }

            @Override
            public void receivedUploadProgress(int progress) {
                progressNotifyBuilder.setContentText(String.format("%d%%", progress)).setProgress(100, progress, false);
                manager.notify(NOTIFICATION_ID, progressNotifyBuilder.build());
                ((DfuProgressFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).updateProgress(progress);
            }
        };


        taskScheduler.post(new Runnable() {
            @Override
            public void run() {
                DfuProgressFragment.newInstance((stream != null ? R.string.message_manual_dfu : R.string.message_dfu)).show(getSupportFragmentManager(), DFU_PROGRESS_FRAGMENT_TAG);
                (stream != null ? mwBoard.updateFirmware(stream, handler) : mwBoard.updateFirmware(handler)).onComplete(new AsyncOperation.CompletionHandler<Void>() {
                    final Notification.Builder builder = new Notification.Builder(NavigationActivity.this).setOnlyAlertOnce(true)
                            .setOngoing(false).setAutoCancel(true);

                    @Override
                    public void success(Void result) {
                        ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
                        builder.setContentTitle(getString(R.string.notify_dfu_success)).setSmallIcon(android.R.drawable.stat_sys_upload_done);
                        manager.notify(NOTIFICATION_ID, builder.build());

                        Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), R.string.message_dfu_success, Snackbar.LENGTH_LONG).show();
                        resetConnectionStateHandler(5000L);
                    }

                    @Override
                    public void failure(Throwable error) {
                        Log.e("MetaWearApp", "Firmware update failed", error);

                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        ((DialogFragment) getSupportFragmentManager().findFragmentByTag(DFU_PROGRESS_FRAGMENT_TAG)).dismiss();
                        builder.setContentTitle(getString(R.string.notify_dfu_fail)).setSmallIcon(android.R.drawable.ic_dialog_alert)
                                .setContentText(cause.getLocalizedMessage());
                        manager.notify(NOTIFICATION_ID, builder.build());

                        Snackbar.make(NavigationActivity.this.findViewById(R.id.drawer_layout), error.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                        resetConnectionStateHandler(5000L);
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        mwBoard.setConnectionStateHandler(null);
        getApplicationContext().unbindService(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ModuleFragmentBase) currentFragment).showHelpDialog();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.nav_home));
        } else {
            currentFragment= getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_KEY);
        }

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (currentFragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_KEY, currentFragment);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK)
            return;

        fileStreamUri= null;
        switch (requestCode) {
            case SELECT_FILE_REQ:
                // and read new one
                final Uri uri = data.getData();
                /*
                 * The URI returned from application may be in 'file' or 'content' schema.
                 * 'File' schema allows us to create a File object and read details from if directly.
                 *
                 * Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
                 */
                if (uri.getScheme().equals("file")) {
                    // the direct path to the file has been returned
                    try {
                        initiateDfu(new FileInputStream(new File(uri.getPath())));
                    } catch (FileNotFoundException e) {
                        Snackbar.make(findViewById(R.id.drawer_layout), R.string.error_missing_firmware, Snackbar.LENGTH_LONG).show();
                    }
                } else if (uri.getScheme().equals("content")) {
                    fileStreamUri= uri;

                    // file name and size must be obtained from Content Provider
                    final Bundle bundle = new Bundle();
                    bundle.putParcelable(EXTRA_URI, uri);
                    getSupportLoaderManager().restartLoader(0, bundle, this);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            mwBoard.setConnectionStateHandler(null);
            mwBoard.disconnect();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_reset:
                try {
                    mwBoard.getModule(Debug.class).resetDevice();
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_soft_reset, Snackbar.LENGTH_LONG).show();
                } catch (UnsupportedModuleException e) {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.error_soft_reset, Snackbar.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_disconnect:
                mwBoard.setConnectionStateHandler(null);
                mwBoard.disconnect();
                finish();
                return true;
            case R.id.action_manual_dfu:
                if (checkLocationPermission()) {
                    startContentSelectionIntent();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction= fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }

        String fragmentTag= FRAGMENT_CLASSES.get(id).getCanonicalName();
        currentFragment= fragmentManager.findFragmentByTag(fragmentTag);

        if (currentFragment == null) {
            try {
                currentFragment= FRAGMENT_CLASSES.get(id).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate fragment", e);
            }

            transaction.add(R.id.container, currentFragment, fragmentTag);
        }

        transaction.attach(currentFragment).commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(item.getTitle());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mwBoard= ((MetaWearBleService.LocalBinder) service).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(connectionHandler);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain all columns and than check
         * which columns are present.
	     */
        //final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
        return new CursorLoader(this, uri, null /*all columns, instead of projection*/, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);

            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            try {
                if (dataIndex != -1) {
                    initiateDfu(new FileInputStream(new File(data.getString(dataIndex /*2 DATA */))));
                } else {
                    initiateDfu(getContentResolver().openInputStream(fileStreamUri));
                }
            } catch (FileNotFoundException e) {
                Snackbar.make(findViewById(R.id.drawer_layout), R.string.error_missing_firmware, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Code for content selection adapted from the nRF Toolbox app by Nordic Semiconductor
     * https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrftoolbox&hl=en
     */
    private void startContentSelectionIntent() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, SELECT_FILE_REQ);
    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission code taken from Radius Networks
            // http://developer.radiusnetworks.com/2015/09/29/is-your-beacon-app-ready-for-android-6.html

            // Android M Permission check
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_request_permission);
            builder.setMessage(R.string.permission_read_external_storage);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_READ_STORAGE);
                }
            });
            builder.show();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_STORAGE: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(findViewById(R.id.drawer_layout), R.string.message_permission_denied, Snackbar.LENGTH_LONG).show();
                } else {
                    startContentSelectionIntent();
                }
            }
        }
    }
}