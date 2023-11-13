package com.project.pokeguess

import android.content.Context
import android.util.AttributeSet
import android.media.MediaPlayer

class CustomButton : androidx.appcompat.widget.AppCompatButton {

    private var clickSound: MediaPlayer? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {
        // Initialize the MediaPlayer with your click sound
        clickSound = MediaPlayer.create(context, R.raw.ui_click)
    }

    override fun performClick(): Boolean {
        // Play the click sound
        if (!GLOBAL.MUTESOUNDS)
            clickSound?.start()

        // Call the super method to perform the default click action
        return super.performClick()
    }

    override fun onDetachedFromWindow() {
        // Release the MediaPlayer when the button is detached from the window
        clickSound?.release()
        super.onDetachedFromWindow()
    }
}
