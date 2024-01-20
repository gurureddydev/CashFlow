package com.guru.cashflow.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.guru.cashflow.R
import com.guru.cashflow.util.showToast

class ForgotPassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val submitButton: Button = findViewById(R.id.forgotPassBtn)
        val etEmail: EditText = findViewById(R.id.emailForgotPass)

        submitButton.setOnClickListener {
            val email: String = etEmail.text.toString().trim { it <= ' ' }
            if (email.isEmpty()){
                showToast(getString(R.string.please_enter_email_address))

            }else{
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful){
                            showToast(getString(R.string.check_your_email_including_spam))
                            finish()
                        }else{
                            showToast(task.exception?.message.toString())
                        }
                    }
            }
        }
    }
}