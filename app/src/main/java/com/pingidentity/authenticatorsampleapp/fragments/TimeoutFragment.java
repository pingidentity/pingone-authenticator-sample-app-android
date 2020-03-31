package com.pingidentity.authenticatorsampleapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pingidentity.authenticatorsampleapp.R;

import static android.os.Looper.getMainLooper;


/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class TimeoutFragment extends Fragment {


    public TimeoutFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_timeout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    requireActivity().onBackPressed();
                }catch (IllegalStateException e){
                    //activity already closed do nothing
                }
            }
        }, 2000);
    }
}
