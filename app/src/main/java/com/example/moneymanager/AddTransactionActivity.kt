package com.example.moneymanager

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseHelper = FirebaseHelper()

    private var userId = ""
    private var transactionId: String? = null
    private var selectedType = "income"

    private lateinit var txtTitle: TextView
    private lateinit var spType: Spinner
    private lateinit var spCategory: Spinner
    private lateinit var edtAmount: EditText
    private lateinit var edtDetail: EditText
    private lateinit var edtDate: EditText
    private lateinit var btnSave: Button

    private var categories = ArrayList<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }
        userId = currentUser.uid

        transactionId = intent.getStringExtra("transaction_id")

        txtTitle = findViewById(R.id.txtTitle)
        spType = findViewById(R.id.spType)
        spCategory = findViewById(R.id.spCategory)
        edtAmount = findViewById(R.id.edtAmount)
        edtDetail = findViewById(R.id.edtDetail)
        edtDate = findViewById(R.id.edtDate)
        btnSave = findViewById(R.id.btnSave)

        setupTypeSpinner()
        setupKeyboard(edtAmount)
        setupKeyboard(edtDetail)

        edtDate.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            saveTransaction()
        }

        if (!transactionId.isNullOrEmpty()) {
            loadTransaction(transactionId!!)
        } else {
            setCurrentDate()
            loadCategories("income")
        }
    }

    private fun setupKeyboard(editText: EditText) {
        editText.setOnClickListener {
            editText.requestFocus()
            editText.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

        editText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.postDelayed({
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                }, 100)
            }
        }
    }

    private fun setupTypeSpinner() {
        val types = arrayOf("รายรับ", "รายจ่าย")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        )

        spType.adapter = adapter
        spType.setSelection(0)

        spType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedType = if (position == 0) "income" else "expense"
                loadCategories(selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCategories(type: String) {
        categories = ArrayList(CategoryRepository.getCategories(type))
        val names = categories.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
        spCategory.adapter = adapter
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        edtDate.setText(dateFormat.format(Calendar.getInstance().time))
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )
                edtDate.setText(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun loadTransaction(id: String) {
        txtTitle.text = "แก้ไขรายการ"

        firebaseHelper.getTransaction(id) { transaction ->
            if (isFinishing || isDestroyed) return@getTransaction
            if (transaction == null) {
                Toast.makeText(this, "ไม่พบรายการ", Toast.LENGTH_SHORT).show()
                finish()
                return@getTransaction
            }

            selectedType = transaction.type
            if (transaction.type == "income" || transaction.type == "รายรับ") {
                spType.setSelection(0)
            } else {
                spType.setSelection(1)
            }

            edtAmount.setText(transaction.amount.toString())
            edtDetail.setText(transaction.detail)
            edtDate.setText(transaction.date)

            spCategory.postDelayed({
                for (i in categories.indices) {
                    if (categories[i].id == transaction.categoryId || categories[i].name == transaction.category) {
                        spCategory.setSelection(i)
                        break
                    }
                }
            }, 200)
        }
    }

    private fun saveTransaction() {
        val amountText = edtAmount.text.toString().trim()
        val detail = edtDetail.text.toString().trim()
        val date = edtDate.text.toString().trim()

        if (amountText.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกจำนวนเงิน", Toast.LENGTH_SHORT).show()
            edtAmount.requestFocus()
            return
        }

        if (categories.isEmpty()) {
            Toast.makeText(this, "ไม่พบหมวดหมู่", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "จำนวนเงินไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            edtAmount.requestFocus()
            return
        }

        val category = categories[spCategory.selectedItemPosition]

        val onResult: (Boolean) -> Unit = { success ->
            if (!isFinishing && !isDestroyed) {
                if (success) {
                    Toast.makeText(this, "บันทึกข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "ไม่สามารถบันทึกข้อมูลได้", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (transactionId.isNullOrEmpty()) {
            firebaseHelper.addTransaction(
                userId = userId,
                categoryId = category.id,
                type = selectedType,
                amount = amount,
                category = category.name,
                detail = detail,
                date = date,
                onComplete = onResult
            )
        } else {
            firebaseHelper.updateTransaction(
                transactionId = transactionId!!,
                userId = userId,
                categoryId = category.id,
                type = selectedType,
                amount = amount,
                category = category.name,
                detail = detail,
                date = date,
                onComplete = onResult
            )
        }
    }
}
