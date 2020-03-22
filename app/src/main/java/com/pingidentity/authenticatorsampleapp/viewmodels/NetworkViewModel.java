package com.pingidentity.authenticatorsampleapp.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NetworkViewModel extends ViewModel {
    private final MutableLiveData<Boolean> network = new MutableLiveData<>();

    public void updateNetwork(Boolean network){
        this.network.setValue(network);
    }

    public MutableLiveData<Boolean> getNetwork() {
        return network;
    }
}
