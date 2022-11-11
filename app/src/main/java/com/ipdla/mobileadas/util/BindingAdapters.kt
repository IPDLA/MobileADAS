package com.ipdla.mobileadas.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.ipdla.mobileadas.R

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("setImage")
    fun setImage(imageview: ImageView, label: String?) {
        Glide.with(imageview.context)
            .load(when (label) {
                "caution_bump" -> R.drawable.img_sign_bump
                "instruction_children" -> R.drawable.img_sign_children
                "instruction_crosswalk" -> R.drawable.img_sign_crosswalk
                "restriction_speed30" -> R.drawable.img_sign_speed_30
                else -> null
            })
            .into(imageview)
    }
}
