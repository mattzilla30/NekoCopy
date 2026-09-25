package eu.kanade.tachiyomi.widget

import android.view.animation.Animation

/**
 * Add a listener to this Animation using the provided actions.
 *
 * @return the [Animation.AnimationListener] added to the Animator
 */
inline fun Animation.setListener(
    crossinline onEnd: (animation: Animation) -> Unit = {},
    crossinline onStart: (animation: Animation) -> Unit = {},
    crossinline onRepeat: (animation: Animation) -> Unit = {},
): Animation.AnimationListener {
    val listener =
        object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation) = onRepeat(animation)

            override fun onAnimationEnd(animation: Animation) = onEnd(animation)

            override fun onAnimationStart(animation: Animation) = onStart(animation)
        }
    setAnimationListener(listener)
    return listener
}
