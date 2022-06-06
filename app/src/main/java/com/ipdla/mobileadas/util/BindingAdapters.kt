package com.ipdla.mobileadas.util

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("setImage")
    fun setImage(imageview: ImageView, url: String?) {
        Glide.with(imageview.context)
            .load(url)
            .into(imageview)
    }
}
