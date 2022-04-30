package dev.olog.shared.extension

import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Fragment> T.withArguments(vararg params: Pair<String, Any>): T {
    arguments = bundleOf(*params)
    return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.dip(value: Int): Int = requireContext().dip(value)
@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.dip(value: Float): Int = requireContext().dip(value)
@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.dimen(@DimenRes resId: Int): Int = requireContext().dimen(resId)
@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.toast(@StringRes resId: Int) = requireContext().toast(resId)
@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.toast(message: CharSequence) = requireContext().toast(message)

@Suppress("UNCHECKED_CAST", "ObjectPropertyName")
fun <T : Any> Fragment.argument(key: String): Lazy<T> {
    return object : Lazy<T> {
        private var _value: T? = null

        override val value: T
            get() {
                if (_value == null) {
                    _value = requireArguments().get(key) as T
                }
                return _value!!
            }

        override fun isInitialized(): Boolean = _value != null
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> Flow<T>.collectOnViewLifecycle(
    fragment: Fragment,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collect: FlowCollector<T>,
) {
    return collectOnLifecycle(
        owner = fragment.viewLifecycleOwner,
        minActiveState = minActiveState,
        collect = collect
    )
}

fun Fragment.launchWhenResumed(block: suspend CoroutineScope.() -> Unit): Job {
    return viewLifecycleOwner.lifecycleScope.launchWhenResumed(block)
}