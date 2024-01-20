package com.guru.cashflow.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.guru.cashflow.model.IntroSlide
import com.guru.cashflow.adapter.IntroSliderAdapter
import com.guru.cashflow.R
import com.guru.cashflow.databinding.FragmentOnboardingBinding
import com.google.firebase.auth.FirebaseAuth
import com.guru.cashflow.activity.Login
import com.guru.cashflow.util.Constants
import kotlinx.coroutines.launch

class OnboardingFragment : Fragment() {
    private var binding: FragmentOnboardingBinding? = null
    private lateinit var prefs: DataStore<Preferences>

    private val introSliderAdapter = IntroSliderAdapter(
        listOf(
            IntroSlide(
                "Cash Flow Tracker",
                "Track your income and expenses with our Cash Flow Financial Tracker",
                "fin.json"
            ),
            IntroSlide(
                "Expense Monitoring",
                "Manage your spending and financial health with our Expense Monitoring tools",
                "money.json"
            ),
            IntroSlide(
                "Financial Tips",
                "Get tips on managing your cash flow and improving your financial well-being",
                "fin3.json"
            )
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentOnboardingBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.viewPager?.adapter = introSliderAdapter
        binding?.indicator?.setViewPager(binding?.viewPager)

        binding?.viewPager?.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == introSliderAdapter.itemCount - 1) {
                        val animation = AnimationUtils.loadAnimation(
                            requireActivity(),
                            R.anim.app_name_animation
                        )
                        binding?.buttonNext?.animation = animation
                        binding?.buttonNext?.text = getString(R.string.finish)
                        binding?.buttonNext?.setOnClickListener {
                            lifecycleScope.launch {
                                saveOnboarding()
                            }

                            // Check if the user is logged in
                            val auth = FirebaseAuth.getInstance()
                            if (auth.currentUser != null) {
                                requireView().findNavController()
                                    .navigate(OnboardingFragmentDirections.actionOnboardingFragmentToWelcomeFragment())
                            } else {
                                // User is not logged in, start the Login activity
                                val intent = Intent(requireContext(), Login::class.java)
                                startActivity(intent)
                                requireActivity().finish() // Optionally, finish the Onboarding activity
                            }
                        }
                    } else {
                        binding?.buttonNext?.text = getString(R.string.next)
                        binding?.buttonNext?.setOnClickListener {
                            binding?.viewPager?.currentItem?.let {
                                binding?.viewPager?.setCurrentItem(it + 1, false)
                            }
                        }
                    }
                }
            }
        )
        prefs = requireActivity().applicationContext.createDataStore(name = Constants.PREFERENCES)
    }

    suspend fun saveOnboarding() {
        prefs.edit {
            val oneTime = true
            it[preferencesKey<Boolean>(getString(R.string.onboard))] = oneTime
        }
    }
}
