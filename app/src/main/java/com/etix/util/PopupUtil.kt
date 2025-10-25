package com.etix.util

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.etix.R

object PopupUtil {

    fun showSuccess(context: Context, title: String, message: String) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_success, null)
        view.findViewById<TextView>(R.id.textTitle).text = title
        view.findViewById<TextView>(R.id.textMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showError(context: Context, title: String, message: String) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_error, null)
        view.findViewById<TextView>(R.id.textTitle).text = title
        view.findViewById<TextView>(R.id.textMessage).text = message

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}