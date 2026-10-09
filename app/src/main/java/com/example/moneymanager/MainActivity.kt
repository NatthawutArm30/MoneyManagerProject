package com.example.moneymanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = currentUser.uid

        // Load user name
        firebaseHelper.getUserProfile(uid) { profile ->
            if (isFinishing || isDestroyed) return@getUserProfile
            val fullname = profile?.fullname ?: "ผู้ใช้งาน"
            findViewById<TextView>(R.id.txtWelcome).text = "สวัสดี $fullname"
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        findViewById<Button>(R.id.btnTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        findViewById<Button>(R.id.btnSummary).setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateSummary(uid)
    }

    override fun onResume() {
        super.onResume()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            updateSummary(uid)
        }
    }

    private fun updateSummary(uid: String) {
        firebaseHelper.getTransactionsForUser(uid) { transactions ->
            if (isFinishing || isDestroyed) return@getTransactionsForUser
            var income = 0.0
            var expense = 0.0

            for (item in transactions) {
                if (item.type == "income" || item.type == "รายรับ") {
                    income += item.amount
                } else {
                    expense += item.amount
                }
            }

            val balance = income - expense

            findViewById<TextView>(R.id.txtIncome).text = "%.2f บาท".format(income)
            findViewById<TextView>(R.id.txtExpense).text = "%.2f บาท".format(expense)
            findViewById<TextView>(R.id.txtBalance).text = "%.2f บาท".format(balance)
        }
    }
}
