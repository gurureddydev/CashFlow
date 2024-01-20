package com.guru.cashflow.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.guru.cashflow.R
import com.guru.cashflow.activity.Login
import com.guru.cashflow.databinding.FragmentAccountBinding
import com.guru.cashflow.model.TransactionModel
import com.guru.cashflow.util.Constants.EXPENSE
import com.guru.cashflow.util.Constants.INCOME
import com.guru.cashflow.util.Constants.MILLIS_IN_A_DAY
import com.guru.cashflow.util.showToast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class AccountFragment : Fragment() {
    private companion object {
        const val DELAY: Long = 200
    }

    private lateinit var binding: FragmentAccountBinding

    // Initialize Firebase Auth and database
    private var auth: FirebaseAuth = Firebase.auth
    private var user = Firebase.auth.currentUser
    private val uid = user?.uid ?: ""
    private var dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference(uid)

    // Initialize val for storing amount value from db
    var amountExpense: Double = 0.0
    var amountIncome: Double = 0.0
    var allTimeExpense: Double = 0.0
    var allTimeIncome: Double = 0.0

    private var dateStart: Long = 0
    private var dateEnd: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logout()  //logout button clicked
        accountDetails() //Output Account details from firebase
        setInitDate() //initialized or set the current date data to this month date range, it is default date range when the fragment is open
        chartMenu()
        setupCharts()
        setupDateRangePicker() //date range picker

        swipeRefresh()
    }

    private fun swipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { //call getTransaction() back to refresh the recyclerview
            accountDetails()
            showAllTimeRecap()
            setupPieChart()
            setupBarChart()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupCharts() {
        Handler(Looper.getMainLooper()).postDelayed({
            showAllTimeRecap()
            setupPieChart()
            setupBarChart()
        }, DELAY)
    }

    private fun chartMenu() {
        binding.RadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbBarChart -> binding.apply {
                    setVisibility(listOf(barChart, pieChart), barChart)
                }

                R.id.rbPieChart -> binding.apply {
                    setVisibility(listOf(barChart, pieChart), pieChart)
                }
            }
        }
    }

    private fun setVisibility(views: List<View>, visibleView: View) {
        views.forEach { view ->
            view.visibility = if (view == visibleView) View.VISIBLE else View.GONE
        }
    }

    private fun setInitDate() {
        Calendar.getInstance(TimeZone.getDefault()).apply {
            time = Date()
            set(Calendar.DAY_OF_MONTH, getActualMinimum(Calendar.DAY_OF_MONTH))
            dateStart = timeInMillis

            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            dateEnd = timeInMillis
        }

        fetchAmount(dateStart, dateEnd)
        binding.buttonDate.text = getString(R.string.this_month)
    }

    private fun setupDateRangePicker() {
        // Set a click listener on the date range button
        binding.buttonDate.setOnClickListener {
            // Build a MaterialDatePicker for date range selection
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.select_date))
                .setSelection(Pair(dateStart, dateEnd))
                .build()

            // Show the date picker using the Fragment Manager
            datePicker.show(
                requireActivity().supportFragmentManager,
                getString(R.string.datepicker)
            )

            // Set up an event for when the "OK" button is clicked
            datePicker.addOnPositiveButtonClickListener { selectedDate ->
                // Extract the start and end date from the selected date range
                val pickedDateStart = selectedDate.first
                val pickedDateEnd = selectedDate.second

                // Set the button text to the selected date range
                binding.buttonDate.text = convertDate(pickedDateStart, pickedDateEnd)

                // Fetch and show the report based on the selected date range
                fetchAmount(pickedDateStart, pickedDateEnd)

                // Delay the setup of charts to ensure the updated data is available
                Handler(Looper.getMainLooper()).postDelayed({
                    setupPieChart()
                    setupBarChart()
                }, DELAY)
            }
        }
    }

    private fun accountDetails() {
        user?.reload()
        user?.let { currentUser ->
            val userName: String? = currentUser.displayName
            val email: String? = currentUser.email

            // Apply function for setting up properties
            binding.verified.visibility =
                if (currentUser.isEmailVerified) View.VISIBLE else View.GONE
            binding.notVerified.visibility =
                if (!currentUser.isEmailVerified) View.VISIBLE else View.GONE

            binding.verified.setOnClickListener {
                this@AccountFragment.activity?.showToast(getString(R.string.your_account_is_verified))
            }

            binding.notVerified.setOnClickListener {
                currentUser.sendEmailVerification().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        this@AccountFragment.activity?.showToast(getString(R.string.check_your_email_including_spam))
                    } else {
                        this@AccountFragment.activity?.showToast("${task.exception?.message}")
                    }
                }
            }

            // Simplify split and null-checking
            val name: String? = email?.split("@")?.get(0)
            binding.apply {
                tvName.text = name
                picture.text = name?.get(0)?.uppercase()
                tvEmail.text = email
            }

            // Simplify null-checking for userName
            userName?.let {
                binding.tvName.text = it
                binding.picture.text = it[0].toString().uppercase()
            }
        }
    }

    private fun logout() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Intent(this.activity, Login::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity?.startActivity(it)
            }
        }
    }

    private fun showAllTimeRecap() {
        binding.apply {
            netAmount.text = "${allTimeIncome - allTimeExpense}"
            expenseAmount.text = "$allTimeExpense"
            incomeAmount.text = "$allTimeIncome"
        }
    }

    private fun setupBarChart() {
        //Bar Chart Library Dependency : https://github.com/PhilJay/MPAndroidChart
        binding.netAmountRange.text =
            "${amountIncome - amountExpense}" //show the net amount on textview

        val barChart: BarChart = requireView().findViewById(R.id.barChart)

        val barEntries = arrayListOf<BarEntry>()
        barEntries.add(BarEntry(1f, amountExpense.toFloat()))
        barEntries.add(BarEntry(2f, amountIncome.toFloat()))

        val xAxisValue = arrayListOf("", EXPENSE, INCOME)

        //custom bar chart :
        barChart.animateXY(500, 500) //create bar chart animation
        barChart.description.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawBarShadow(false)
        barChart.setPinchZoom(false)
        barChart.isDoubleTapToZoomEnabled = false
        barChart.setScaleEnabled(false)
        barChart.setFitBars(true)
        barChart.legend.isEnabled = false

        barChart.axisRight.isEnabled = false
        barChart.axisLeft.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f


        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter =
            com.github.mikephil.charting.formatter.IndexAxisValueFormatter(xAxisValue)

        val barDataSet = BarDataSet(barEntries, "")
        barDataSet.setColors(
            ContextCompat.getColor(requireContext(), R.color.orangeSecondary),
            ContextCompat.getColor(requireContext(), R.color.purple)
        )

        barDataSet.valueTextColor = Color.BLACK
        barDataSet.valueTextSize = 15f
        barDataSet.isHighlightEnabled = false

        //setup bar data
        val barData = BarData(barDataSet)
        barData.barWidth = 0.5f

        barChart.data = barData
    }

    private fun setupPieChart() {
        //Pie Chart Library Dependency : https://github.com/PhilJay/MPAndroidChart


        val pieEntries = arrayListOf<PieEntry>()
        pieEntries.add(PieEntry(amountExpense.toFloat(), EXPENSE))
        pieEntries.add(PieEntry(amountIncome.toFloat(), INCOME))

        //pie chart animation
        binding.pieChart.animateXY(500, 500)

        //setup pie chart colors
        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.setColors(
            ContextCompat.getColor(requireContext(), R.color.orangeSecondary),
            ContextCompat.getColor(requireContext(), R.color.purple)
        )

        binding.apply {
            pieChart.description.isEnabled = false
            pieChart.legend.isEnabled = false
            pieChart.setUsePercentValues(true)
            pieChart.setEntryLabelTextSize(12f)
            pieChart.setEntryLabelColor(Color.WHITE)
            pieChart.holeRadius = 46f
        }


        // Setup pie data
        val pieData = PieData(pieDataSet)
        pieData.setDrawValues(true) //enable the value on each pieEntry
        pieData.setValueFormatter(PercentFormatter(binding.pieChart))
        pieData.setValueTextSize(12f)
        pieData.setValueTextColor(Color.WHITE)
        binding.apply {
            pieChart.data = pieData
            pieChart.invalidate()
        }
    }

    private fun fetchAmount(dateStart: Long, dateEnd: Long) {
        var amountExpenseTemp = 0.0
        var amountIncomeTemp = 0.0

        val transactionList: ArrayList<TransactionModel> = arrayListOf()

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                if (snapshot.exists()) {
                    for (transactionSnap in snapshot.children) {
                        val transactionData =
                            transactionSnap.getValue(TransactionModel::class.java) //reference data class
                        transactionData?.let {
                            transactionList.add(it)
                        }
                    }
                }
                // separate expense amount and income amount, and show it based on the range date:
                for (transaction in transactionList) {
                    if (transaction.type == 1 && transaction.date?.let { it > dateStart - MILLIS_IN_A_DAY } == true &&
                        transaction.date?.let { it <= dateEnd } == true
                    ) {
                        amountExpenseTemp += transaction.amount ?: 0.0
                    } else if (transaction.type == 2 &&
                        transaction.date?.let { it > dateStart - MILLIS_IN_A_DAY } == true &&
                        transaction.date?.let { it <= dateEnd } == true
                    ) {
                        amountIncomeTemp += transaction.amount ?: 0.0
                    }
                }
                amountExpense = amountExpenseTemp
                amountIncome = amountIncomeTemp

                // take all amount expense and income:
                for (transaction in transactionList) {
                    if (transaction.type == 1) {
                        amountExpenseTemp += transaction.amount ?: 0.0
                    } else if (transaction.type == 2) {
                        amountIncomeTemp += transaction.amount ?: 0.0
                    }
                }
                allTimeExpense = amountExpenseTemp
                allTimeIncome = amountIncomeTemp
            }

            override fun onCancelled(error: DatabaseError) {
                println(error)
            }
        })
    }

    private fun convertDate(dateStart: Long, dateEnd: Long): String {
        val simpleDateFormat = SimpleDateFormat(getString(R.string.dd_mm_yyyy), Locale.ENGLISH)
        val date1 = Date(dateStart)
        val date2 = Date(dateEnd)
        val result1 = simpleDateFormat.format(date1)
        val result2 = simpleDateFormat.format(date2)
        return "$result1 - $result2"
    }

    override fun onResume() {
        super.onResume()
        showAllTimeRecap() //show all time recap text
        setupPieChart()
        setupBarChart()
    }
}