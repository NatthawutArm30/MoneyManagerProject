package com.example.moneymanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullname = findViewById<EditText>(R.id.edtFullname)
        val username = findViewById<EditText>(R.id.edtUsername)
        val password = findViewById<EditText>(R.id.edtPassword)
        val confirm = findViewById<EditText>(R.id.edtConfirmPassword)
        val register = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        register.setOnClickListener {
            val name = fullname.text.toString().trim()
            val userInput = username.text.toString().trim()
            val pass = password.text.toString()
            val confirmPass = confirm.text.toString()

            if (name.isEmpty() || userInput.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "รหัสผ่านไม่ตรงกัน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sanitize username (remove spaces) and convert to email format if not an email
            val cleanUser = userInput.replace("\\s+".toRegex(), "").lowercase()
            val email = if (userInput.contains("@")) userInput else "$cleanUser@moneymanager.app"

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (isFinishing || isDestroyed) return@addOnCompleteListener

                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        firebaseHelper.saveUserProfile(
                            uid = uid,
                            fullname = name,
                            username = userInput,
                            email = email
                        ) { success ->
                            auth.signOut() // Sign out so user logs in cleanly
                            if (isFinishing || isDestroyed) return@saveUserProfile
                            if (success) {
                                Toast.makeText(this, "สมัครสมาชิกสำเร็จ กรุณาเข้าสู่ระบบ", Toast.LENGTH_SHORT).show()
                                finish() // Return to LoginActivity
                            } else {
                                Toast.makeText(this, "เกิดข้อผิดพลาดในการบันทึกข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMsg = getFirebaseErrorMessage(task.exception)
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun getFirebaseErrorMessage(exception: Exception?): String {
        val message = exception?.message ?: ""
        return when {
            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "ยังไม่ได้เปิดใช้งาน Email/Password ใน Firebase Console\n(ไปที่ Firebase Console > Authentication > Sign-in method แล้วเปิด Email/Password)"
            message.contains("email-already-in-use", ignoreCase = true) || message.contains("EMAIL_EXISTS", ignoreCase = true) ->
                "ชื่อผู้ใช้หรืออีเมลนี้มีผู้ใช้งานแล้ว"
            message.contains("invalid-email", ignoreCase = true) ->
                "รูปแบบชื่อผู้ใช้หรืออีเมลไม่ถูกต้อง"
            message.contains("weak-password", ignoreCase = true) ->
                "รหัสผ่านไม่ปลอดภัย ต้องมีอย่างน้อย 6 ตัวอักษร"
            message.contains("network", ignoreCase = true) ->
                "เกิดข้อผิดพลาดในการเชื่อมต่ออินเทอร์เน็ต"
            else ->
                "สมัครสมาชิกไม่สำเร็จ: ${exception?.localizedMessage}"
        }
    }
}
