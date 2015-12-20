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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.app.ModuleFragmentBase.FragmentBus;
import com.mbientlab.metawear.module.Debug;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection, FragmentBus {
    public static final String EXTRA_BT_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.EXTRA_BT_DEVICE";

    private final static String FRAGMENT_KEY= "com.mbientlab.metawear.app.NavigationActivity.FRAGMENT_KEY";
    private static final Map<Integer, Class<? extends ModuleFragmentBase>> FRAGMENT_CLASSES;

    static {
        Map<Integer, Class<? extends ModuleFragmentBase>> tempMap= new LinkedHashMap<>();
        tempMap.put(R.id.nav_home, HomeFragment.class);
        tempMap.put(R.id.nav_accelerometer, AccelerometerFragment.class);
        tempMap.put(R.id.nav_barometer, BarometerFragment.class);
        tempMap.put(R.id.nav_gpio, GpioFragment.class);
        tempMap.put(R.id.nav_gyro, GyroFragment.class);
        tempMap.put(R.id.nav_haptic, HapticFragment.class);
        tempMap.put(R.id.nav_ibeacon, IBeaconFragment.class);
        tempMap.put(R.id.nav_light, AmbientLightFragment.class);
        tempMap.put(R.id.nav_neopixel, NeoPixelFragment.class);
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

    private final String RECONNECT_DIALOG_TAG= "reconnect_dialog_tag";
    private final Handler connectScheduler= new Handler();
    private BluetoothDevice btDevice;
    private MetaWearBoard mwBoard;
    private Fragment currentFragment= null;

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
            connectScheduler.postDelayed(new Runnable() {
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
}
