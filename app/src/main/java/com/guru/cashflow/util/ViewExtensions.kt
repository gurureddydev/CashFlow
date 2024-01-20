package com.guru.cashflow.util

// ViewExtensions.kt

import android.view.View
import com.google.android.material.snackbar.Snackbar

// Create an extension function for View to show a SnackBar
fun View.showSnackBar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}
