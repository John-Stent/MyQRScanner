package com.istentoumis.myqrscanner

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.istentoumis.myqrscanner.databinding.ActivityMessageBinding


class MessageActivity : Activity() {
    private lateinit var binding: ActivityMessageBinding

    private lateinit var databaseRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://myqr-5607b-default-rtdb.europe-west1.firebasedatabase.app")

        val token = intent.getStringExtra("token").toString()
        val userID = intent.getStringExtra("userID").toString()
        val invalid = intent.getStringExtra("invalid").toString()

        databaseRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("Clients").child(userID).child("Valid Subscription").value.toString() == "yes"){
                    binding.textMessage.text = "Valid"
                    binding.imageView.setImageResource(R.drawable.green_check)
                    //binding.textName.text = snapshot.child("Clients").child(userID).child("Name").value.toString()
                    //binding.textSurname.text = snapshot.child("Clients").child(userID).child("Surname").value.toString()


                    // Delete token child after elapsed time
                    deleteToken(token);
                }
                else if(invalid == "yes"){
                    binding.textMessage.text = "No such user exists"
                    binding.imageView.setImageResource(R.drawable.imageedit_3_2234035677_r)
                }
                else{
                    binding.textMessage.text = "No valid subscription"
                    binding.imageView.setImageResource(R.drawable.imageedit_3_2234035677_y)
                    //binding.textName.text = snapshot.child("Clients").child(userID).child("Name").value.toString()
                    //binding.textSurname.text = snapshot.child("Clients").child(userID).child("Surname").value.toString()


                    // Delete token child after elapsed time
                    deleteToken(token);
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
                Toast.makeText(this@MessageActivity, "Database access denied. Please try again with another QR",
                    Toast.LENGTH_LONG).show()
            }
        })


    }

    private fun deleteToken(string: String){
        Handler(Looper.getMainLooper()).postDelayed({
            databaseRef.child("Tokens").child(string).removeValue()
        }, 20000) //20 seconds
    }

    override fun onBackPressed() {
        val intent = Intent(this@MessageActivity, MainActivity::class.java)
        startActivity(intent)
    }
}