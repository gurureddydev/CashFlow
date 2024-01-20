package com.guru.cashflow.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.guru.cashflow.databinding.ActivitySignUpBinding
import com.guru.cashflow.util.showToast


class SignUp : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupBtn.setOnClickListener { //when the sign up button get click
            val email = binding.email.text.toString()
            val pass = binding.password.text.toString()
            val confirmPass = binding.passwordRetype.text.toString()

            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()){ //checking is the email match or not
                if(pass.isNotEmpty() && confirmPass.isNotEmpty()){
                    if (pass == confirmPass){
                        binding.progressBar.visibility = View.VISIBLE //show loading progress bar
                        firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                            if (it.isSuccessful){
                                val intent = Intent(this, MainActivity::class.java)
                                showToast("Sign Up Successful")
                                binding.progressBar.visibility = View.GONE
                                startActivity(intent)
                            }else{
                                binding.progressBar.visibility = View.GONE
                                showToast(it.exception.toString())
                            }
                        }
                    }else{
                        showToast("Password id not Matching")
                    }
                }else{
                    showToast("Empty Fields Are no Allowed")
                }
            }else{
                showToast("Invalid or Empty Email")
            }
        }

        binding.haveAccount.setOnClickListener {
            finish()
        }

    }
}