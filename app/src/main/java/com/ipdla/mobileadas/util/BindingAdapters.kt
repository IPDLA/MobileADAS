package com.ipdla.mobileadas.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.ipdla.mobileadas.R

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("setImage")
    fun setImage(imageview: ImageView, label: String?) {
        if (label != "") {
            Glide.with(imageview.context)
                .load(when (label) {
                    "caution_bump" -> R.drawable.img_sign_bump
                    "caution_danger" -> R.drawable.img_sign_danger
                    "instruction_people_only" -> R.drawable.img_sign_people_only
                    "instruction_children", "caution_children" -> R.drawable.img_sign_children
                    "instruction_crosswalk", "caution_crosswalk" -> R.drawable.img_sign_crosswalk
                    "restriction_speed30" -> R.drawable.img_sign_speed_30
                    "restriction_speed20" -> R.drawable.img_sign_speed_20
                    else -> null
                })
                .into(imageview)
        }
    }
}
