package com.ipdla.mobileadas.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _isCaution = MutableLiveData(false)
    val isCaution: LiveData<Boolean> = _isCaution

    private val _isGuide = MutableLiveData(false)
    val isGuide: LiveData<Boolean> = _isGuide

    private val _isSoundOn = MutableLiveData(true)
    val isSoundOn: LiveData<Boolean> = _isSoundOn

    private val _mainImg = MutableLiveData<String>()
    val mainImg: LiveData<String> = _mainImg

    private val _subImg = MutableLiveData<String>()
    val subImg: LiveData<String> = _subImg

    private val _speed = MutableLiveData(0)
    val speed: LiveData<Int> = _speed

    private val _destination = MutableLiveData(" - ")
    val destination: LiveData<String> = _destination

    private val _time = MutableLiveData(" - ")
    val time: LiveData<String> = _time

    fun initIsGuide(isGuide: Boolean) {
        _isGuide.postValue(isGuide)
    }

    fun initIsSoundOn(isSoundOn: Boolean) {
        _isSoundOn.postValue(isSoundOn)
    }

    fun initSpeed(speed: Int) {
        _speed.postValue(speed)
    }

    fun initDestination(destination: String) {
        _destination.postValue(destination)
    }

    fun initTime(time: String) {
        _time.postValue(time)
    }
}
