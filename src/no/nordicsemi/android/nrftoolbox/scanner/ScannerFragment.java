/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/

/*
 * NORDIC SEMICONDUTOR EXAMPLE CODE AND LICENSE AGREEMENT
 *
 * You are receiving this document because you have obtained example code (“Software”) 
 * from Nordic Semiconductor ASA * (“Licensor”). The Software is protected by copyright 
 * laws and international treaties. All intellectual property rights related to the 
 * Software is the property of the Licensor. This document is a license agreement governing 
 * your rights and obligations regarding usage of the Software. Any variation to the terms 
 * of this Agreement shall only be valid if made in writing by the Licensor.
 * 
 * == Scope of license rights ==
 * 
 * You are hereby granted a limited, non-exclusive, perpetual right to use and modify the 
 * Software in order to create your own software. You are entitled to distribute the 
 * Software in original or modified form as part of your own software.
 *
 * If distributing your software in source code form, a copy of this license document shall 
 * follow with the distribution.
 *   
 * The Licensor can at any time terminate your rights under this license agreement.
 * 
 * == Restrictions on license rights ==
 * 
 * You are not allowed to distribute the Software on its own, without incorporating it into 
 * your own software.  
 * 
 * You are not allowed to remove, alter or destroy any proprietary, 
 * trademark or copyright markings or notices placed upon or contained with the Software.
 *     
 * You shall not use Licensor’s name or trademarks without Licensor’s prior consent.
 * 
 * == Disclaimer of warranties and limitation of liability ==
 * 
 * YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THE SOFTWARE IS AT YOUR OWN RISK AND THAT THE 
 * SOFTWARE IS PROVIDED *AS IS" WITHOUT ANY WARRANTIES OR CONDITIONS WHATSOEVER. NORDIC SEMICONDUCTOR ASA 
 * DOES NOT WARRANT THAT THE FUNCTIONS OF THE SOFTWARE WILL MEET YOUR REQUIREMENTS OR THAT THE 
 * OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR FREE. YOU ASSUME RESPONSIBILITY FOR 
 * SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE *USE AND THE RESULTS 
 * OBTAINED FROM THE SOFTWARE.
 * 
 * NORDIC SEMICONDUCTOR ASA DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO WARRANTIES RELATED TO: NON-INFRINGEMENT, LACK OF VIRUSES, ACCURACY OR COMPLETENESS OF RESPONSES 
 * OR RESULTS, IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL OR 
 * CONSEQUENTIAL DAMAGES OR FOR ANY DAMAGES WHATSOEVER (INCLUDING BUT NOT LIMITED TO DAMAGES FOR 
 * LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, PERSONAL INJURY, 
 * LOSS OF PRIVACY OR OTHER PECUNIARY OR OTHER LOSS WHATSOEVER) ARISING OUT OF USE OR INABILITY TO 
 * USE THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * REGARDLESS OF THE FORM OF ACTION, NORDIC SEMICONDUCTOR ASA AGGREGATE LIABILITY ARISING OUT OF 
 * OR RELATED TO THIS AGREEMENT SHALL NOT EXCEED THE TOTAL AMOUNT PAYABLE BY YOU UNDER THIS AGREEMENT. 
 * THE FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE MAXIMUM EXTENT ALLOWED BY 
 * APPLICABLE LAW.
 * 
 * == Dispute resolution and legal venue ==
 * 
 * Any and all disputes arising out of the rights and obligations in this license agreement shall be 
 * submitted to ordinary court proceedings. You accept the Oslo City Court as legal venue under this agreement.
 * 
 * This license agreement shall be governed by Norwegian law.
 * 
 * == Contact information ==
 * 
 * All requests regarding the Software or the API shall be directed to: 
 * Nordic Semiconductor ASA, P.O. Box 436, Skøyen, 0213 Oslo, Norway.
 * 
 * http://www.nordicsemi.com/eng/About-us/Contact-us
 */
package no.nordicsemi.android.nrftoolbox.scanner;

import java.util.Set;
import java.util.UUID;

import com.mbientlab.metawear.app.R;

import no.nordicsemi.android.nrftoolbox.utility.DebugLogger;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter devices with standard BLE Service UUID and devices with custom BLE Service UUID It contains a
 * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is implemented by activity in order to receive selected device. The scanning will continue for 5
 * seconds and then stop
 */
public class ScannerFragment extends DialogFragment {
	private final static String TAG = "ScannerFragment";

	private final static String PARAM_UUID = "param_uuid";
	private final static String CUSTOM_UUID = "custom_uuid";
	private final static long SCAN_DURATION = 5000;

	private BluetoothAdapter mBluetoothAdapter;
	private OnDeviceSelectedListener mListener;
	private DeviceListAdapter mAdapter;
	private Handler mHandler = new Handler();
	private Button mScanButton;

	private boolean mIsCustomUUID;
	private UUID mUuid;

	private boolean mIsScanning = false;

	private static final boolean DEVICE_IS_BONDED = true;
	private static final boolean DEVICE_NOT_BONDED = false;
	/* package */static final int NO_RSSI = -1000;

