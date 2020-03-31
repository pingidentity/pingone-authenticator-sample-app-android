package com.pingidentity.authenticatorsampleapp.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BarcodeViewModel extends ViewModel {

    private final MutableLiveData<String> barcode = new MutableLiveData<>();

    public void updateBarcode(String barcode){
        this.barcode.setValue(barcode);
    }
    public MutableLiveData<String> getBarcode(){
        return barcode;
    }
}
