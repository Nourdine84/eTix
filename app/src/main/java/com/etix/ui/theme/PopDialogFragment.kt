package com.etix.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PopDialogFragment : DialogFragment() {

    interface Listener {
        fun onPositive() {}
        fun onNegative() {}
        fun onNeutral() {}
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = requireArguments().getString(ARG_TITLE)
        val message = requireArguments().getString(ARG_MESSAGE)
        val positive = requireArguments().getString(ARG_POSITIVE)
        val negative = requireArguments().getString(ARG_NEGATIVE)
        val neutral  = requireArguments().getString(ARG_NEUTRAL)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)

        if (!positive.isNullOrBlank()) {
            builder.setPositiveButton(positive) { _, _ ->
                (parentFragment as? Listener)?.onPositive()
                    ?: (activity as? Listener)?.onPositive()
            }
        }
        if (!negative.isNullOrBlank()) {
            builder.setNegativeButton(negative) { _, _ ->
                (parentFragment as? Listener)?.onNegative()
                    ?: (activity as? Listener)?.onNegative()
            }
        }
        if (!neutral.isNullOrBlank()) {
            builder.setNeutralButton(neutral) { _, _ ->
                (parentFragment as? Listener)?.onNeutral()
                    ?: (activity as? Listener)?.onNeutral()
            }
        }
        return builder.create()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_NEGATIVE = "negative"
        private const val ARG_NEUTRAL = "neutral"

        fun newInstance(
            title: String,
            message: String,
            positive: String? = "OK",
            negative: String? = null,
            neutral: String? = null
        ): PopDialogFragment {
            return PopDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_POSITIVE, positive)
                    putString(ARG_NEGATIVE, negative)
                    putString(ARG_NEUTRAL, neutral)
                }
            }
        }
    }
}