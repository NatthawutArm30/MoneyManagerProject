package com.example.moneymanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cleanUser = username.replace("\\s+".toRegex(), "").lowercase()
            val email = if (username.contains("@")) username else "$cleanUser@moneymanager.app"

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (isFinishing || isDestroyed) return@addOnCompleteListener

                    if (task.isSuccessful) {
                        Toast.makeText(this, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMsg = getFirebaseErrorMessage(task.exception)
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun getFirebaseErrorMessage(exception: Exception?): String {
        val message = exception?.message ?: ""
        return when {
            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "ยังไม่ได้เปิดใช้งาน Email/Password ใน Firebase Console\n(ไปที่ Firebase Console > Authentication > Sign-in method แล้วเปิด Email/Password)"
            message.contains("user-not-found", ignoreCase = true) ||
            message.contains("invalid-credential", ignoreCase = true) ||
            message.contains("wrong-password", ignoreCase = true) ->
                "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง"
            message.contains("invalid-email", ignoreCase = true) ->
                "รูปแบบชื่อผู้ใช้ไม่ถูกต้อง"
            message.contains("network", ignoreCase = true) ->
                "เกิดข้อผิดพลาดในการเชื่อมต่ออินเทอร์เน็ต"
            else ->
                "เข้าสู่ระบบไม่สำเร็จ: ${exception?.localizedMessage}"
        }
    }
}
