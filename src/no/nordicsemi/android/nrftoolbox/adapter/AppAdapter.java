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
package no.nordicsemi.android.nrftoolbox.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.mbientlab.metawear.app.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter {
	private static final String CATEGORY = "no.nordicsemi.android.nrftoolbox.LAUNCHER";
	private static final String MCP_PACKAGE = "no.nordicsemi.android.mcp";

	private final Context mContext;
	private final PackageManager mPackageManager;
	private final LayoutInflater mInflater;
	private final List<ResolveInfo> mApplications;

	public AppAdapter(final Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);

		// get nRF installed app plugins from package manager
		final PackageManager pm = mPackageManager = context.getPackageManager();
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(CATEGORY);

		final List<ResolveInfo> appList = mApplications = pm.queryIntentActivities(intent, 0);
		// TODO remove the following loop after some time, when there will be no more MCP 1.1 at the market.
		for (final ResolveInfo info : appList) {
			if (MCP_PACKAGE.equals(info.activityInfo.packageName)) {
				appList.remove(info);
				break;
			}
		}
		Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));
	}

	@Override
	public int getCount() {
		return mApplications.size();
	}

	@Override
	public Object getItem(int position) {
		return mApplications.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = mInflater.inflate(R.layout.feature_icon, parent, false);

			final ViewHolder holder = new ViewHolder();
			holder.view = view;
			holder.icon = (ImageView) view.findViewById(R.id.icon);
			holder.label = (TextView) view.findViewById(R.id.label);
			view.setTag(holder);
		}

		final ResolveInfo info = mApplications.get(position);
		final PackageManager pm = mPackageManager;

		final ViewHolder holder = (ViewHolder) view.getTag();
		holder.icon.setImageDrawable(info.loadIcon(pm));
		holder.label.setText(info.loadLabel(pm).toString().toUpperCase(Locale.US));
		holder.view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent();
				intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				mContext.startActivity(intent);
			}
		});

		return view;
	}

	private class ViewHolder {
		private View view;
		private ImageView icon;
		private TextView label;
	}
}
