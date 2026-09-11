package com.paradox.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paradox.app.core.database.ParadoxDatabase
import com.paradox.app.data.local.dao.ExpenseDao
import com.paradox.app.data.local.dao.ProfileDao
import com.paradox.app.data.local.entity.ExpenseEntity
import com.paradox.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ProfileIsolationTest {

    private lateinit var db: ParadoxDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ParadoxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        profileDao = db.profileDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun verifyStrictProfileDataIsolation() = runBlocking {
        // Setup Profile A
        val profileA = ProfileEntity(
            id = "profile_A",
            name = "Alice",
            primaryAuthType = "PIN",
            credentialHash = "hashA",
            biometricEnabled = false
        )
        profileDao.insertProfile(profileA)

        // Setup Profile B
        val profileB = ProfileEntity(
            id = "profile_B",
            name = "Bob",
            primaryAuthType = "PIN",
            credentialHash = "hashB",
            biometricEnabled = false
        )
        profileDao.insertProfile(profileB)

        // Add expense to Profile A
        val expenseA = ExpenseEntity(
            id = "exp_A1",
            profileId = "profile_A",
            title = "Alice Secret Expense",
            amount = BigDecimal("1500.00"),
            currency = "INR",
            categoryId = "cat_food",
            paymentMethodId = "pm_upi",
            date = LocalDate.now(),
            createdAt = Instant.now()
        )
        expenseDao.insertExpense(expenseA)

        // Add expense to Profile B
        val expenseB = ExpenseEntity(
            id = "exp_B1",
            profileId = "profile_B",
            title = "Bob Secret Expense",
            amount = BigDecimal("2500.00"),
            currency = "INR",
            categoryId = "cat_shopping",
            paymentMethodId = "pm_card",
            date = LocalDate.now(),
            createdAt = Instant.now()
        )
        expenseDao.insertExpense(expenseB)

        // Query expenses for Profile A
        val profileAExpenses = expenseDao.getAllExpenses("profile_A").first()
        assertEquals(1, profileAExpenses.size)
        assertEquals("Alice Secret Expense", profileAExpenses.first().title)
        assertTrue(profileAExpenses.none { it.profileId == "profile_B" })

        // Query expenses for Profile B
        val profileBExpenses = expenseDao.getAllExpenses("profile_B").first()
        assertEquals(1, profileBExpenses.size)
        assertEquals("Bob Secret Expense", profileBExpenses.first().title)
        assertTrue(profileBExpenses.none { it.profileId == "profile_A" })
    }
}
