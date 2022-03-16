package com.adreal.birdmessenger.Activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adreal.birdmessenger.Adapter.PeopleAdapter
import com.adreal.birdmessenger.Model.UserModel
import com.adreal.birdmessenger.R
import com.adreal.birdmessenger.ViewModel.OfflineViewModel
import com.adreal.birdmessenger.ViewModel.OnlineViewModel
import com.adreal.birdmessenger.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.judemanutd.autostarter.AutoStartPermissionHelper
import java.io.ByteArrayOutputStream
import java.lang.Boolean
import kotlin.Int
import kotlin.String
import kotlin.toString


class PeopleActivity : AppCompatActivity(), PeopleAdapter.OnItemClickListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var onlineViewModel : OnlineViewModel

    private lateinit var offlineViewModel: OfflineViewModel

    private lateinit var dialog: Dialog

    lateinit var adapter: PeopleAdapter

    lateinit var recyclerView: RecyclerView

    lateinit var imageString: String

    lateinit var manufacturer : String

    private var auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_BirdMessenger)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onlineViewModel = ViewModelProvider(this).get(OnlineViewModel::class.java)

        offlineViewModel = ViewModelProvider(this).get(OfflineViewModel::class.java)

        binding.toolbar.inflateMenu(R.menu.toprightmenu)

        initDialog()

        checkForName()

        initRecycler()

        initImage()

        optimizeFCMForChineseDevices()

        offlineViewModel.readAllUsers.observe(this)
        {
            adapter.setData(it)
        }

        binding.fab.setOnClickListener()
        {
            showCustomDialog("Enter The Key","Connect")
        }

        binding.toolbar.setOnMenuItemClickListener()
        {
            when(it.itemId)
            {
                R.id.settings ->{
                    val builder = AlertDialog.Builder(this)
                    builder.setNegativeButton("ok"){_,_->

                    }
                    builder.setTitle("Underdevelopment")
                    builder.setMessage("This feature will be available soon")
                    builder.create().show()
                }

                R.id.key ->{
                    val intent= Intent()
                    intent.action=Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_TEXT,auth.uid)
                    intent.type="text/*"
                    startActivity(Intent.createChooser(intent,"Share To:"))
                }
            }
            true
        }
    }

    private fun initRecycler() {
        adapter = PeopleAdapter(this,this)
        recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun optimizeFCMForChineseDevices()
    {
//        val intent = Intent()

        manufacturer = android.os.Build.MANUFACTURER

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val previouslyStarted = prefs.getBoolean("previously_started", false)
        if (!previouslyStarted) {
            val edit = prefs.edit()
            edit.putBoolean("previously_started", Boolean.TRUE)
            edit.apply()

            if(AutoStartPermissionHelper.getInstance().isAutoStartPermissionAvailable(this))
            {
                AutoStartPermissionHelper.getInstance().getAutoStartPermission(this)
            }
        }

//        when(manufacturer)
//        {
//            "xiaomi" ->{
//                intent.component = ComponentName(
//                    "com.miui.securitycenter",
//                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
//                )
//            }
//
//            "oppo" ->{
//                intent.component = ComponentName(
//                    "com.coloros.safecenter",
//                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
//                )
//            }
//
//            "vivo" ->{
//                intent.component = ComponentName(
//                    "com.vivo.permissionmanager",
//                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
//                )
//            }
//        }

//        val arrayList = packageManager.queryIntentActivities(
//            intent,
//            PackageManager.MATCH_DEFAULT_ONLY
//        )
//
//        if (arrayList.size > 0) {
//            startActivity(intent);
//        }
    }

    private fun initImage()
    {
        val bitmap = cropToSquare(BitmapFactory.decodeResource(this.resources, R.drawable.ic_stat_person))
        val image = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, image)
        val imageByteArray = image.toByteArray()
        imageString = Base64.encodeToString(imageByteArray, Base64.DEFAULT)
        Log.d("BASE64 of image is",imageString)
    }

    private fun initDialog() {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    private fun checkForName() {
        val sharedPreferences = this.getSharedPreferences("myData",Context.MODE_PRIVATE)
        val name = sharedPreferences.getString("name",null)
        if(name == null)
        {
            showCustomDialog("Enter Your Name","Set")
        }
    }

    private fun setNameAndImage(name : String)
    {
        val sharedPreferences = this.getSharedPreferences("myData",Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString("name",name)
        edit.putString("image",imageString)
        edit.apply()
    }

    private fun showCustomDialog(hint : String, buttonText : String){
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.add_user_dialog)
        val key = dialog.findViewById<EditText>(R.id.key)
        val button = dialog.findViewById<Button>(R.id.connect)
        val image = dialog.findViewById<ImageView>(R.id.imageView)
        val edit = dialog.findViewById<FloatingActionButton>(R.id.edit)
        val layout = dialog.findViewById<TextInputLayout>(R.id.layout1)
        val textview = dialog.findViewById<TextView>(R.id.textView1)
        layout.hint = hint
        button.text = buttonText
        if(button.text.equals("Set"))
        {
            image.isVisible = true
            Glide.with(this).load(R.drawable.ic_stat_person).into(dialog.findViewById(R.id.imageView))
            edit.isVisible = true
            textview.isVisible = false
            dialog.setCancelable(false)
        }
        else
        {
            image.isVisible = false
            edit.isVisible = false
            textview.isVisible = true
            dialog.setCancelable(true)
        }

        edit.setOnClickListener()
        {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, 100)
        }

        button.setOnClickListener()
        {
            if(button.text.toString() == "Set")
            {
                if(image.drawable != null && key.text.isNotEmpty())
                {
                    setNameAndImage(key.text.toString())
                    dialog.dismiss()
                    onlineViewModel.uploadImage(imageString)
                }
                else
                {
                    Toast.makeText(this,"Please set both name and image",Toast.LENGTH_SHORT).show()
                }
            }
            else
            {
                if(key.text.isNotEmpty())
                {
                    onlineViewModel.addUser(key.text.toString())
                    Log.d("key is",key.text.toString())
                    dialog.dismiss()
                }
                else
                {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        dialog.dismiss()
        onlineViewModel.setStatus("offline")
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 100)
        {
            if (data != null) {
                val imageUri = data.data
                Glide.with(this).load(imageUri).circleCrop().into(dialog.findViewById(R.id.imageView))
                val bitmap = cropToSquare(MediaStore.Images.Media.getBitmap(contentResolver, imageUri))
                val image = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, image)
                val imageByteArray = image.toByteArray()
                imageString = Base64.encodeToString(imageByteArray, Base64.DEFAULT)
                Log.d("BASE64 of image is",imageString)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showImageDialog(image : String)
    {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.image_dialog)
        dialog.setCancelable(true)
        val imageView = dialog.findViewById<ImageView>(R.id.imageView)
        Glide.with(this).asBitmap().load(base64ToBitmap(image)).centerCrop().into(imageView)
        dialog.show()
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = if (height > width) width else height
        val newHeight = if (height > width) height - (height - width) else height
        var cropW = (width - height) / 2
        cropW = if (cropW < 0) 0 else cropW
        var cropH = (height - width) / 2
        cropH = if (cropH < 0) 0 else cropH
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight)
    }

    private fun base64ToBitmap(data: String): Bitmap {
        val imageBytes = Base64.decode(data, 0)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onItemClick(data: UserModel) {
        showImageDialog(data.imageByteArray.toString())
    }

    override fun onStart() {
        //onlineViewModel.setStatus("online")
        super.onStart()
    }

    override fun onPause() {
        //onlineViewModel.setStatus("offline")
        super.onPause()
    }

    override fun onRestart() {
        //onlineViewModel.setStatus("online")
        super.onRestart()
    }

    override fun onResume() {
        //onlineViewModel.setStatus("online")
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }
}