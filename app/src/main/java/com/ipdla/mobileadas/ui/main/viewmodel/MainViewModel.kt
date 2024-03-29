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

    private val _speed = MutableLiveData(0)
    val speed: LiveData<Int> = _speed

    private val _destination = MutableLiveData(" - ")
    val destination: LiveData<String> = _destination

    private val _distance = MutableLiveData(0)
    val distance: LiveData<Int> = _distance

    private val _trafficSign = MutableLiveData("")
    var trafficSign: LiveData<String> = _trafficSign

    private var timeLeft = 15
    private var sameSignLeft = 5 //0.5초간 같은 것 감지 시
    private var tempTrafficSign = ""

    private var speedLimit = 1000

    private fun initIsCaution(isCaution: Boolean) {
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
        if (speed > speedLimit) {
            initIsCaution(true)
        } else {
            initIsCaution(false)
        }
    }

    fun initDestination(destination: String?) {
        _destination.postValue(destination)
    }

    fun initDistance(distance: Int) {
        _distance.postValue(distance)
    }

    fun initTrafficSign(sign: String) {
        _trafficSign.postValue(sign)
        setSpeedLimit(when (sign) {
            "restriction_speed20" -> 20
            "caution_children", "instruction_children", "restriction_speed30" -> 30
            else -> 1000
        })
    }

    fun setTime(time: Int) {
        timeLeft = time
    }

    fun getTime(): Int {
        return timeLeft
    }

    fun setSameSign(time: Int){
        sameSignLeft = time
    }

    fun getSameSign(): Int{
        return sameSignLeft
    }

    fun setTempSign(sign: String){
        tempTrafficSign = sign
    }
    fun getTempSign(): String{
        return tempTrafficSign
    }

    private fun setSpeedLimit(limit: Int) {
        speedLimit = limit
    }
}
