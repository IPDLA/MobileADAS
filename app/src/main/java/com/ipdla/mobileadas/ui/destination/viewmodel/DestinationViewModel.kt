package com.ipdla.mobileadas.ui.destination.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skt.Tmap.TMapData
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            kotlin.runCatching {
                TMapData().findAllPOI(destination.value
                )
            }.onSuccess { poiItem ->
                destination.postValue(poiItem[0].poiName)
                _longitude.postValue(poiItem[0].frontLon.toDouble())
                _latitude.postValue(poiItem[0].frontLat.toDouble())
                initIstTyping(false)
            }.onFailure {

            }
        }
    }
}
