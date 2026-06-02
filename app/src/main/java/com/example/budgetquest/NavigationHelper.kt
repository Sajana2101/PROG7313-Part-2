package com.example.budgetquest

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView

object NavigationHelper {

    // Passes the authenticated Firebase UID between converted app screens.
    fun setupBottomNavigation(
        activity: Activity,
        userUid: String,
        currentPage: String
    ) {
        val views = findNavigationViews(activity)

        styleNavigation(views, currentPage)

        views.navHome.setOnClickListener {
            if (currentPage != "Home") {
                openFirebasePage(activity, Home::class.java, userUid)
            }
        }

        views.navCategories.setOnClickListener {
            if (currentPage != "Categories") {
                openFirebasePage(activity, Categories::class.java, userUid)
            }
        }

        views.navAddExpense.setOnClickListener {
            if (currentPage != "AddExpense") {
                openFirebasePage(activity, Expenses::class.java, userUid)
            }
        }

        views.navGoals.setOnClickListener {
            if (currentPage != "Goals") {
                openFirebasePage(activity, MonthlyGoals::class.java, userUid)
            }
        }

        views.navSavingsDebt.setOnClickListener {
            if (currentPage != "Savings") {
                openFirebasePage(activity, SavingsDebt::class.java, userUid)
            }
        }

        views.navProfile.setOnClickListener {
            if (currentPage != "Profile") {
                openFirebasePage(activity, Profile::class.java, userUid)
            }
        }
    }

    // Supports any remaining screen that still passes the earlier integer user ID.
    fun setupBottomNavigation(
        activity: Activity,
        userId: Int,
        currentPage: String
    ) {
        val views = findNavigationViews(activity)

        styleNavigation(views, currentPage)

        views.navHome.setOnClickListener {
            if (currentPage != "Home") {
                openLegacyPage(activity, Home::class.java, userId)
            }
        }

        views.navCategories.setOnClickListener {
            if (currentPage != "Categories") {
                openLegacyPage(activity, Categories::class.java, userId)
            }
        }

        views.navAddExpense.setOnClickListener {
            if (currentPage != "AddExpense") {
                openLegacyPage(activity, Expenses::class.java, userId)
            }
        }

        views.navGoals.setOnClickListener {
            if (currentPage != "Goals") {
                openLegacyPage(activity, MonthlyGoals::class.java, userId)
            }
        }

        views.navSavingsDebt.setOnClickListener {
            if (currentPage != "Savings") {
                openLegacyPage(activity, SavingsDebt::class.java, userId)
            }
        }

        views.navProfile.setOnClickListener {
            if (currentPage != "Profile") {
                openLegacyPage(activity, Profile::class.java, userId)
            }
        }
    }

    private fun findNavigationViews(activity: Activity): NavigationViews {
        return NavigationViews(
            navHome = activity.findViewById(R.id.navHome),
            navCategories = activity.findViewById(R.id.navCategories),
            navAddExpense = activity.findViewById(R.id.navAddExpense),
            navGoals = activity.findViewById(R.id.navGoals),
            navSavingsDebt = activity.findViewById(R.id.navSavingsDebt),
            navProfile = activity.findViewById(R.id.navProfile)
        )
    }

    private fun styleNavigation(
        views: NavigationViews,
        currentPage: String
    ) {
        resetNavItem(views.navHome)
        resetNavItem(views.navCategories)
        resetNavItem(views.navGoals)
        resetNavItem(views.navSavingsDebt)
        resetNavItem(views.navProfile)

        when (currentPage) {
            "Home" -> setActiveItem(views.navHome)
            "Categories" -> setActiveItem(views.navCategories)
            "Goals" -> setActiveItem(views.navGoals)
            "Savings" -> setActiveItem(views.navSavingsDebt)
            "Profile" -> setActiveItem(views.navProfile)
        }
    }

    private fun openFirebasePage(
        activity: Activity,
        destination: Class<*>,
        userUid: String
    ) {
        val intent = Intent(activity, destination)
        intent.putExtra("userUid", userUid)
        activity.startActivity(intent)
    }

    private fun openLegacyPage(
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

    private data class NavigationViews(
        val navHome: TextView,
        val navCategories: TextView,
        val navAddExpense: TextView,
        val navGoals: TextView,
        val navSavingsDebt: TextView,
        val navProfile: TextView
    )
}

