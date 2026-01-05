package com.etix.utils

import android.graphics.Rect
import android.view.MotionEvent
import android.widget.EditText

/**
 * Détecte le clic sur le drawableEnd (icône œil)
 */
fun EditText.setOnDrawableEndClickListener(action: () -> Unit) {
    setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_UP) {
            val drawableEnd = compoundDrawables[2] // right drawable
            if (drawableEnd != null) {
                val bounds: Rect = drawableEnd.bounds
                val x = event.x.toInt()
                val width = width
                if (x >= width - paddingEnd - bounds.width()) {
                    action.invoke()
                    return@setOnTouchListener true
                }
            }
        }
        false
    }
}
