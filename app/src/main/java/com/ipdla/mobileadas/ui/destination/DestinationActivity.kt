package com.ipdla.mobileadas.ui.destination

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.viewModels
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.ActivityDestinationBinding
import com.ipdla.mobileadas.ui.base.BaseActivity
import com.ipdla.mobileadas.ui.destination.viewmodel.DestinationViewModel

class DestinationActivity :
    BaseActivity<ActivityDestinationBinding>(R.layout.activity_destination) {
    private val destinationViewModel by viewModels<DestinationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.destinationViewModel = destinationViewModel

        // 밑줄
        binding.tvDestinationLocation.paintFlags = Paint.UNDERLINE_TEXT_FLAG
    }
}