	/**
	 * Static implementation of fragment so that it keeps data when phone orientation is changed For standard BLE Service UUID, we can filter devices using normal android provided command
	 * startScanLe() with required BLE Service UUID For custom BLE Service UUID, we will use class ScannerServiceParser to filter out required device.
	 */
	public static ScannerFragment getInstance(final Context context, final UUID uuid, final boolean isCustomUUID) {
		final ScannerFragment fragment = new ScannerFragment();

		final Bundle args = new Bundle();
		args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
		args.putBoolean(CUSTOM_UUID, isCustomUUID);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Interface required to be implemented by activity.
	 */
	public static interface OnDeviceSelectedListener {
		/**
		 * Fired when user selected the device.
		 * 
		 * @param device
		 *            the device to connect to
		 * @param name
		 *            the device name. Unfortunately on some devices {@link BluetoothDevice#getName()} always returns <code>null</code>, f.e. Sony Xperia Z1 (C6903) with Android 4.3. The name has to
		 *            be parsed manually form the Advertisement packet.
		 */
		public void onDeviceSelected(final BluetoothDevice device, final String name);

		/**
		 * Fired when scanner dialog has been cancelled without selecting a device.
		 */
		public void onDialogCanceled();
	}

	/**
	 * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity.
	 */
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		try {
			this.mListener = (OnDeviceSelectedListener) activity;
		} catch (final ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Bundle args = getArguments();
		if (args.containsKey(CUSTOM_UUID)) {
			final ParcelUuid pu = args.getParcelable(PARAM_UUID);
			mUuid = pu.getUuid();
		}
		mIsCustomUUID = args.getBoolean(CUSTOM_UUID);

		final BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
	}

	@Override
	public void onDestroyView() {
		stopScan();
		super.onDestroyView();
	}

	/**
	 * When dialog is created then set AlertDialog with list and button views.
	 */
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
		final ListView listview = (ListView) dialogView.findViewById(android.R.id.list);

		listview.setEmptyView(dialogView.findViewById(android.R.id.empty));
		listview.setAdapter(mAdapter = new DeviceListAdapter(getActivity()));

		builder.setTitle(R.string.scanner_title);
		final AlertDialog dialog = builder.setView(dialogView).create();
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				stopScan();
				dialog.dismiss();
				final ExtendedBluetoothDevice d = (ExtendedBluetoothDevice) mAdapter.getItem(position);
				mListener.onDeviceSelected(d.device, d.name);
			}
		});

		mScanButton = (Button) dialogView.findViewById(R.id.action_cancel);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.action_cancel) {
					if (mIsScanning) {
						dialog.cancel();
					} else {
						startScan();
					}
				}
			}
		});

		addBondedDevices();
		if (savedInstanceState == null)
			startScan();
		return dialog;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);

		mListener.onDialogCanceled();
	}

	/**
	 * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out.
	 * using class ScannerServiceParser
	 */
	private void startScan() {
		mAdapter.clearDevices();
		mScanButton.setText(R.string.scanner_action_cancel);

		mIsCustomUUID = true; // Samsung Note II with Android 4.3 build JSS15J.N7100XXUEMK9 is not filtering by UUID at all. We have to disable it
		if (mIsCustomUUID) {
			mBluetoothAdapter.startLeScan(mLEScanCallback);
		} else {
			final UUID[] uuids = new UUID[1];
			uuids[0] = mUuid;
			mBluetoothAdapter.startLeScan(uuids, mLEScanCallback);
		}

		mIsScanning = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mIsScanning) {
					stopScan();
				}
			}
		}, SCAN_DURATION);
	}

	/**
	 * Stop scan if user tap Cancel button.
	 */
	private void stopScan() {
		if (mIsScanning) {
			mScanButton.setText(R.string.scanner_action_scan);
			mBluetoothAdapter.stopLeScan(mLEScanCallback);
			mIsScanning = false;
		}
	}

	private void addBondedDevices() {
		final Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
		for (BluetoothDevice device : devices) {
			mAdapter.addBondedDevice(new ExtendedBluetoothDevice(device, device.getName(), NO_RSSI, DEVICE_IS_BONDED));
		}
	}

	/**
	 * if scanned device already in the list then update it otherwise add as a new device
	 */
	private void addScannedDevice(final BluetoothDevice device, final String name, final int rssi, final boolean isBonded) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mAdapter.addOrUpdateDevice(new ExtendedBluetoothDevice(device, name, rssi, isBonded));
			}
		});
	}

	/**
	 * if scanned device already in the list then update it otherwise add as a new device.
	 */
	private void updateScannedDevice(final BluetoothDevice device, final int rssi) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mAdapter.updateRssiOfBondedDevice(device.getAddress(), rssi);
			}
		});
	}

	/**
	 * Callback for scanned devices class {@link ScannerServiceParser} will be used to filter devices with custom BLE service UUID then the device will be added in a list.
	 */
	private BluetoothAdapter.LeScanCallback mLEScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
			if (device != null) {
				updateScannedDevice(device, rssi);
				if (mIsCustomUUID) {
					try {
						if (ScannerServiceParser.decodeDeviceAdvData(scanRecord, mUuid)) {
							// On some devices device.getName() is always null. We have to parse the name manually :(
							// This bug has been found on Sony Xperia Z1 (C6903) with Android 4.3.
							// https://devzone.nordicsemi.com/index.php/cannot-see-device-name-in-sony-z1
							addScannedDevice(device, ScannerServiceParser.decodeDeviceName(scanRecord), rssi, DEVICE_NOT_BONDED);
						}
					} catch (Exception e) {
						DebugLogger.e(TAG, "Invalid data in Advertisement packet " + e.toString());
					}
				} else {
					addScannedDevice(device, ScannerServiceParser.decodeDeviceName(scanRecord), rssi, DEVICE_NOT_BONDED);
				}
			}
		}
	};
}
