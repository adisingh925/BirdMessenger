package com.adreal.birdmessenger.Fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.ViewModel.VideoCallViewModel
import com.adreal.birdmessenger.databinding.FragmentVideoCallBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.RtcEngineConfig
import io.agora.rtc.models.ChannelMediaOptions
import io.agora.rtc.video.VideoCanvas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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

    private val appId = "489ab3de83944e87b7ffd278c4ccf35b"
    private val channelName = "birdMessenger"
    private val token = "007eJxTYEg65bDRd0tn6sHJ6uUdjgxPc1o0OzLntq3wcH1npsL64ZsCg4mFZWKScUqqhbGliUmqhXmSeVpaipG5RbJJcnKasWlS6xyZlIZARoZqD0FGRgYIBPF5GZIyi1J8U4uLU/PSU4sYGABUuyIy"
    private val uid = 0
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private var isAudioOn = true
    private var isVideoOn = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        initValues()

        if (context?.let { checkSelfPermission(it,CAMERA_PERMISSION) }
            == PackageManager.PERMISSION_GRANTED && context?.let { checkSelfPermission(it,CAMERA_PERMISSION) }
            == PackageManager.PERMISSION_GRANTED) {
            showMessage("all permission granted")
            setupVideoSDKEngine()
            joinChannel()

        }else{
            requestCameraAndAudioPermission()
        }

        binding.switchCameraButton.setOnClickListener {
            agoraEngine?.switchCamera()
        }

        videoCallViewModel.readAllCalls.observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {

            }
        }

        binding.audioOutputButton.setOnClickListener {

        }

        binding.videoButton.setOnClickListener {
            isVideoOn = if(isVideoOn){
                agoraEngine?.muteLocalVideoStream(true)
                binding.videoButton.setImageResource(R.drawable.video_off)
                false
            }else{
                agoraEngine?.muteLocalVideoStream(false)
                binding.videoButton.setImageResource(R.drawable.video)
                true
            }
        }

        binding.micButton.setOnClickListener {
            isAudioOn = if(isAudioOn){
                agoraEngine?.muteLocalAudioStream(true)
                binding.micButton.setImageResource(R.drawable.mic_off)
                false
            }else{
                agoraEngine?.muteLocalAudioStream(false)
                binding.micButton.setImageResource(R.drawable.mic)
                true
            }
        }

        binding.endCallButton.setOnClickListener {
            leaveChannel()
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
            onCameraAndAudioPermissionGranted()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(context, "Camera and Audio Permission Denied", Toast.LENGTH_LONG).show()
    }

    private fun onCameraAndAudioPermissionGranted(){
        setupVideoSDKEngine()
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")

            // Set the remote video view
            CoroutineScope(Dispatchers.Main).launch {
                setupRemoteVideo(uid)
            }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")

            CoroutineScope(Dispatchers.Main).launch {
                remoteSurfaceView!!.visibility = View.GONE
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container: FrameLayout = binding.remoteView
        remoteSurfaceView = SurfaceView(context)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container: FrameLayout = binding.localView
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(context)
        container.addView(localSurfaceView)
        // Call setupLocalVideo with a VideoCanvas having uid set to 0.
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun joinChannel() {
        val options = ChannelMediaOptions()
        // For a Video call, set the channel profile as COMMUNICATION.
//        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
//        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Display LocalSurfaceView.
        setupLocalVideo()
        localSurfaceView!!.visibility = View.VISIBLE
        // Start local preview.
        agoraEngine!!.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine!!.joinChannel(token, channelName,"hi", uid, options)
    }

    private fun leaveChannel() {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            // Stop remote video rendering.
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            // Stop local video rendering.
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }

    fun showMessage(msg : String){
        Log.d("Video Call",msg)
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine!!.stopPreview()
        agoraEngine!!.leaveChannel()
        CoroutineScope(Dispatchers.IO).launch {
            RtcEngine.destroy()
            agoraEngine = null
        }
    }
}