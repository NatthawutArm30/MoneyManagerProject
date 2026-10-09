package com.example.moneymanager

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        val uid = currentUser.uid

        val monthDisplay = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("th-TH")).format(Date())
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        findViewById<TextView>(R.id.txtMonth).text = "สรุปประจำเดือน $monthDisplay"

        firebaseHelper.getTransactionsForUser(uid) { transactions ->
            if (isFinishing || isDestroyed) return@getTransactionsForUser
            var monthlyIncome = 0.0
            var monthlyExpense = 0.0
            var allIncome = 0.0
            var allExpense = 0.0

            for (item in transactions) {
                val isIncome = item.type == "income" || item.type == "รายรับ"
                val isCurrentMonth = item.date.startsWith(currentYearMonth)

                if (isIncome) {
                    allIncome += item.amount
                    if (isCurrentMonth) {
                        monthlyIncome += item.amount
                    }
                } else {
                    allExpense += item.amount
                    if (isCurrentMonth) {
                        monthlyExpense += item.amount
                    }
                }
            }

            val monthlyBalance = monthlyIncome - monthlyExpense

            findViewById<TextView>(R.id.txtMonthlyIncome).text =
                "รายรับเดือนนี้\n%.2f บาท".format(monthlyIncome)

            findViewById<TextView>(R.id.txtMonthlyExpense).text =
                "รายจ่ายเดือนนี้\n%.2f บาท".format(monthlyExpense)

            findViewById<TextView>(R.id.txtMonthlyBalance).text =
                "คงเหลือเดือนนี้\n%.2f บาท".format(monthlyBalance)

            findViewById<TextView>(R.id.txtAllIncome).text =
                "รายรับทั้งหมด\n%.2f บาท".format(allIncome)

            findViewById<TextView>(R.id.txtAllExpense).text =
                "รายจ่ายทั้งหมด\n%.2f บาท".format(allExpense)
        }
    }
}
