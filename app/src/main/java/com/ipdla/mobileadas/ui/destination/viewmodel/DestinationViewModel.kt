package com.ipdla.mobileadas.ui.destination.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skt.Tmap.TMapData
import com.skt.Tmap.poi_item.TMapPOIItem

class DestinationViewModel : ViewModel() {
    private val _isTyping = MutableLiveData(true)
    val isTyping: LiveData<Boolean> = _isTyping

    private val _longitude = MutableLiveData<Double>()
    val longitude: LiveData<Double> = _longitude

    private val _latitude = MutableLiveData<Double>()
    val latitude: LiveData<Double> = _latitude

    var destination = MutableLiveData("")

    fun initIstTyping(typing: Boolean) {
        _isTyping.postValue(typing)
    }

    fun initDestination() {
        TMapData().findAllPOI(destination.value
        ) { poiItem ->
            destination.postValue(poiItem[0].poiName)
            _longitude.postValue(poiItem[0].frontLon.toDouble())
            _latitude.postValue(poiItem[0].frontLat.toDouble())
            initIstTyping(false)
        }
    }
}
