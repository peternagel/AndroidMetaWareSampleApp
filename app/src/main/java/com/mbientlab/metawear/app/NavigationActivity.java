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

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Debug;

public class NavigationActivity extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks, SensorFragment.FragmentBus, ServiceConnection {
    public static final String EXTRA_BT_DEVICE= "com.mbientlab.metawear.app.NavigationActivity.EXTRA_BT_DEVICE";

    private ProgressDialog reconnectProgress= null;
    private final ConnectionStateHandler connectionHandler= new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            if (reconnectProgress != null) {
                reconnectProgress.dismiss();
            }
            reconnectProgress= null;
            ((ModuleFragmentBase) currentFragment).reconnected();
        }

        @Override
        public void disconnected() {
            attemptReconnect();
        }

        @Override
        public void failure(int status, Throwable error) {
            mwBoard.connect();
        }
    };

    private void attemptReconnect() {
        reconnectProgress= new ProgressDialog(NavigationActivity.this);
        reconnectProgress.setTitle(getString(R.string.title_reconnect_attempt));
        reconnectProgress.setMessage(getString(R.string.message_wait));
        reconnectProgress.setCancelable(false);
        reconnectProgress.setCanceledOnTouchOutside(false);
        reconnectProgress.setIndeterminate(true);
        reconnectProgress.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mwBoard.disconnect();
                finish();
            }
        });
        reconnectProgress.show();

        mwBoard.connect();
    }

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private MetaWearBoard mwBoard;
    private BluetoothDevice btDevice;
    private Fragment currentFragment= null;

    @Override
    public void onBackPressed() {
        mwBoard.setConnectionStateHandler(null);
        mwBoard.disconnect();

        super.onBackPressed();
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

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = AppFragments.getFragmentTag(mNavigationDrawerFragment.getCurrentPosition());

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        btDevice= getIntent().getParcelableExtra(EXTRA_BT_DEVICE);
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, BIND_AUTO_CREATE);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction= fragmentManager.beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }

        String newFragmentTag= AppFragments.getFragmentTag(position);
        currentFragment= fragmentManager.findFragmentByTag(newFragmentTag);

        if (currentFragment == null) {
            try {
                currentFragment= AppFragments.findFragmentClass(newFragmentTag).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate fragment", e);
            }

            transaction.add(R.id.container, currentFragment, newFragmentTag);
        }
        mTitle= newFragmentTag;
        restoreActionBar();
        transaction.attach(currentFragment).commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(mTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.navigation, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_reset:
                try {
                    mwBoard.getModule(Debug.class).resetDevice();
                    Toast.makeText(this, R.string.message_soft_reset, Toast.LENGTH_LONG).show();
                } catch (UnsupportedModuleException e) {
                    Toast.makeText(this, R.string.error_soft_reset, Toast.LENGTH_SHORT).show();
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
    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    @Override
    public void resetConnectionStateHandler() {
        mwBoard.setConnectionStateHandler(connectionHandler);
        attemptReconnect();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mwBoard= ((MetaWearBleService.LocalBinder) iBinder).getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(connectionHandler);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }
}
