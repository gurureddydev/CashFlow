package com.guru.cashflow.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.guru.cashflow.R
import com.guru.cashflow.activity.TransactionDetails
import com.guru.cashflow.adapter.TransactionAdapter
import com.guru.cashflow.model.TransactionModel
import com.guru.cashflow.util.Constants.MILLIS_IN_A_DAY
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class TransactionFragment : Fragment() {
    companion object {
        private const val ALL_TYPE = "All Type"
        private const val EXPENSE = "Expense"
        private const val INCOME = "Income"
        private const val ALL_TIME = "All Time"
        private const val THIS_MONTH = "This Month"
        private const val THIS_WEEK = "This Week"
        private const val TODAY = "Today"
        private const val EXTRA_TRANSACTION_ID = "transactionID"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_CATEGORY = "category"
        private const val EXTRA_AMOUNT = "amount"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_NOTE = "note"
    }

    private lateinit var transactionRecyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var noDataImage: ImageView
    private lateinit var tvNoDataTitle: TextView
    private lateinit var tvVisibilityNoData: TextView
    private lateinit var shimmerLoading: ShimmerFrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var transactionList: ArrayList<TransactionModel>
    private lateinit var typeOption: Spinner
    private lateinit var timeSpanOption: Spinner
    private lateinit var dbRef: DatabaseReference
    private val user = Firebase.auth.currentUser
    private var selectedType: String = ALL_TYPE
    private var selectedTimeSpan: String = ALL_TIME
    private var dateStart: Long = 0
    private var dateEnd: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeItems()
        showUserName()
        visibilityOptions()
        //--Recycler View transaction items--
        transactionRecyclerView = view.findViewById(R.id.rvTransaction)
        transactionRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        transactionRecyclerView.setHasFixedSize(true)

        transactionList = arrayListOf()

        getTransactionData()

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener { //call getTransaction() back to refresh the recyclerview
            getTransactionData()
            swipeRefreshLayout.isRefreshing = false
        }
    }
    private fun initializeItems() {
        tvNoData = requireView().findViewById(R.id.tvNoData)
        noDataImage = requireView().findViewById(R.id.noDataImage)
        tvNoDataTitle = requireView().findViewById(R.id.tvNoDataTitle)
        tvVisibilityNoData = requireView().findViewById(R.id.visibilityNoData)
        shimmerLoading = requireView().findViewById(R.id.shimmerFrameLayout)
    }
    private fun showUserName() {
        user?.reload()
        val tvUserName: TextView = requireView().findViewById(R.id.userNameTV)
        val email = user?.email
        val userName = user?.displayName

        val name = if (userName == null || userName == "") {
            val splitValue = email?.split("@")
            splitValue?.get(0).toString()
        } else {
            userName
        }

        val greetingText = getString(R.string.greeting_message, name)
        tvUserName.text = greetingText
    }
    private fun setupSpinner(
        spinner: Spinner,
        itemList: Array<String>,
        onItemSelectedAction: (String) -> Unit
    ) {
        val adapter = ArrayAdapter(this.requireActivity(), R.layout.selected_spinner, itemList)
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = itemList[position]
                onItemSelectedAction(selectedItem)
                getTransactionData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected if needed
            }
        }
    }
    private fun setupTypeSpinner() {
        val typeList = arrayOf(ALL_TYPE, EXPENSE, INCOME)
        setupSpinner(typeOption, typeList) { selectedType = it }
    }
    private fun setupTimeSpanSpinner() {
        val timeSpanList = arrayOf(ALL_TIME, THIS_MONTH, THIS_WEEK, TODAY)
        setupSpinner(timeSpanOption, timeSpanList) {
            selectedTimeSpan = it
            handleTimeSpanSelection(it)
        }
    }
    private fun handleTimeSpanSelection(selectedTimeSpan: String) {
        when (selectedTimeSpan) {
            THIS_MONTH -> getRangeDate(Calendar.DAY_OF_MONTH)
            THIS_WEEK -> getRangeDate(Calendar.DAY_OF_WEEK)
            TODAY -> {
                dateStart = System.currentTimeMillis()
                dateEnd = System.currentTimeMillis()
            }
        }
    }
    private fun visibilityOptions() {
        typeOption = requireView().findViewById(R.id.typeSpinner)
        timeSpanOption = requireView().findViewById(R.id.timeSpanSpinner)

        setupTypeSpinner()
        setupTimeSpanSpinner()
    }
    private fun getRangeDate(rangeType: Int) {
        val currentDate = Date()
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        cal.time = currentDate

        val startDay = cal.getActualMinimum(rangeType) //get the first date of the month
        cal.set(rangeType, startDay)
        val startDate = cal.time
        dateStart = startDate.time //convert to millis

        val endDay = cal.getActualMaximum(rangeType) //get the last date of the month
        cal.set(rangeType, endDay)
        val endDate = cal.time
        dateEnd = endDate.time //convert to millis
    }
    private fun getSelectedTypeFilter(): (TransactionModel) -> Boolean {
        return when (selectedType) {
            ALL_TYPE -> { _ -> true }
            EXPENSE -> { transaction -> transaction.type == 1 }
            INCOME -> { transaction -> transaction.type == 2 }
            else -> throw IllegalArgumentException("Invalid selectedType: $selectedType")
        }
    }
    private fun filterByTimeSpan(transaction: TransactionModel): Boolean {
        return selectedTimeSpan == ALL_TIME || (
                transaction.date!! > dateStart - MILLIS_IN_A_DAY &&
                        transaction.date!! <= dateEnd
                )
    }
    private fun setupTransactionList(snapshot: DataSnapshot) {
        transactionList.clear()

        val typeFilter = getSelectedTypeFilter()

        for (transactionSnap in snapshot.children) {
            val transactionData = transactionSnap.getValue(TransactionModel::class.java)

            if (transactionData != null && typeFilter(transactionData) && filterByTimeSpan(
                    transactionData
                )
            ) {
                transactionList.add(transactionData)
            }
        }
    }
    private fun showNoDataMessage() {
        noDataImage.visibility = View.VISIBLE
        tvNoDataTitle.visibility = View.VISIBLE
        tvVisibilityNoData.visibility = View.VISIBLE
        val message = getString(R.string.no_data_message, selectedType, selectedTimeSpan)
        tvVisibilityNoData.text = message
    }
    private fun showTransactionData() {
        val mAdapter = TransactionAdapter(transactionList)
        transactionRecyclerView.adapter = mAdapter

        mAdapter.setOnItemClickListener(object : TransactionAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                startTransactionDetailsActivity(position)
            }
        })
        transactionRecyclerView.visibility = View.VISIBLE
    }
    private fun startTransactionDetailsActivity(position: Int) {
        val intent = Intent(this@TransactionFragment.activity, TransactionDetails::class.java)
        putTransactionExtras(intent,position)
        startActivity(intent)
    }
    private fun putTransactionExtras(intent: Intent, position: Int) {
        val transaction = transactionList[position]
        intent.apply {
            putExtra(EXTRA_TRANSACTION_ID, transaction.transactionID)
            putExtra(EXTRA_TYPE, transaction.type)
            putExtra(EXTRA_TITLE, transaction.title)
            putExtra(EXTRA_CATEGORY, transaction.category)
            putExtra(EXTRA_AMOUNT, transaction.amount)
            putExtra(EXTRA_DATE, transaction.date)
            putExtra(EXTRA_NOTE, transaction.note)
        }
    }
    private fun showShimmerEffect() {
        shimmerLoading.startShimmerAnimation()
        shimmerLoading.visibility = View.VISIBLE
        tvVisibilityNoData.visibility = View.GONE
        transactionRecyclerView.visibility = View.GONE
        tvNoData.visibility = View.GONE
        noDataImage.visibility = View.GONE
        tvNoDataTitle.visibility = View.GONE
    }
    private fun hideShimmerEffect() {
        shimmerLoading.stopShimmerAnimation()
        shimmerLoading.visibility = View.GONE
    }
    private fun handleDatabaseEmptyState() {
        shimmerLoading.stopShimmerAnimation()
        shimmerLoading.visibility = View.GONE
        noDataImage.visibility = View.VISIBLE
        tvNoDataTitle.visibility = View.VISIBLE
        tvNoData.visibility = View.VISIBLE
    }

    private fun getTransactionData() {
        showShimmerEffect()

        val uid = user?.uid
        if (uid != null) {
            dbRef = FirebaseDatabase.getInstance().getReference(uid)
        }

        val query: Query = dbRef.orderByChild("invertedDate")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    setupTransactionList(snapshot)

                    if (transactionList.isEmpty()) {
                        showNoDataMessage()
                    } else {
                        showTransactionData()
                    }
                } else {
                    handleDatabaseEmptyState()
                }

                hideShimmerEffect()
            }

            override fun onCancelled(error: DatabaseError) {
                print("Listener was cancelled")
                hideShimmerEffect()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getTransactionData()
    }
}
