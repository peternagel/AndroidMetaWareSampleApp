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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Gpio;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;
import com.mbientlab.metawear.processor.Time;

/**
 * @author etsai
 *
 */
public class LoggingFragment extends ModuleFragment {
    private MetaWearBoard currBoard;

    private interface Sensor {
        String getDescription();
        void stopSensors();
        void setup();
        File[] saveDataToFile() throws IOException;
    }
    
    private class GPIOSensor implements Sensor {
        private class GPIOLogData {
            public GPIOLogData(double time, short adcData) {
                this.time= time;
                this.adc= adcData;
            }
            public final double time;
            public final short adc;
        }

        private Calendar first= null;
        private ArrayList<GPIOLogData> gpioData;
        private final String CSV_HEADER_ADC= "time,adc";
        private final byte gpioPin;

        public GPIOSensor(byte gpioPin) {
            this.gpioPin= gpioPin;
        }

        @Override
        public void setup() {
            try {
                gpioData= new ArrayList<>();
                final Gpio gpioModule = currBoard.getModule(Gpio.class);
                final Timer timerModule = currBoard.getModule(Timer.class);
                final Logging logModule = currBoard.getModule(Logging.class);

                currBoard.removeRoutes();
                gpioModule.routeData().fromAnalogGpio(gpioPin, Gpio.AnalogReadMode.ADC).log("log_gpio_adc")
                        .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.setLogMessageHandler("log_gpio_adc", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        double offset;

                                        if (first == null) {
                                            first= message.getTimestamp();
                                            offset= 0.0;
                                        } else {
                                            offset= (message.getTimestamp().getTimeInMillis() - first.getTimeInMillis()) / 1000.0;
                                        }

                                        gpioData.add(new GPIOLogData(offset, message.getData(Short.class)));
                                    }
                                });
                            }
                        });
                timerModule.scheduleTask(new Timer.Task() {
                    @Override
                    public void commands() {
                        gpioModule.readAnalogIn(gpioPin, Gpio.AnalogReadMode.ADC);
                    }
                }, 500, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                    @Override
                    public void success(Timer.Controller result) {
                        logModule.startLogging();
                        result.start();
                    }
                });
            } catch (UnsupportedModuleException e) {
                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public String getDescription() {
            return String.format(Locale.US, "Logs ADC value of GPIO pin %d every 500ms.  %s", 
                    gpioPin, "You will need firmware 1.0.0 or higher to log GPIO data");
        }
        
        @Override
        public String toString() { return String.format(Locale.US, "GPIO pin %d", gpioPin); }
        
        @Override
        public void stopSensors() {
            try {
                final Timer timerModule = currBoard.getModule(Timer.class);
                final Logging logModule = currBoard.getModule(Logging.class);

                logDLProgress= new ProgressDialog(getActivity());
                logDLProgress.setOwnerActivity(getActivity());
                logDLProgress.setTitle("Log Download");
                logDLProgress.setMessage("Downloading log...");
                logDLProgress.setIndeterminate(true);
                logDLProgress.show();

                timerModule.removeTimers();
                logModule.stopLogging();
                logModule.downloadLog(0.0f, new Logging.DownloadHandler() {
                    @Override
                    public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                        if (nEntriesLeft == 0) {
                            logDLProgress.dismiss();
                            startEmailIntent();
                        }
                    }

                    @Override
                    public void receivedUnknownLogEntry(byte logId, Calendar timestamp, byte[] data) {
                        Log.i("Logging", String.format("ID= %d", logId));
                    }
                });
            } catch (UnsupportedModuleException e) {
                Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
        
        @Override
        public File[] saveDataToFile() throws IOException {
            File tempFile= LoggingFragment.this.getActivity().getFileStreamPath("GPIO_Pin0_ADC_Data.csv");
            File[] dataFiles= new File[] {tempFile};
            
            FileOutputStream fos= new FileOutputStream(tempFile);
            fos.write(String.format("%s%n", CSV_HEADER_ADC).getBytes());
            for(GPIOLogData it: gpioData) {
                fos.write(String.format(Locale.US, "%.3f,%d%n", it.time, it.adc).getBytes());
            }
            fos.close();

            return dataFiles;
        }
    }
    
    private int sensorIndex;
    private Sensor[] sensors= {
            new Sensor() {
                private ArrayList<Message> xyzData;
                private final String CSV_HEADER_XYZ= "time,x-axis,y-axis,z-axis";
                  
                @Override
                public String getDescription() {
                    return "Logs the accelerometer axis data sampling at 50Hz";
                }
                
                @Override
                public String toString() { return "Accelerometer"; }
                
                @Override
                public void stopSensors() {
                    try {
                        final Accelerometer accelController= currBoard.getModule(Accelerometer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        accelController.stop();

                        logDLProgress= new ProgressDialog(getActivity());
                        logDLProgress.setOwnerActivity(getActivity());
                        logDLProgress.setTitle("Log Download");
                        logDLProgress.setMessage("Downloading log...");
                        logDLProgress.setIndeterminate(true);
                        logDLProgress.show();

                        logModule.stopLogging();
                        logModule.downloadLog(0.0f, new Logging.DownloadHandler() {
                            @Override
                            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                                if (nEntriesLeft == 0) {
                                    logDLProgress.dismiss();
                                    startEmailIntent();
                                }
                            }
                        });

                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public void setup() {
                    try {
                        final Accelerometer accelController= currBoard.getModule(Accelerometer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        currBoard.removeRoutes();
                        xyzData= new ArrayList<>();
                        logModule.startLogging();
                        accelController.routeData().fromAxes().log("accelerometer_axis_log").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.setLogMessageHandler("accelerometer_axis_log", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        xyzData.add(message);
                                    }
                                });

                                accelController.setOutputDataRate(50.f);
                                accelController.enableAxisSampling();
                                accelController.start();
                            }
                        });
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public File[] saveDataToFile() throws IOException {
                    File xyzFile= LoggingFragment.this.getActivity().getFileStreamPath("Accelerometer_xyz_data.csv");
                    File[] dataFiles= new File[] {xyzFile};

                    Calendar first= null;
                    FileOutputStream fos= new FileOutputStream(xyzFile);
                    fos.write(String.format("%s%n", CSV_HEADER_XYZ).getBytes());
                    for(Message it: xyzData) {
                        double offset;
                        CartesianFloat axisData= it.getData(CartesianFloat.class);
                        if (first == null) {
                            first= it.getTimestamp();
                            offset= 0;
                        } else {
                            offset= (it.getTimestamp().getTimeInMillis() - first.getTimeInMillis()) / 1000.0;
                        }
                        fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f,%.3f%n", offset, axisData.x(), axisData.y(), axisData.z()).getBytes());
                    }
                    fos.close();

                    return dataFiles;
                }
            },
            new Sensor() {
                private final String CSV_HEADER_ACTIVITY= "time,activity";
                private final int TIME_DELAY_PERIOD= 30000;
                private ArrayList<Message> activityData;

                @Override
                public String getDescription() {
                    return "Computes and logs activity data every 30 seconds using the on board accelerometer and data processor";
                }

                @Override
                public String toString() { return "Activity"; }

                @Override
                public void setup() {
                    try {
                        final Accelerometer accelController= currBoard.getModule(Accelerometer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        currBoard.removeRoutes();
                        activityData= new ArrayList<>();
                        logModule.startLogging();
                        accelController.routeData().fromAxes().process("rms").process("accumulator?output=4").process(new Time(Time.OutputMode.ABSOLUTE, TIME_DELAY_PERIOD)).log("activity_log")
                                .commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                    @Override
                                    public void success(RouteManager result) {
                                        result.setLogMessageHandler("activity_log", new RouteManager.MessageHandler() {
                                            @Override
                                            public void process(Message message) {
                                                activityData.add(message);
                                            }
                                        });

                                        accelController.setOutputDataRate(50.f);
                                        accelController.enableAxisSampling();
                                        accelController.start();
                                    }

                            @Override
                            public void failure(Throwable error) {
                                Log.e("Loggable", "error committing", error);
                            }
                        });
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void stopSensors() {
                    try {
                        final Accelerometer accelController = currBoard.getModule(Accelerometer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        accelController.stop();
                        accelController.disableAxisSampling();

                        logDLProgress= new ProgressDialog(getActivity());
                        logDLProgress.setOwnerActivity(getActivity());
                        logDLProgress.setTitle("Log Download");
                        logDLProgress.setMessage("Downloading log...");
                        logDLProgress.setIndeterminate(true);
                        logDLProgress.show();

                        logModule.stopLogging();
                        logModule.downloadLog(0.0f, new Logging.DownloadHandler() {
                            @Override
                            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                                if (nEntriesLeft == 0) {
                                    logDLProgress.dismiss();
                                    startEmailIntent();
                                }
                            }

                            @Override
                            public void receivedUnknownLogEntry(byte logId, Calendar timestamp, byte[] data) {
                                Log.i("Logging", String.format("ID= %d", logId));
                            }
                        });
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public File[] saveDataToFile() throws IOException {
                    File tempFile= LoggingFragment.this.getActivity().getFileStreamPath("Activity_Data.csv");
                    File[] dataFiles= new File[] {tempFile};

                    Calendar first= null;
                    FileOutputStream fos= new FileOutputStream(tempFile);
                    fos.write(String.format("%s%n", CSV_HEADER_ACTIVITY).getBytes());
                    for(Message it: activityData) {
                        double offset;
                        if (first == null) {
                            first= it.getTimestamp();
                            offset= 0;
                        } else {
                            offset= (it.getTimestamp().getTimeInMillis() - first.getTimeInMillis()) / 1000.0;
                        }
                        fos.write(String.format(Locale.US, "%.3f,%.3f%n", offset, it.getData(Float.class)).getBytes());
                    }
                    fos.close();

                    return dataFiles;
                }
            },
            new GPIOSensor((byte) 0),
            new GPIOSensor((byte) 1),
            new Sensor() {
                private ArrayList<Message> tempData;
                private final String CSV_HEADER_TEMP= "time,temperature";

                @Override
                public String getDescription() {
                    return "Logs the temperature value every 500ms";
                }

                @Override
                public String toString() { return "Temperature"; }

                @Override
                public void stopSensors() {
                    try {
                        final Timer timerModule = currBoard.getModule(Timer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        timerModule.removeTimers();
                        logDLProgress= new ProgressDialog(getActivity());
                        logDLProgress.setOwnerActivity(getActivity());
                        logDLProgress.setTitle("Log Download");
                        logDLProgress.setMessage("Downloading log...");
                        logDLProgress.setIndeterminate(true);
                        logDLProgress.show();

                        logModule.stopLogging();
                        logModule.downloadLog(0.0f, new Logging.DownloadHandler() {
                            @Override
                            public void onProgressUpdate(int nEntriesLeft, int totalEntries) {
                                if (nEntriesLeft == 0) {
                                    logDLProgress.dismiss();
                                    startEmailIntent();
                                }
                            }
                        });

                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void setup() {
                    try {
                        final Temperature tempModule= currBoard.getModule(Temperature.class);
                        final Timer timerModule= currBoard.getModule(Timer.class);
                        final Logging logModule = currBoard.getModule(Logging.class);

                        currBoard.removeRoutes();
                        tempData= new ArrayList<>();
                        tempModule.routeData().fromSensor().log("temp_logger").commit().onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                            @Override
                            public void success(RouteManager result) {
                                result.setLogMessageHandler("temp_logger", new RouteManager.MessageHandler() {
                                    @Override
                                    public void process(Message message) {
                                        tempData.add(message);
                                    }
                                });
                            }
                        });
                        timerModule.scheduleTask(new Timer.Task() {
                            @Override
                            public void commands() {
                                tempModule.readTemperature();
                            }
                        }, 500, false).onComplete(new AsyncOperation.CompletionHandler<Timer.Controller>() {
                            @Override
                            public void success(Timer.Controller result) {
                                logModule.startLogging();
                                result.start();
                            }
                        });

                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public File[] saveDataToFile() throws IOException {
                    File tempFile= LoggingFragment.this.getActivity().getFileStreamPath("Temperature_Data.csv");
                    File[] dataFiles= new File[] {tempFile};

                    Calendar first= null;
                    FileOutputStream fos= new FileOutputStream(tempFile);
                    fos.write(String.format("%s%n", CSV_HEADER_TEMP).getBytes());
                    for(Message it: tempData) {
                        double offset;
                        if (first == null) {
                            first= it.getTimestamp();
                            offset= 0;
                        } else {
                            offset= (it.getTimestamp().getTimeInMillis() - first.getTimeInMillis()) / 1000.0;
                        }
                        fos.write(String.format(Locale.US, "%.3f,%.3f%n", offset, it.getData(Float.class)).getBytes());
                    }
                    fos.close();

                    return dataFiles;
                }
            }
    };

    private ProgressDialog logDLProgress= null;

    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
    }

    @Override
    public void disconnected() {

    }

    @Override
    public void onAttach(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        super.onAttach(activity);
    }
    
    @Override
    public void onPause() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_logging, container,
                false);
    }
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Spinner spinnerObj;
        
        spinnerObj= (Spinner) view.findViewById(R.id.spinner1);
        spinnerObj.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.command_row, R.id.command_name, sensors));
        spinnerObj.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View innerView,
                    int position, long id) {
                sensorIndex= position;
                TextView description= (TextView) view.findViewById(R.id.textView2);
                description.setText(sensors[sensorIndex].getDescription());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        
        view.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    sensors[sensorIndex].setup();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    sensors[sensorIndex].stopSensors();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void startEmailIntent() {
        ArrayList<Uri> fileUris= new ArrayList<>();

        logDLProgress= new ProgressDialog(getActivity());
        logDLProgress.setOwnerActivity(getActivity());
        logDLProgress.setTitle("Saving to file");
        logDLProgress.setMessage("Saving...");
        logDLProgress.setIndeterminate(true);
        logDLProgress.show();

        try {
            for(File it: sensors[sensorIndex].saveDataToFile()) {
                fileUris.add(FileProvider.getUriForFile(getActivity(), 
                        "com.mbientlab.metawear.app.fileprovider", it));
            }

            logDLProgress.dismiss();
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, String.format(Locale.US, 
                    "Logged %s data - %tY-%<tm-%<tdT%<tH-%<tM-%<tS", sensors[sensorIndex].toString(), 
                    Calendar.getInstance().getTime()));
            
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (IOException e) {
            logDLProgress.dismiss();
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
