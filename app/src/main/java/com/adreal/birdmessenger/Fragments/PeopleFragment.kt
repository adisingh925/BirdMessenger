package com.adreal.birdmessenger.Fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adreal.birdmessenger.Activity.Info
import com.adreal.birdmessenger.Adapter.PeopleAdapter
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import com.adreal.birdmessenger.ViewModel.OfflineViewModel
import com.adreal.birdmessenger.ViewModel.PeopleViewModel
import com.adreal.birdmessenger.databinding.AddUserDialogBinding
import com.adreal.birdmessenger.databinding.FragmentPeopleBinding
import com.bumptech.glide.Glide
import io.noties.markwon.Markwon
import java.io.ByteArrayOutputStream

class PeopleFragment : Fragment(), PeopleAdapter.OnItemClickListener {

    private val binding by lazy {
        FragmentPeopleBinding.inflate(layoutInflater)
    }

    private val peopleViewModel by lazy {
        ViewModelProvider(this)[PeopleViewModel::class.java]
    }

    private val offlineViewModel by lazy {
        ViewModelProvider(this)[OfflineViewModel::class.java]
    }

    private val recyclerView by lazy {
        binding.recyclerView
    }

    private val adapter by lazy {
        context?.let { PeopleAdapter(it, this) }!!
    }

    private lateinit var dialog: Dialog
    private lateinit var imageString: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding.toolbar.inflateMenu(R.menu.toprightmenu)

        binding.fab.isEnabled = false

        initDialog()
        initRecycler()

        binding.toolbar.setOnMenuItemClickListener()
        {
            when (it.itemId) {
                R.id.settings -> {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setNegativeButton("ok") { _, _ ->

                    }
                    builder.setTitle("Underdevelopment")
                    builder.setMessage("This feature will be available soon")
                    builder.create().show()
                }

                R.id.key -> {
                    val intent = Intent()
                    intent.action = Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_TEXT, SharedPreferences.read("installationId", ""))
                    intent.type = "text/*"
                    startActivity(Intent.createChooser(intent, "Share To:"))
                }

                R.id.info -> {
                    val intent = Intent(context, Info::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        offlineViewModel.readAllUsers.observe(viewLifecycleOwner) {
            adapter.setData(it)
        }

        binding.fab.setOnClickListener {
//            findNavController().navigate(R.id.action_peopleFragment_to_addPeople)
            showCustomDialog("Enter The Key", "Connect")
        }

        checkForName()

        return binding.root
    }

    private fun initRecycler() {
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun initDialog() {
        dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    private fun checkForName() {
        if (SharedPreferences.read("name", "") == "") {
            showCustomDialog("Enter Your Name", "Set")
        } else {
            binding.fab.isEnabled = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCustomDialog(hint: String, buttonText: String) {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val bind = AddUserDialogBinding.inflate(layoutInflater)
        dialog.setContentView(bind.root)

        bind.layout1.hint = hint
        bind.connect.text = buttonText

        if (buttonText == "Set") {
            bind.imageView.isVisible = true

            Glide
                .with(this)
                .load(R.drawable.programmer)
                .into(bind.imageView)

            bind.edit.isVisible = true
            bind.textView1.isVisible = false
            dialog.setCancelable(false)
        } else {
            bind.imageView.isVisible = false
            bind.edit.isVisible = false
            bind.textView1.isVisible = true
            dialog.setCancelable(true)
        }

        bind.edit.setOnClickListener()
        {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, 100)
        }

        bind.connect.setOnClickListener()
        {
            if (buttonText == "Set") {
                if (bind.imageView.drawable != null && bind.key.text?.isNotEmpty() == true) {
                    if(!this::imageString.isInitialized){
                        val bitmap = peopleViewModel.cropToSquare(
                            BitmapFactory.decodeResource(resources, R.drawable.programmer)
                        )
                        bitmapToString(bitmap)
                    }

                    peopleViewModel.storeNameAndImage(bind.key.text.toString(), imageString)
                    peopleViewModel.uploadImage(imageString)
                    peopleViewModel.uploadName(bind.key.text.toString())
                    dialog.dismiss()
                    binding.fab.isEnabled = true
                }
            } else {
                if (bind.key.text?.isNotEmpty() == true) {
                    peopleViewModel.addUser(bind.key.text.toString().trim(), requireContext())
                    Log.d("key is", bind.key.text.toString())
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100) {
            if (data != null) {
                val imageUri = data.data

                Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(dialog.findViewById(R.id.imageView))

                val bitmap = peopleViewModel.cropToSquare(
                    MediaStore.Images.Media.getBitmap(
                        context?.contentResolver,
                        imageUri
                    )
                )

                bitmapToString(bitmap)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun bitmapToString(bitmap : Bitmap){
        val image = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, image)
        val imageByteArray = image.toByteArray()
        imageString = Base64.encodeToString(imageByteArray, Base64.DEFAULT)
        Log.d("BASE64 of image is", imageString)
    }

    private fun showImageDialog(image: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.image_dialog)
        dialog.setCancelable(true)
        val imageView = dialog.findViewById<ImageView>(R.id.imageView)
        Glide.with(this).asBitmap().load(peopleViewModel.base64ToBitmap(image)).centerCrop()
            .into(imageView)
        dialog.show()
    }

    override fun onItemClick(data: UserModel, type: Int) {
        if (type == 0) {
            showImageDialog(data.imageByteArray.toString())
        } else {
            val bundle = Bundle()
            bundle.putString("receiverName", data.userName)
            bundle.putString("receiverId", data.Id)
            bundle.putString("receiverToken", data.userToken)
            findNavController().navigate(R.id.action_peopleFragment_to_chatFragment, bundle)
        }
    }
}