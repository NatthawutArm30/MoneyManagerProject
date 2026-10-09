package com.example.moneymanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val txtFullname = findViewById<TextView>(R.id.txtFullname)
        val txtUsername = findViewById<TextView>(R.id.txtUsername)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        firebaseHelper.getUserProfile(currentUser.uid) { profile ->
            txtFullname.text = "ชื่อ: ${profile?.fullname ?: "-"}"
            txtUsername.text = "ชื่อผู้ใช้: ${profile?.username ?: profile?.email ?: "-"}"
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            getSharedPreferences("session", MODE_PRIVATE).edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finishAffinity()
        }
    }
}
