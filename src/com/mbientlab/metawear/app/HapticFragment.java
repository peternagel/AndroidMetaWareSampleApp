package com.mbientlab.metawear.app;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Haptic;

public class HapticFragment extends ModuleFragment {
    private Haptic hapticController;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_haptic, container, false);
    }
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticController.startMotor(Short.valueOf(((EditText) view.findViewById(R.id.editText1)).getEditableText().toString()));
            }
        });
        
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                hapticController.startBuzzer(Short.valueOf(((EditText) view.findViewById(R.id.editText1)).getEditableText().toString()));
            }
        });
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        hapticController= (Haptic)this.mwController.getModuleController(Module.HAPTIC);
    }
}
