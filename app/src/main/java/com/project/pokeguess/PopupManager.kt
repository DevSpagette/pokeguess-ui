package com.project.pokeguess

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class PopupManager(private val context: Context) {

    private var alertDialog: AlertDialog? = null
    private var updateLink: String? = null

    fun showPopup(currentVersion: String, newVersion: String, updateLink: String) {
        this.updateLink = updateLink

        val dialogView = LayoutInflater.from(context).inflate(R.layout.activity_popup, null)
        val currentVersionTextView: TextView = dialogView.findViewById(R.id.currentVersion)
        val btnUpdate: Button = dialogView.findViewById(R.id.btnUpdate)
        val btnCancel: Button = dialogView.findViewById(R.id.btnCancel)

        // Set popup title, message, and current version
        currentVersionTextView.text = "Installed version : $currentVersion\nNew version: $newVersion"

        btnUpdate.setOnClickListener {
            // Handle update button click
            openBrowser(updateLink)
            showToast("Downloading update...")
            alertDialog?.dismiss()
        }

        btnCancel.setOnClickListener {
            // Handle cancel button click
            showToast("Update canceled.")
            alertDialog?.dismiss()
        }

        // Create and show the AlertDialog
        val builder = AlertDialog.Builder(context)
        builder.setView(dialogView)
        alertDialog = builder.create()
        alertDialog?.show()
    }

    private fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}