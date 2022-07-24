package dev.olog.feature.player.main

import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.core.math.MathUtils.clamp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dev.olog.core.gateway.PlayingQueueGateway
import dev.olog.feature.lyrics.offline.api.FeatureLyricsOfflineNavigator
import dev.olog.feature.main.api.FeatureMainPopupNavigator
import dev.olog.feature.media.api.MusicPreferencesGateway
import dev.olog.feature.media.api.MediaProvider
import dev.olog.feature.player.PlayerTutorial
import dev.olog.platform.fragment.BaseFragment
import dev.olog.platform.theme.PlayerAppearance
import dev.olog.platform.theme.hasPlayerAppearance
import dev.olog.feature.player.R
import dev.olog.platform.adapter.drag.DragListenerImpl
import dev.olog.platform.adapter.drag.IDragListener
import dev.olog.platform.navigation.FragmentTagFactory
import dev.olog.scrollhelper.layoutmanagers.OverScrollLinearLayoutManager
import dev.olog.shared.extension.collectOnViewLifecycle
import dev.olog.shared.extension.findInContext
import dev.olog.shared.extension.mapListItem
import dev.olog.shared.isMarshmallow
import dev.olog.ui.adapter.drag.CircularRevealAnimationController
import kotlinx.android.synthetic.main.fragment_player_default.*
import kotlinx.android.synthetic.main.player_toolbar_default.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.abs

@Keep
@AndroidEntryPoint
class PlayerFragment : BaseFragment(), IDragListener by DragListenerImpl() {

    companion object {
        val TAG = FragmentTagFactory.create(PlayerFragment::class)
    }

    private val viewModel by viewModels<PlayerFragmentViewModel>()
    @Inject
    internal lateinit var presenter: PlayerFragmentPresenter
    @Inject
    lateinit var featureMainPopupNavigator: FeatureMainPopupNavigator
    @Inject
    lateinit var featureLyricsOfflineNavigator: FeatureLyricsOfflineNavigator

    @Inject lateinit var musicPrefs: MusicPreferencesGateway

    private lateinit var layoutManager: LinearLayoutManager

    private val mediaProvider: MediaProvider
        get() = requireContext().findInContext()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val hasPlayerAppearance = requireContext().hasPlayerAppearance()

        val adapter = PlayerFragmentAdapter(
            mediaProvider = requireContext().findInContext(),
            viewModel = viewModel,
            presenter = presenter,
            musicPrefs = musicPrefs,
            dragListener = this,
            playerAppearanceAdaptiveBehavior = IPlayerAppearanceAdaptiveBehavior.get(hasPlayerAppearance.playerAppearance()),
            onItemLongClick = { v, mediaId ->
                featureMainPopupNavigator.toItemDialog(v, mediaId)
            },
            onLyricsButtonClick = { featureLyricsOfflineNavigator.toOfflineLyrics(requireActivity()) }
        )

        layoutManager = OverScrollLinearLayoutManager(list)
        list.adapter = adapter
        list.layoutManager = layoutManager
        list.setHasFixedSize(true)

        setupDragListener(
            scope = viewLifecycleOwner.lifecycleScope,
            list = list,
            direction = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
            animation = CircularRevealAnimationController(),
        )

        val statusBarAlpha = if (!isMarshmallow()) 1f else 0f
        statusBar?.alpha = statusBarAlpha

        mediaProvider.observeQueue()
            .mapListItem { it.toDisplayableItem() }
            .map { queue ->
                if (!hasPlayerAppearance.isMini()) {
                    val copy = queue.toMutableList()
                    if (copy.size > PlayingQueueGateway.MINI_QUEUE_SIZE - 1) {
                        copy.add(viewModel.footerLoadMore)
                    }
                    copy.add(0, viewModel.playerControls())
                    copy
                } else {
                    listOf(viewModel.playerControls())
                }
            }
            .flowOn(Dispatchers.Default)
            .collectOnViewLifecycle(this) {
                adapter.submitList(it)
            }
    }

    override fun onResume() {
        super.onResume()
        getSlidingPanel()?.addBottomSheetCallback(slidingPanelListener)
    }

    override fun onPause() {
        super.onPause()
        getSlidingPanel()?.removeBottomSheetCallback(slidingPanelListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        list.adapter = null
    }

    override fun provideLayoutId(): Int {
        val appearance = requireContext().hasPlayerAppearance()
        return when (appearance.playerAppearance()) {
            PlayerAppearance.FULLSCREEN -> R.layout.fragment_player_fullscreen
            PlayerAppearance.CLEAN -> R.layout.fragment_player_clean
            PlayerAppearance.MINI -> R.layout.fragment_player_mini
            PlayerAppearance.SPOTIFY -> R.layout.fragment_player_spotify
            PlayerAppearance.BIG_IMAGE -> R.layout.fragment_player_big_image
            else -> R.layout.fragment_player_default
        }
    }

    private val slidingPanelListener = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (!isMarshmallow() && slideOffset in .9f..1f) {
                val alpha = (1 - slideOffset) * 10
                statusBar?.alpha = clamp(abs(1 - alpha), 0f, 1f)
            }
            val alpha = clamp(slideOffset * 5f, 0f, 1f)
            view?.alpha = alpha
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                if (viewModel.showLyricsTutorialIfNeverShown()){
                    lyrics?.let { PlayerTutorial.lyrics(it) }
                }
            }
        }
    }
}