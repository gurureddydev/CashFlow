package com.guru.cashflow.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.datastore.DataStore
import androidx.datastore.preferences.Preferences
import androidx.datastore.preferences.createDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.guru.cashflow.activity.MainActivity
import com.guru.cashflow.databinding.FragmentSplashBinding
import com.guru.cashflow.util.Constants.PREFERENCES
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FragmentSplash : Fragment() {
    companion object{
        private const val DELAY:Long =  3000
    }
    private var binding: FragmentSplashBinding? = null
    private lateinit var prefs: DataStore<Preferences>
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        lifecycleScope.launch {
            prefs.data.collectLatest { _ ->
                val auth = FirebaseAuth.getInstance()

                if (auth.currentUser != null) {
                    // User is logged in, navigate to the MainActivity
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish() // Optionally, finish the Splash activity
                } else {
                    // User is not logged in, navigate to the onboarding screen
                    requireView().findNavController()
                        .navigate(FragmentSplashDirections.actionSplashFragmentToOnboardingFragment())
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        prefs = requireActivity().applicationContext.createDataStore(name = PREFERENCES)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, DELAY)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
