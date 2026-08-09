package com.example.mylittlefinancontrol.Servicios

import android.app.Activity
import android.view.View
import androidx.core.content.ContextCompat
import com.example.mylittlefinancontrol.R
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal

class TutorialHelper(private val activity: Activity) {

    private var currentPrompt: MaterialTapTargetPrompt? = null

    fun showStep(
        targetView: View,
        title: String,
        description: String,
        isRectangle: Boolean = false,
        onNext: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        currentPrompt = MaterialTapTargetPrompt.Builder(activity)
            .setTarget(targetView)
            .setPrimaryText(title)
            .setSecondaryText(description)
            .setBackButtonDismissEnabled(false)
            .setPromptStateChangeListener { prompt, state ->
                if (state == MaterialTapTargetPrompt.STATE_FOCAL_PRESSED || state == MaterialTapTargetPrompt.STATE_NON_FOCAL_PRESSED) {
                    onNext()
                }
            }
            .setBackgroundColour(ContextCompat.getColor(activity, R.color.verde_oscuro_transparente))
            .setPrimaryTextColour(ContextCompat.getColor(activity, android.R.color.white))
            .setSecondaryTextColour(ContextCompat.getColor(activity, android.R.color.white))
            .apply {
                if (isRectangle) {
                    setPromptBackground(RectanglePromptBackground())
                    setPromptFocal(RectanglePromptFocal())
                }
            }
            .show()
    }

    /**
     * Busca una vista recursivamente y espera a que esté disponible.
     */
    fun waitForViewAndShow(
        viewId: Int,
        title: String,
        description: String,
        isRectangle: Boolean = false,
        delay: Long = 500,
        onNext: () -> Unit
    ) {
        val view = activity.findViewById<View>(viewId)
        if (view != null && view.isShown) {
            showStep(view, title, description, isRectangle, onNext)
        } else {
            activity.window.decorView.postDelayed({
                waitForViewAndShow(viewId, title, description, isRectangle, delay, onNext)
            }, delay)
        }
    }

    fun dismissCurrent() {
        currentPrompt?.dismiss()
        currentPrompt = null
    }
}
