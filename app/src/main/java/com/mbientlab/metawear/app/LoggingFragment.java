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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Accelerometer;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.FullScaleRange;
import com.mbientlab.metawear.api.controller.Accelerometer.SamplingConfig.OutputDataRate;
import com.mbientlab.metawear.api.controller.DataProcessor;
import com.mbientlab.metawear.api.controller.DataProcessor.FilterConfig;
import com.mbientlab.metawear.api.controller.Event;
import com.mbientlab.metawear.api.controller.GPIO;
import com.mbientlab.metawear.api.controller.GPIO.AnalogMode;
import com.mbientlab.metawear.api.controller.Logging;
import com.mbientlab.metawear.api.controller.Logging.LogEntry;
import com.mbientlab.metawear.api.controller.Temperature;
import com.mbientlab.metawear.api.controller.Timer;
import com.mbientlab.metawear.api.util.BytesInterpreter;
import com.mbientlab.metawear.api.util.FilterConfigBuilder;
import com.mbientlab.metawear.api.util.LoggingTrigger;
import com.mbientlab.metawear.api.util.TriggerBuilder;

/**
 * @author etsai
 *
 */
public class LoggingFragment extends ModuleFragment {
    private abstract class Sensor extends Logging.Callbacks {
        private int totalEntries;
        private boolean ready;
        protected LogEntry firstEntry;
        
        public abstract String getDescription();
        public abstract void stopSensors();
        public abstract File[] saveDataToFile() throws IOException;
        public abstract void processData(double offset, LogEntry entry);
        
        public void setupLogger() {
            ready= false;
        }
        
        public boolean dataReady() {
            return ready;
        }
        
