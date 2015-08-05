package com.mbientlab.metawear.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Haptic;

public class HapticFragment extends ModuleFragment {
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
                
        view.findViewById(R.id.button1).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        Haptic hapticController= currBoard.getModule(Haptic.class);
                        hapticController.startMotor(Float.valueOf((dutyCycle).getEditableText().toString()),
                                Short.valueOf((pulseWidth).getEditableText().toString()));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), "Enter a valid pulse width and duty cycle", 
                                Toast.LENGTH_SHORT).show();
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board, 
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        
        view.findViewById(R.id.button2).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currBoard != null && currBoard.isConnected()) {
                    try {
                        Haptic hapticController= currBoard.getModule(Haptic.class);
                        hapticController.startBuzzer(Short.valueOf((pulseWidth).getEditableText().toString()));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(getActivity(), "Enter a valid pulse width",
                                Toast.LENGTH_SHORT).show();
                    } catch (UnsupportedModuleException e) {
                        Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_connect_board,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private MetaWearBoard currBoard;
    @Override
    public void connected(MetaWearBoard currBoard) {
        this.currBoard= currBoard;
    }

    @Override
    public void disconnected() {

    }
}
