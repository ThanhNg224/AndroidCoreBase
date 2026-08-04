package com.example.androidcorebase.appshell.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.androidcorebase.databinding.FragmentAppshellHomeBinding
import com.thanhng224.androidcorebase.core.ui.base.BaseFragment

class HomeFragment : BaseFragment<FragmentAppshellHomeBinding>() {
    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentAppshellHomeBinding = FragmentAppshellHomeBinding.inflate(inflater, container, false)

    override fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    ) = Unit
}