        @Override
        public void receivedTriggerId(byte triggerId) {
            loggingController.startLogging();
            Toast.makeText(getActivity(), R.string.label_lob_start_message, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void receivedLogEntry(LogEntry entry) {
            if (firstEntry == null) {
                firstEntry= entry;
            }
            processData(entry.offset(firstEntry) / 1000.0, entry);
        }
        
        @Override
        public void receivedTotalEntryCount(int totalEntries) {
            if (logDLProgress == null || !logDLProgress.isShowing()) {
                this.totalEntries= totalEntries;
                
                logDLProgress= new ProgressDialog(getActivity());
                logDLProgress.setOwnerActivity(getActivity());
                logDLProgress.setTitle("Log Download");
                logDLProgress.setMessage("Downloading log...");
                logDLProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                logDLProgress.setProgress(0);
                logDLProgress.setMax(totalEntries);
                logDLProgress.show();
            
                loggingController.downloadLog(totalEntries, (int) (totalEntries * 0.01));
            }
        }
        
        @Override
        public void receivedDownloadProgress(int nEntriesLeft) {
            logDLProgress.setProgress(totalEntries - nEntriesLeft);
        }

        @Override
        public void downloadCompleted() {
            if (logDLProgress.isShowing()) {
                logDLProgress.dismiss();
            }
            
            firstEntry= null;
            ready= true;
            loggingController.removeAllTriggers();
            startEmailIntent();
        }
    }
    
    private class GPIOSensor extends Sensor {
        private class GPIOLogData {
            public GPIOLogData(double time, byte[] adcData) {
                this.time= time;
                this.adc= (short) ((adcData[0] & 0xff) | ((adcData[1] << 8) & 0xffff));
            }
            public final double time;
            public final short adc;
        }

        private Event eventController;
        private Timer timerController;
        
        private ArrayList<GPIOLogData> gpioData;
        private final String CSV_HEADER_ADC= "time,adc";
        private byte myTimerId= -1, commandId= -1;
        private final byte gpioPin;

        public GPIOSensor(byte gpioPin) {
            this.gpioPin= gpioPin;
        }
        
        @Override
        public String getDescription() {
            return String.format(Locale.US, "Logs ADC value of GPIO pin %d every 500ms.  %s", 
                    gpioPin, "You will need firmware 1.0.0 or higher to log GPIO data");
        }
        @Override
        public void receivedTriggerId(byte triggerId) {
            super.receivedTriggerId(triggerId);
            timerController.startTimer(myTimerId);
        }
        
        @Override
        public String toString() { return String.format(Locale.US, "GPIO pin %d", gpioPin); }
        
        @Override
        public void stopSensors() {
            final MetaWearController mwController= mwMnger.getCurrentController();
            final Event  eventController= (Event) mwController.getModuleController(Module.EVENT);
            final Timer timerController= (Timer) mwController.getModuleController(Module.TIMER);

            timerController.removeTimer(myTimerId);
            eventController.removeCommand(commandId);
        }
        
        @Override
        public void setupLogger() {
            super.setupLogger();
            
            gpioData= new ArrayList<>();
            
            final MetaWearController mwController= mwMnger.getCurrentController();
            final GPIO gpioController= (GPIO) mwController.getModuleController(Module.GPIO);
            final Event eventController= (Event) mwController.getModuleController(Module.EVENT);
            final Timer timerController= (Timer) mwController.getModuleController(Module.TIMER);
            mwController.addModuleCallback(new Timer.Callbacks() {
                @Override
                public void receivedTimerId(byte timerId) {
                    myTimerId= timerId;
                    
                    eventController.recordMacro(Timer.Register.TIMER_NOTIFY, timerId);
                    gpioController.readAnalogInput(gpioPin, AnalogMode.SUPPLY_RATIO);
                    eventController.stopRecord();
                    
                    loggingController.addReadTrigger(TriggerBuilder.buildGPIOAnalogTrigger(true, gpioPin));
                    
                    mwController.removeModuleCallback(this);
                }
            }).addModuleCallback(new Event.Callbacks() {
                @Override
                public void receivedCommandId(byte id) {
                    commandId= id;
                    mwController.removeModuleCallback(this);
                }
            });
            
            timerController.addTimer(500, (short) 0, false);
        }
        
        @Override
        public void processData(double offset, LogEntry entry) {
            gpioData.add(new GPIOLogData(offset, entry.data()));
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
                private byte xyAxisId= -1, zAxisId= -1;
                private ArrayList<double[]> xyData= null, zData= null;
                private final String CSV_HEADER_XY= "time,x-Axis,y-Axis", CSV_HEADER_Z= "time,z-Axis";
                  
                @Override
                public String getDescription() {
                    return "Logs the accelerometer axis data sampling at 100Hz";
                }
                
                @Override
                public String toString() { return "Accelerometer"; }
                
                @Override
                public void stopSensors() {
                    final Accelerometer accelController= (Accelerometer) mwMnger.getCurrentController()
                                .getModuleController(Module.ACCELEROMETER);
                    accelController.stopComponents();
                }
                
                @Override
                public void setupLogger() {
                    super.setupLogger();

                    final Accelerometer accelController= (Accelerometer) mwMnger.getCurrentController()
                            .getModuleController(Module.ACCELEROMETER);
                    xyData= new ArrayList<>();
                    zData= new ArrayList<>();
                    
                    xyAxisId= -1;
                    zAxisId= -1;
                    
                    loggingController.addTrigger(LoggingTrigger.ACCELEROMETER_XY_AXIS);
                    loggingController.addTrigger(LoggingTrigger.ACCELEROMETER_Z_AXIS);
                    
                    accelController.enableXYZSampling().withFullScaleRange(FullScaleRange.FSR_8G)
                        .withOutputDataRate(OutputDataRate.ODR_100_HZ)
                        .withSilentMode();
                    accelController.startComponents();
                }
                
                @Override
                public void receivedTriggerId(byte triggerId) {
                    if (xyAxisId == -1) {
                        xyAxisId= triggerId;
                    } else {
                        zAxisId= triggerId;
                        
                        super.receivedTriggerId(triggerId);
                    }
                }
                
                @Override
                public void processData(double offset, LogEntry entry) {
                    if (entry.triggerId() == xyAxisId) {
                        xyData.add(new double[] { offset, BytesInterpreter.logBytesToGs(entry.data(), (byte) 0), 
                                BytesInterpreter.logBytesToGs(entry.data(), (byte) 2) });
                    } else if (entry.triggerId() == zAxisId) {
                        zData.add(new double[] { offset, BytesInterpreter.logBytesToGs(entry.data(), (byte) 0) });
                    }
                }
                
                @Override
                public File[] saveDataToFile() throws IOException {
                    File xyFile= LoggingFragment.this.getActivity().getFileStreamPath("Accelerometer_xy_data.csv"),
                            zFile= LoggingFragment.this.getActivity().getFileStreamPath("Accelerometer_z_data.csv");
                    File[] dataFiles= new File[] {xyFile, zFile};
                    
                    FileOutputStream fos= new FileOutputStream(xyFile);
                    fos.write(String.format("%s%n", CSV_HEADER_XY).getBytes());
                    for(double[] it: xyData) {
                        fos.write(String.format(Locale.US, "%.3f,%.3f,%.3f%n", it[0], it[1], it[2]).getBytes());
                    }
                    fos.close();
                    
                    fos= new FileOutputStream(zFile);
                    fos.write(String.format("%s%n", CSV_HEADER_Z).getBytes());
                    for(double[] it: zData) {
                        fos.write(String.format(Locale.US, "%.3f,%.3f%n", it[0], it[1]).getBytes());
                    }
                    fos.close();
                    
                    return dataFiles;
                }
            },
            new Sensor() {
                private final String CSV_HEADER_ACTIVITY= "time,activity";
                private final byte ACTIVITY_DATA_SIZE = 4;
                private final int TIME_DELAY_PERIOD= 30000;


                private ArrayList<double[]> activityData;
                private byte rmsFilterId= -1, accumFilterId= -1, timeFilterId= -1;


                @Override
                public String getDescription() {
                    return "Computes and logs activity data every 30 seconds using the on board accelerometer and data processor";
                }

                @Override
                public String toString() { return "Activity"; }

                @Override
                public void stopSensors() {
                    final MetaWearController currCtrllr= mwMnger.getCurrentController();
                    final DataProcessor dpCtrllr= (DataProcessor) currCtrllr.getModuleController(Module.DATA_PROCESSOR);
                    final Accelerometer accelCtrllr= (Accelerometer) currCtrllr.getModuleController(Module.ACCELEROMETER);

                    accelCtrllr.stopComponents();
                    dpCtrllr.removeFilter(rmsFilterId);
                    dpCtrllr.removeFilter(timeFilterId);
                    dpCtrllr.removeFilter(accumFilterId);
                }

                @Override
                public void processData(double offset, LogEntry entry) {
                    int activityMilliG= ByteBuffer.wrap(entry.data()).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    activityData.add(new double[] {offset, activityMilliG / 1000.0});
                }
                @Override
                public File[] saveDataToFile() throws IOException {
                    File tempFile= LoggingFragment.this.getActivity().getFileStreamPath("Activity_Data.csv");
                    File[] dataFiles= new File[] {tempFile};

                    FileOutputStream fos= new FileOutputStream(tempFile);
                    fos.write(String.format("%s%n", CSV_HEADER_ACTIVITY).getBytes());
                    for(double[] it: activityData) {
                        fos.write(String.format(Locale.US, "%.3f,%.3f%n", it[0], it[1]).getBytes());
                    }
                    fos.close();

                    return dataFiles;
                }

                @Override
                public void setupLogger() {
                    super.setupLogger();

                    final MetaWearController currCtrllr= mwMnger.getCurrentController();
                    final DataProcessor dpCtrllr= (DataProcessor) currCtrllr.getModuleController(Module.DATA_PROCESSOR);

                    activityData= new ArrayList<>();

                    currCtrllr.addModuleCallback(new DataProcessor.Callbacks() {
                        @Override
                        public void receivedFilterId(byte filterId) {
                            if (rmsFilterId == -1) {
                                rmsFilterId = filterId;
                                FilterConfig accumFilter= new FilterConfigBuilder.AccumulatorBuilder()
                                        .withInputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                                        .withOutputSize(ACTIVITY_DATA_SIZE)
                                        .build();

                                dpCtrllr.chainFilters(rmsFilterId, ACTIVITY_DATA_SIZE, accumFilter);
                            } else if (accumFilterId == -1) {
                                accumFilterId= filterId;
                                FilterConfig timeFilter = new FilterConfigBuilder.TimeDelayBuilder()
                                        .withFilterMode(FilterConfigBuilder.TimeDelayBuilder.FilterMode.ABSOLUTE)
                                        .withPeriod(TIME_DELAY_PERIOD)
                                        .withDataSize(ACTIVITY_DATA_SIZE)
                                        .build();

                                dpCtrllr.chainFilters(accumFilterId, ACTIVITY_DATA_SIZE, timeFilter);
                            } else {
                                timeFilterId= filterId;
                                loggingController.addTrigger(TriggerBuilder.buildDataFilterTrigger(timeFilterId, ACTIVITY_DATA_SIZE));

                                currCtrllr.removeModuleCallback(this);
                            }
                        }
                    });

                    FilterConfig rmsFilter= new FilterConfigBuilder.RMSBuilder()
                            .withInputCount((byte) 3).withSignedInput()
                            .withInputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                            .withOutputSize(LoggingTrigger.ACCELEROMETER_X_AXIS.length())
                            .build();
                    dpCtrllr.addFilter(TriggerBuilder.buildAccelerometerTrigger(), rmsFilter);

                    final Accelerometer accelCtrllr= (Accelerometer) currCtrllr.getModuleController(Module.ACCELEROMETER);
                    accelCtrllr.enableXYZSampling().withFullScaleRange(FullScaleRange.FSR_8G)
                            .withHighPassFilter((byte) 0).withOutputDataRate(OutputDataRate.ODR_100_HZ)
                            .withSilentMode();
                    accelCtrllr.startComponents();
                }
            },
            new GPIOSensor((byte) 0),
            new GPIOSensor((byte) 1),
            new Sensor() {
                private ArrayList<double[]> tempData;
                private final String CSV_HEADER_TEMP= "time,temperature";

                @Override
                public String getDescription() {
                    return String.format(Locale.US, "%s.  %s","Logs the temperature value every 500ms",
                            "You can use the temperature panel to enable/disable thermistor mode");
                }

                @Override
                public String toString() { return "Temperature"; }

                @Override
                public void stopSensors() {
                    final Temperature tempController= (Temperature) mwMnger.getCurrentController()
                            .getModuleController(Module.TEMPERATURE);

                    tempController.disableSampling();
                }

                @Override
                public void setupLogger() {
                    super.setupLogger();

                    final Temperature tempController= (Temperature) mwMnger.getCurrentController()
                            .getModuleController(Module.TEMPERATURE);
                    tempData= new ArrayList<>();

                    loggingController.addTrigger(LoggingTrigger.TEMPERATURE);
                    tempController.enableSampling().withSamplingPeriod(500).withSilentMode().commit();
                }

                @Override
                public void processData(double offset, LogEntry entry) {
                    tempData.add(new double[] {offset, BytesInterpreter.bytesToTemp(entry.data())});
                }

                @Override
                public File[] saveDataToFile() throws IOException {
                    File tempFile= LoggingFragment.this.getActivity().getFileStreamPath("Temperature_Data.csv");
                    File[] dataFiles= new File[] {tempFile};

                    FileOutputStream fos= new FileOutputStream(tempFile);
                    fos.write(String.format("%s%n", CSV_HEADER_TEMP).getBytes());
                    for(double[] it: tempData) {
                        fos.write(String.format(Locale.US, "%.3f,%.3f%n", it[0], it[1]).getBytes());
                    }
                    fos.close();

                    return dataFiles;
                }
            }
    };
    
    private Logging loggingController;
    private ProgressDialog logDLProgress= null;
    
    /* (non-Javadoc)
     * @see com.mbientlab.metawear.app.ModuleFragment#controllerReady(com.mbientlab.metawear.api.MetaWearController)
     */
    @Override
    public void controllerReady(MetaWearController mwController) {
        loggingController= (Logging) mwController.getModuleController(Module.LOGGING);
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
        spinnerObj.setAdapter(new ArrayAdapter<Sensor>(getActivity(), 
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
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    mwMnger.getCurrentController().addModuleCallback(sensors[sensorIndex]);
                    sensors[sensorIndex].setupLogger();
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    mwMnger.getCurrentController().addModuleCallback(sensors[sensorIndex]);
                    sensors[sensorIndex].stopSensors();
                
                    if (sensors[sensorIndex].dataReady()) {
                        startEmailIntent();
                    } else {
                        loggingController.stopLogging();
                        loggingController.readTotalEntryCount();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void startEmailIntent() {
        mwMnger.getCurrentController().removeModuleCallback(sensors[sensorIndex]);
        
        ArrayList<Uri> fileUris= new ArrayList<>();
        
        try {
            for(File it: sensors[sensorIndex].saveDataToFile()) {
                fileUris.add(FileProvider.getUriForFile(getActivity(), 
                        "com.mbientlab.metawear.app.fileprovider", it));
            }
            
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, String.format(Locale.US, 
                    "Logged %s data - %tY-%<tm-%<tdT%<tH-%<tM-%<tS", sensors[sensorIndex].toString(), 
                    Calendar.getInstance().getTime()));
            
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            startActivity(Intent.createChooser(intent, "Send email..."));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
