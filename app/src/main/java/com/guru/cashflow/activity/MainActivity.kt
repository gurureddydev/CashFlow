package com.guru.cashflow.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.guru.cashflow.R
import com.guru.cashflow.databinding.ActivityMainBinding
import com.guru.cashflow.fragments.AccountFragment
import com.guru.cashflow.fragments.TransactionFragment

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transactionFragment = TransactionFragment()
        val accountFragment = AccountFragment()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.ITEM_ID, "1")
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, "cash flow")
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")

        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM, params)

        binding.chipAppBar.setItemSelected(R.id.ic_transaction,true)
        makeCurrentFragment(transactionFragment)
        binding.chipAppBar.setOnItemSelectedListener { //when the bottom nav clicked
            when (it){
                R.id.ic_transaction -> makeCurrentFragment(transactionFragment)
                R.id.ic_account -> makeCurrentFragment(accountFragment)
            }
            val b = true
            b
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) { //method 2
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fl_wrapper, fragment)
            commit()
        }
    }


    fun floating_button(view: View){
        val intent = Intent(this, InsertionActivity::class.java)
        startActivity(intent)
    }
}