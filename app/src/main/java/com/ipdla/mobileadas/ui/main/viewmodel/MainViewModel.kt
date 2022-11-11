package com.ipdla.mobileadas.ui.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.task.vision.detector.Detection

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

    //지준호 추가
    private val _newTraffic = MutableLiveData("")
    var newTraffic: LiveData<String> = _newTraffic
    private var timeLeft: Int = 3   //이미지를 띄우는 남은 시간

    private var speedLimit = -1

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

    //지준호 추가
    fun initTraffic(sign: String){
        _newTraffic.postValue(sign)
    }
    fun setTime(time: Int){
        timeLeft = time
    }
    fun getTime(): Int {
        return timeLeft
    }

    fun setSpeedLimit(limit: Int){
        speedLimit = limit
    }
    fun getSpeedLimit(): Int{
        return speedLimit
    }
}