package com.mbientlab.metawear.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.controller.Haptic;

public class HapticFragment extends ModuleFragment {
    private Haptic hapticController;
    private EditText pulseWidth, dutyCycle;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_haptic, container, false);
    }
    
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        pulseWidth= (EditText) view.findViewById(R.id.editText1);
        dutyCycle= (EditText) view.findViewById(R.id.editText2);
                
        ((Button) view.findViewById(R.id.button1)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        hapticController.startMotor(Float.valueOf((dutyCycle).getEditableText().toString()),
                                Short.valueOf((pulseWidth).getEditableText().toString()));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), "Enter a valid pulse width and duty cycle", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        
        ((Button) view.findViewById(R.id.button2)).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mwMnger.controllerReady()) {
                    try {
                        hapticController.startBuzzer(Short.valueOf((pulseWidth).getEditableText().toString()));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), "Enter a valid pulse width", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    @Override
    public void controllerReady(MetaWearController mwController) {
        hapticController= (Haptic) mwController.getModuleController(Module.HAPTIC);
        
    }
}
