package com.example.moneymanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class TransactionsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    private lateinit var list: ListView
    private var userId = ""
    private var transactions = ArrayList<FirebaseTransaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }
        userId = currentUser.uid

        list = findViewById(R.id.listTransactions)

        list.setOnItemClickListener { _, _, position, _ ->
            if (position < transactions.size) {
                val item = transactions[position]
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("transaction_id", item.id)
                startActivity(intent)
            }
        }

        list.setOnItemLongClickListener { _, _, position, _ ->
            if (position < transactions.size) {
                val item = transactions[position]
                AlertDialog.Builder(this)
                    .setTitle("ลบรายการ")
                    .setMessage("ต้องการลบรายการนี้หรือไม่?")
                    .setPositiveButton("ลบ") { _, _ ->
                        firebaseHelper.deleteTransaction(item.id) { success ->
                            if (isFinishing || isDestroyed) return@deleteTransaction
                            if (success) {
                                Toast.makeText(this, "ลบรายการเรียบร้อย", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "ไม่สามารถลบรายการได้", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("ยกเลิก", null)
                    .show()
            }
            true
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) {
            loadData()
        }
    }

    private fun loadData() {
        firebaseHelper.getTransactionsForUser(userId) { listData ->
            if (isFinishing || isDestroyed) return@getTransactionsForUser
            transactions = ArrayList(listData)
            list.adapter = TransactionAdapter(this, transactions)
        }
    }

    private class TransactionAdapter(
        context: Context,
        private val items: ArrayList<FirebaseTransaction>
    ) : ArrayAdapter<FirebaseTransaction>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.item_transaction, parent, false
            )

            val item = items[position]

            val imgType = view.findViewById<ImageView>(R.id.imgType)
            val txtCategory = view.findViewById<TextView>(R.id.txtCategory)
            val txtDetail = view.findViewById<TextView>(R.id.txtDetail)
            val txtDate = view.findViewById<TextView>(R.id.txtDate)
            val txtAmount = view.findViewById<TextView>(R.id.txtAmount)

            txtCategory.text = item.category
            txtDetail.text = if (item.detail.isEmpty()) "ไม่มีรายละเอียด" else item.detail
            txtDate.text = item.date

            val isIncome = item.type == "income" || item.type == "รายรับ"

            if (isIncome) {
                imgType.setImageResource(R.drawable.ic_income)
                imgType.setBackgroundResource(R.drawable.bg_income_card)
                txtAmount.text = "+%.2f ฿".format(item.amount)
                txtAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green))
            } else {
                imgType.setImageResource(R.drawable.ic_expense)
                imgType.setBackgroundResource(R.drawable.bg_expense_card)
                txtAmount.text = "-%.2f ฿".format(item.amount)
                txtAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
            }

            return view
        }
    }
}
