package com.example.budgetquest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView

object NavigationHelper {

    fun setupBottomNavigation(
        activity: Activity,
        userId: Int,
        currentPage: String
    ) {
        val navHome = activity.findViewById<TextView>(R.id.navHome)
        val navCategories = activity.findViewById<TextView>(R.id.navCategories)
        val navAddExpense = activity.findViewById<TextView>(R.id.navAddExpense)
        val navGoals = activity.findViewById<TextView>(R.id.navGoals)
        val navSavingsDebt = activity.findViewById<TextView>(R.id.navSavingsDebt)
        val navProfile = activity.findViewById<TextView>(R.id.navProfile)

        resetNavItem(navHome)
        resetNavItem(navCategories)
        resetNavItem(navGoals)
        resetNavItem(navSavingsDebt)
        resetNavItem(navProfile)

        when (currentPage) {
            "Home" -> setActiveItem(navHome)
            "Categories" -> setActiveItem(navCategories)
            "Goals" -> setActiveItem(navGoals)
            "Savings" -> setActiveItem(navSavingsDebt)
            "Profile" -> setActiveItem(navProfile)
        }

        navHome.setOnClickListener {
            if (currentPage != "Home") {
                openPage(activity, Home::class.java, userId)
            }
        }

        navCategories.setOnClickListener {
            if (currentPage != "Categories") {
                openPage(activity, Categories::class.java, userId)
            }
        }

        navAddExpense.setOnClickListener {
            if (currentPage != "AddExpense") {
                openPage(activity, Expenses::class.java, userId)
            }
        }

        navGoals.setOnClickListener {
            if (currentPage != "Goals") {
                openPage(activity, MonthlyGoals::class.java, userId)
            }
        }

        navSavingsDebt.setOnClickListener {
            if (currentPage != "Savings") {
                openPage(activity, SavingsDebt::class.java, userId)
            }
        }

        navProfile.setOnClickListener {
            if (currentPage != "Profile") {
                openPage(activity, Profile::class.java, userId)
            }
        }
    }

    private fun openPage(
        activity: Activity,
        destination: Class<*>,
        userId: Int
    ) {
        val intent = Intent(activity, destination)
        intent.putExtra("userId", userId)
        activity.startActivity(intent)
    }

    private fun resetNavItem(item: TextView) {
        item.setTextColor(Color.parseColor("#212121"))
        item.setTypeface(null, Typeface.NORMAL)
    }

    private fun setActiveItem(item: TextView) {
        item.setTextColor(Color.WHITE)
        item.setTypeface(null, Typeface.BOLD)
    }
}