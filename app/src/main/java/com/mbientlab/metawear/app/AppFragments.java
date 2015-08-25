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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by etsai on 8/24/2015.
 */
public class AppFragments {
    private static final LinkedHashMap<String, Class<? extends ModuleFragmentBase>> fragmentClasses;
    private static final ArrayList<String> fragmentTags;
    
    static {
        fragmentClasses= new LinkedHashMap<>();
        fragmentClasses.put("Home", HomeFragment.class);
        fragmentClasses.put("Accelerometer", AccelerometerFragment.class);
        fragmentClasses.put("Barometer", BarometerFragment.class);
        fragmentClasses.put("Gpio", GpioFragment.class);
        fragmentClasses.put("Gyro", GyroFragment.class);
        fragmentClasses.put("Haptic", HapticFragment.class);
        fragmentClasses.put("IBeacon", IBeaconFragment.class);
        fragmentClasses.put("Light", AmbientLightFragment.class);
        fragmentClasses.put("NeoPixel", NeoPixelFragment.class);
        fragmentClasses.put("Settings", SettingsFragment.class);
        fragmentClasses.put("Temperature", TemperatureFragment.class);

        fragmentTags= new ArrayList<>(fragmentClasses.keySet());
    }

    public static Class<? extends ModuleFragmentBase> findFragmentClass(String tag) {
        return fragmentClasses.get(tag);
    }

    public static List<String> getFragmentTags() {
        return Collections.unmodifiableList(fragmentTags);
    }

    public static String getFragmentTag(int index) {
        return fragmentTags.get(index);
    }
}
