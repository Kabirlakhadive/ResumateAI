package com.example.buildairesume

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class VerticalTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(90f, width / 2f, height / 2f) // Rotate text 90 degrees
        canvas.translate(0f, -width.toFloat()) // Adjust position
        super.onDraw(canvas)
        canvas.restore()
    }
}
