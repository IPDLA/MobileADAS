package com.ipdla.mobileadas.ui.destination.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skt.Tmap.TMapData

class DestinationViewModel : ViewModel() {
    private val _isTyping = MutableLiveData(true)
    val isTyping: LiveData<Boolean> = _isTyping

    private val _currentLocation = MutableLiveData("")
    val currentLocation: LiveData<String> = _currentLocation

    var destination = MutableLiveData("")

    fun initIstTyping(typing: Boolean) {
        _isTyping.postValue(typing)
    }

    fun initDestination() {
        TMapData().findAllPOI(destination.value
        ) { poiItem ->
            destination.postValue(poiItem[0].poiName)
            initIstTyping(false)
        }

    }

    fun initCurrentLocation(location: String) {
        _currentLocation.postValue(location)
    }
}
