package com.ipdla.mobileadas.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.ipdla.mobileadas.R
import com.ipdla.mobileadas.databinding.FragmentMainDialogBinding

class MainDialogFragment(private val doAfterConfirm: () -> Unit) : DialogFragment() {
    private var _binding: FragmentMainDialogBinding? = null
    private val binding get() = _binding ?: error("바인딩 에러")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_main_dialog, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initConfirmTextClickListener()
        initCancelTextClickListener()
        setLayout()
    }

    private fun setLayout() {
        requireNotNull(dialog).apply {
            requireNotNull(window).apply {
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.91).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundDrawableResource(R.drawable.shape_white_box)
            }
        }
    }

    private fun initConfirmTextClickListener() {
        binding.tvMainDialogConfirm.setOnClickListener {
            doAfterConfirm()
            dismiss()
        }
    }

    private fun initCancelTextClickListener() {
        binding.tvMainDialogCancel.setOnClickListener {
            dismiss()
        }
    }
}
