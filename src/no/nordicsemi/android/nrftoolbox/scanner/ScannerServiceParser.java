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

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * ScannerServiceParser is responsible to parse scanning data and it check if scanned device has required service in it.
 */
public class ScannerServiceParser {
	private static final String TAG = "ScannerServiceParser";

	private static final int FLAGS_BIT = 0x01;
	private static final int SERVICES_MORE_AVAILABLE_16_BIT = 0x02;
	private static final int SERVICES_COMPLETE_LIST_16_BIT = 0x03;
	private static final int SERVICES_MORE_AVAILABLE_32_BIT = 0x04;
	private static final int SERVICES_COMPLETE_LIST_32_BIT = 0x05;
	private static final int SERVICES_MORE_AVAILABLE_128_BIT = 0x06;
	private static final int SERVICES_COMPLETE_LIST_128_BIT = 0x07;
	private static final int SHORTENED_LOCAL_NAME = 0x08;
	private static final int COMPLETE_LOCAL_NAME = 0x09;

	private static final byte LE_LIMITED_DISCOVERABLE_MODE = 0x01;
	private static final byte LE_GENERAL_DISCOVERABLE_MODE = 0x02;

	/**
	 * Checks if device is connectable (as Android cannot get this information directly we just check if it has GENERAL DISCOVERABLE or LIMITED DISCOVERABLE flag set) and has required service UUID in
	 * the advertising packet. The service UUID may be <code>null</code>.
	 * <p>
	 * For further details on parsing BLE advertisement packet data see https://developer.bluetooth.org/Pages/default.aspx Bluetooth Core Specifications Volume 3, Part C, and Section 8
	 * </p>
	 */
	public static boolean decodeDeviceAdvData(byte[] data, UUID requiredUUID) {
		final String uuid = requiredUUID != null ? requiredUUID.toString() : null;
		if (data != null) {
			boolean connectable = false;
			boolean valid = uuid == null;
			int fieldLength, fieldName;
			int packetLength = data.length;
			for (int index = 0; index < packetLength; index++) {
				fieldLength = data[index];
				if (fieldLength == 0) {
					return connectable && valid;
				}
				fieldName = data[++index];

				if (uuid != null) {
					if (fieldName == SERVICES_MORE_AVAILABLE_16_BIT || fieldName == SERVICES_COMPLETE_LIST_16_BIT) {
						for (int i = index + 1; i < index + fieldLength - 1; i += 2)
							valid = valid || decodeService16BitUUID(uuid, data, i, 2);
					} else if (fieldName == SERVICES_MORE_AVAILABLE_32_BIT || fieldName == SERVICES_COMPLETE_LIST_32_BIT) {
						for (int i = index + 1; i < index + fieldLength - 1; i += 4)
							valid = valid || decodeService32BitUUID(uuid, data, i, 4);
					} else if (fieldName == SERVICES_MORE_AVAILABLE_128_BIT || fieldName == SERVICES_COMPLETE_LIST_128_BIT) {
						for (int i = index + 1; i < index + fieldLength - 1; i += 16)
							valid = valid || decodeService128BitUUID(uuid, data, i, 16);
					}
				}
				if (fieldName == FLAGS_BIT) {
					int flags = data[index + 1];
					connectable = (flags & (LE_GENERAL_DISCOVERABLE_MODE | LE_LIMITED_DISCOVERABLE_MODE)) > 0;
				}
				index += fieldLength - 1;
			}
			return connectable && valid;
		}
		return false;
	}

	/**
	 * Decodes the device name from Complete Local Name or Shortened Local Name field in Advertisement packet. Ususally if should be done by {@link BluetoothDevice#getName()} method but some phones
	 * skips that, f.e. Sony Xperia Z1 (C6903) with Android 4.3 where getName() always returns <code>null</code>. In order to show the device name correctly we have to parse it manually :(
	 */
	public static String decodeDeviceName(byte[] data) {
		String name = null;
		int fieldLength, fieldName;
		int packetLength = data.length;
		for (int index = 0; index < packetLength; index++) {
			fieldLength = data[index];
			if (fieldLength == 0)
				break;
			fieldName = data[++index];

			if (fieldName == COMPLETE_LOCAL_NAME || fieldName == SHORTENED_LOCAL_NAME) {
				name = decodeLocalName(data, index + 1, fieldLength - 1);
				break;
			}
			index += fieldLength - 1;
		}
		return name;
	}

	/**
	 * Decodes the local name
	 */
	public static String decodeLocalName(final byte[] data, final int start, final int length) {
		try {
			return new String(data, start, length, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			Log.e(TAG, "Unable to convert the complete local name to UTF-8", e);
			return null;
		} catch (final IndexOutOfBoundsException e) {
			Log.e(TAG, "Error when reading complete local name", e);
			return null;
		}
	}

	/**
	 * check for required Service UUID inside device
	 */
	private static boolean decodeService16BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
		String serviceUUID = Integer.toHexString(decodeUuid16(data, startPosition));
		String requiredUUID = uuid.substring(4, 8);

		return serviceUUID.equals(requiredUUID);
	}

	/**
	 * check for required Service UUID inside device
	 */
	private static boolean decodeService32BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
		String serviceUUID = Integer.toHexString(decodeUuid16(data, startPosition + serviceDataLength - 4));
		String requiredUUID = uuid.substring(4, 8);

		return serviceUUID.equals(requiredUUID);
	}

	/**
	 * check for required Service UUID inside device
	 */
	private static boolean decodeService128BitUUID(String uuid, byte[] data, int startPosition, int serviceDataLength) {
		String serviceUUID = Integer.toHexString(decodeUuid16(data, startPosition + serviceDataLength - 4));
		String requiredUUID = uuid.substring(4, 8);

		return serviceUUID.equals(requiredUUID);
	}

	private static int decodeUuid16(final byte[] data, final int start) {
		final int b1 = data[start] & 0xff;
		final int b2 = data[start + 1] & 0xff;

		return (b2 << 8 | b1 << 0);
	}
}
