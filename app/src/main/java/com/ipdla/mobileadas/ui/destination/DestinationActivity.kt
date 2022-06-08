package com.ipdla.mobileadas.ui.destination

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.location.LocationListener
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import com.ipdla.mobileadas.BuildConfig
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityDestinationBinding
import com.ipdla.mobileadas.ui.base.BaseActivity
import com.ipdla.mobileadas.ui.destination.viewmodel.DestinationViewModel
import com.ipdla.mobileadas.ui.main.MainActivity
import com.skt.Tmap.TMapTapi

class DestinationActivity :
    BaseActivity<ActivityDestinationBinding>(R.layout.activity_destination) {
    private val destinationViewModel by viewModels<DestinationViewModel>()
    private lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.destinationViewModel = destinationViewModel

        setTMapTapi()
        setLayout()
        initDestinationObserver()
        initFindBtnClickListener()
        initSetBtnClickListener()
        initResetBtnClickListener()
    }

    private fun setTMapTapi() {
        val tMapTapi = TMapTapi(this)
        tMapTapi.setSKTMapAuthentication(BuildConfig.TMAP_API_KEY)

    }

    private fun setLayout() {
        binding.tvDestinationLocation.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }

    private fun initDestinationObserver() {
        destinationViewModel.destination.observe(this) {
        }
    }

    private fun initFindBtnClickListener() {
        binding.btnDestinationFind.setOnClickListener {
            destinationViewModel.initDestination()
            val inputMethodManager =
                this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            this.currentFocus?.let { view ->
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
    }

    private fun initSetBtnClickListener() {
        binding.btnDestinationSet.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("isGuide", true)
                putExtra("destination", destinationViewModel.destination.value.toString())
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun initResetBtnClickListener() {
        binding.btnDestinationReset.setOnClickListener {
            destinationViewModel.initIstTyping(true)
        }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }
}
