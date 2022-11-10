package com.ipdla.mobileadas.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _isCaution = MutableLiveData(false)
    val isCaution: LiveData<Boolean> = _isCaution

    private val _cautionLevel = MutableLiveData(0)
    val cautionLevel: LiveData<Int> = _cautionLevel

    private val _isGuide = MutableLiveData(false)
    val isGuide: LiveData<Boolean> = _isGuide

    private val _isSoundOn = MutableLiveData(true)
    val isSoundOn: LiveData<Boolean> = _isSoundOn

    private val _mainImg = MutableLiveData<String>()
    val mainImg: LiveData<String> = _mainImg

    private val _speed = MutableLiveData(0)
    val speed: LiveData<Int> = _speed

    private val _destination = MutableLiveData(" - ")
    val destination: LiveData<String> = _destination

    private val _distance = MutableLiveData(0)
    val distance: LiveData<Int> = _distance

    fun initIsCaution(isCaution: Boolean) {
        _isCaution.postValue(isCaution)
    }

    fun initIsGuide(isGuide: Boolean) {
        _isGuide.postValue(isGuide)
    }

    fun initCautionLevel(cautionLevel: Int) {
        _cautionLevel.postValue(cautionLevel)
    }

    fun initIsSoundOn(isSoundOn: Boolean) {
        _isSoundOn.postValue(isSoundOn)
    }

    fun initSpeed(speed: Int) {
        _speed.postValue(speed)
    }

    fun initDestination(destination: String?) {
        _destination.postValue(destination)
    }

    fun initDistance(distance: Int) {
        _distance.postValue(distance)
    }
}
