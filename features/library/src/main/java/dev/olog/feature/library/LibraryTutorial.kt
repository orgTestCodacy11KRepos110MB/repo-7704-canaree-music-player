package dev.olog.feature.library

import android.view.View
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import dev.olog.shared.android.extensions.findActivity
import dev.olog.shared.widgets.extension.tint

object LibraryTutorial {

    fun floatingWindow(view: View){
        val context = view.context

        val target = TapTarget.forView(view, context.getString(R.string.tutorial_floating_window))
            .icon(ContextCompat.getDrawable(context, R.drawable.vd_search_text))
            .tint(context)
        TapTargetView.showFor(view.findActivity(), target)
    }

}