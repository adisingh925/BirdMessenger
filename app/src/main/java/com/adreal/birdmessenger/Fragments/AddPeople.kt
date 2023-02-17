package com.adreal.birdmessenger.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.databinding.FragmentAddPeopleBinding


class AddPeople : Fragment() {

    private val binding by lazy {
        FragmentAddPeopleBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        return binding.root
    }
}