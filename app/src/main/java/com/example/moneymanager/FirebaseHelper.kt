package com.example.moneymanager

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class UserProfile(
    val uid: String = "",
    val fullname: String = "",
    val username: String = "",
    val email: String = ""
)

data class Category(
    val id: Int,
    val name: String,
    val type: String
)

object CategoryRepository {
    val incomeCategories = listOf(
        Category(1, "เงินเดือน", "income"),
        Category(2, "รายได้เสริม", "income"),
        Category(3, "เงินจากผู้ปกครอง", "income")
    )

    val expenseCategories = listOf(
        Category(101, "อาหาร", "expense"),
        Category(102, "เดินทาง", "expense"),
        Category(103, "ค่าเรียน", "expense"),
        Category(104, "ค่าน้ำค่าไฟ", "expense"),
        Category(105, "ช้อปปิ้ง", "expense"),
        Category(106, "บันเทิง", "expense"),
        Category(107, "อื่นๆ", "expense")
    )

    fun getCategories(type: String): List<Category> {
        return if (type == "income") incomeCategories else expenseCategories
    }
}

data class FirebaseTransaction(
    val id: String = "",
    val userId: String = "",
    val categoryId: Int = 0,
    val type: String = "", // "income" or "expense"
    val amount: Double = 0.0,
    val category: String = "",
    val detail: String = "",
    val date: String = ""
)

class FirebaseHelper {

    private val database = FirebaseDatabase.getInstance("https://moneymanagerproject-674244-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val usersRef = database.getReference("users")
    private val transactionsRef = database.getReference("transactions")

    fun saveUserProfile(
        uid: String,
        fullname: String,
        username: String,
        email: String,
        onComplete: (Boolean) -> Unit
    ) {
        val user = UserProfile(
            uid = uid,
            fullname = fullname,
            username = username,
            email = email
        )
        usersRef.child(uid).setValue(user)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getUserProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        usersRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                onResult(profile)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(null)
            }
        })
    }

    fun addTransaction(
        userId: String,
        categoryId: Int,
        type: String,
        amount: Double,
        category: String,
        detail: String,
        date: String,
        onComplete: (Boolean) -> Unit
    ) {
        val key = transactionsRef.push().key ?: return onComplete(false)

        val transaction = FirebaseTransaction(
            id = key,
            userId = userId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            category = category,
            detail = detail,
            date = date
        )

        transactionsRef.child(key).setValue(transaction)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateTransaction(
        transactionId: String,
        userId: String,
        categoryId: Int,
        type: String,
        amount: Double,
        category: String,
        detail: String,
        date: String,
        onComplete: (Boolean) -> Unit
    ) {
        val transaction = FirebaseTransaction(
            id = transactionId,
            userId = userId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            category = category,
            detail = detail,
            date = date
        )

        transactionsRef.child(transactionId).setValue(transaction)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getTransaction(
        transactionId: String,
        onResult: (FirebaseTransaction?) -> Unit
    ) {
        transactionsRef.child(transactionId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = snapshot.getValue(FirebaseTransaction::class.java)
                    onResult(item)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            }
        )
    }

    fun getTransactionsForUser(
        userId: String,
        onDataChanged: (List<FirebaseTransaction>) -> Unit
    ) {
        transactionsRef.orderByChild("userId").equalTo(userId)
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<FirebaseTransaction>()
                        for (child in snapshot.children) {
                            val item = child.getValue(FirebaseTransaction::class.java)
                            if (item != null) {
                                list.add(item)
                            }
                        }
                        // Sort by date descending
                        list.sortByDescending { it.date }
                        onDataChanged(list)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
            )
    }

    fun deleteTransaction(transactionId: String, onComplete: (Boolean) -> Unit) {
        transactionsRef.child(transactionId).removeValue()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
