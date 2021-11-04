package dev.olog.feature.player.mini

import android.os.Bundle
import android.view.View
import androidx.core.math.MathUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dev.olog.media.model.PlayerState
import dev.olog.media.MediaProvider
import dev.olog.feature.base.BaseFragment
import dev.olog.feature.player.R
import dev.olog.shared.widgets.extension.expand
import dev.olog.shared.widgets.extension.isCollapsed
import dev.olog.shared.widgets.extension.isExpanded
import dev.olog.shared.android.extensions.*
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_mini_player.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.lang.IllegalArgumentException
import javax.inject.Inject

@AndroidEntryPoint
class MiniPlayerFragment : BaseFragment(){

    companion object {
        private const val TAG = "MiniPlayerFragment"
        private const val BUNDLE_IS_VISIBLE = "$TAG.BUNDLE_IS_VISIBLE"
    }

    @Inject lateinit var presenter: MiniPlayerFragmentPresenter

    private val media by lazyFast { requireActivity() as MediaProvider }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            view.toggleVisibility(it.getBoolean(BUNDLE_IS_VISIBLE), true)
        }
        val lastMetadata = presenter.getMetadata()
        title.text = lastMetadata.title
        artist.text = lastMetadata.subtitle

        media.observeMetadata()
                .subscribe(viewLifecycleOwner) {
                    title.text = it.title
                    presenter.startShowingLeftTime(it.isPodcast, it.duration)
                    if (!it.isPodcast){
                        artist.text = it.artist
                    }
                    updateProgressBarMax(it.duration)
                }

        media.observePlaybackState()
                .filter { it.isPlaying|| it.isPaused }
                .distinctUntilChanged()
                .subscribe(viewLifecycleOwner) { progressBar.onStateChanged(it) }

        presenter.observePodcastProgress(progressBar.observeProgress())
            .map { resources.getQuantityString(localization.R.plurals.mini_player_time_left, it.toInt(), it) }
            .filter { timeLeft -> artist.text != timeLeft } // check (new time left != old time left
            .collectOnLifecycle(this) { artist.text = it }

        media.observePlaybackState()
            .filter { it.isPlayOrPause }
            .map { it.state }
            .distinctUntilChanged()
            .subscribe(viewLifecycleOwner) { state ->
                when (state){
                    PlayerState.PLAYING -> playAnimation()
                    PlayerState.PAUSED -> pauseAnimation()
                    else -> throw IllegalArgumentException("invalid state $state")
                }
            }

        media.observePlaybackState()
            .filter { it.isSkipTo }
            .map { it.state == PlayerState.SKIP_TO_NEXT }
            .subscribe(viewLifecycleOwner, this::animateSkipTo)

        presenter.skipToNextVisibility
                .subscribe(viewLifecycleOwner) {
                    next.updateVisibility(it)
                }

        presenter.skipToPreviousVisibility
                .subscribe(viewLifecycleOwner) {
                    previous.updateVisibility(it)
                }
    }

    override fun onResume() {
        super.onResume()
        getSlidingPanel().addBottomSheetCallback(slidingPanelListener)
        view?.setOnClickListener { getSlidingPanel()?.expand() }
        view?.toggleVisibility(!getSlidingPanel().isExpanded(), true)
        next.setOnClickListener { media.skipToNext() }
        playPause.setOnClickListener { media.playPause() }
        previous.setOnClickListener { media.skipToPrevious() }
    }

    override fun onPause() {
        super.onPause()
        getSlidingPanel().removeBottomSheetCallback(slidingPanelListener)
        view?.setOnClickListener(null)
        next.setOnClickListener(null)
        playPause.setOnClickListener(null)
        previous.setOnClickListener(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_IS_VISIBLE, getSlidingPanel().isCollapsed())
    }

    private fun playAnimation() {
        playPause.animationPlay(getSlidingPanel().isCollapsed())
    }

    private fun pauseAnimation() {
        playPause.animationPause(getSlidingPanel().isCollapsed())
    }

    private fun animateSkipTo(toNext: Boolean) {
        if (getSlidingPanel().isExpanded()) return

        if (toNext) {
            next.playAnimation()
        } else {
            previous.playAnimation()
        }
    }

    private fun updateProgressBarMax(max: Long) {
        progressBar.max = max.toInt()
    }

    private val slidingPanelListener = object : BottomSheetBehavior.BottomSheetCallback(){
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            view?.alpha = MathUtils.clamp(1 - slideOffset * 3f, 0f, 1f)
            view?.toggleVisibility(slideOffset <= .8f, true)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            title.isSelected = newState == BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun provideLayoutId(): Int = R.layout.fragment_mini_player
}