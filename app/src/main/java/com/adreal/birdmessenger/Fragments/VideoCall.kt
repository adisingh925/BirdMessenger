package com.adreal.birdmessenger.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adreal.birdmessenger.VideoCall.*
import com.adreal.birdmessenger.ViewModel.VideoCallViewModel
import com.adreal.birdmessenger.databinding.FragmentVideoCallBinding
import kotlinx.coroutines.*

class VideoCall : Fragment() {

    private val binding by lazy {
        FragmentVideoCallBinding.inflate(layoutInflater)
    }

    companion object {
        private const val CAMERA_AUDIO_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        const val TAG = "Video Call Fragment"
    }


    private val videoCallViewModel by lazy {
        ViewModelProvider(this)[VideoCallViewModel::class.java]
    }

    lateinit var receiverId: String
    lateinit var receiverToken: String
    lateinit var receiverName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        initValues()
        requestCameraAndAudioPermission()

        binding.switchCameraButton.setOnClickListener {

        }

        videoCallViewModel.readAllCalls.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {

            }
        }

        binding.audioOutputButton.setOnClickListener {

        }

        binding.videoButton.setOnClickListener {

        }

        binding.micButton.setOnClickListener {

        }

        binding.endCallButton.setOnClickListener {

            findNavController().popBackStack()
        }

        return binding.root
    }

    private fun initValues() {
        receiverId = arguments?.getString("receiverId", "").toString()
        receiverName = arguments?.getString("receiverName", "").toString()
        receiverToken = arguments?.getString("receiverToken", "").toString()
    }

    private fun requestCameraAndAudioPermission(dialogShown: Boolean = false) {
        if (shouldShowRequestPermissionRationale(CAMERA_PERMISSION) &&
            shouldShowRequestPermissionRationale(AUDIO_PERMISSION) &&
            !dialogShown
        ) {
            showPermissionRationaleDialog()
        } else {
            requestPermissions(
                arrayOf(CAMERA_PERMISSION, AUDIO_PERMISSION),
                CAMERA_AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showPermissionRationaleDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setTitle("Camera And Audio Permission Required")
                .setMessage("This app need the camera and audio to function")
                .setPositiveButton("Grant") { dialog, _ ->
                    dialog.dismiss()
                    requestCameraAndAudioPermission(true)
                }
                .setNegativeButton("Deny") { dialog, _ ->
                    dialog.dismiss()
                    onCameraPermissionDenied()
                }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_AUDIO_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//            onCameraAndAudioPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(context, "Camera and Audio Permission Denied", Toast.LENGTH_LONG).show()
    }
}