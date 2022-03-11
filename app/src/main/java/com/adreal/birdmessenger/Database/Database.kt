package com.adreal.birdmessenger.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.adreal.birdmessenger.Dao.Dao
import com.adreal.birdmessenger.Model.ChatModel
import com.adreal.birdmessenger.Model.UserModel

@Database(entities = [UserModel::class,ChatModel::class], version = 1, exportSchema = false)
abstract class Database : RoomDatabase(){

    abstract fun Dao() : Dao

    companion object{
        @Volatile
        private var INSTANCE : com.adreal.birdmessenger.Database.Database? = null

        fun getDatabase(context: Context): com.adreal.birdmessenger.Database.Database
        {
            val tempInstance = INSTANCE
            if(tempInstance!=null)
            {
                return tempInstance
            }
            synchronized(this)
            {
                val instance = Room.databaseBuilder(context.applicationContext,
                    com.adreal.birdmessenger.Database.Database::class.java,
                    "Database").build()

                INSTANCE = instance
                return instance
            }
        }
    }
}