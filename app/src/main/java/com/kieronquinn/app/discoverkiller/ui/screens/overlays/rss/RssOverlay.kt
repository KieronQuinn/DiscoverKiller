package com.kieronquinn.app.discoverkiller.ui.screens.overlays.rss

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.OverlayRssBinding
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.BaseOverlay
import com.kieronquinn.app.discoverkiller.ui.screens.overlays.rss.RssViewModel.State
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.isScrolled
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import com.kieronquinn.app.discoverkiller.utils.extensions.removeStatusNavBackgroundOnPreDraw
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.squareup.picasso.Picasso
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RssOverlay(context: Context): BaseOverlay<OverlayRssBinding>(context, OverlayRssBinding::inflate), KoinComponent {

    companion object {
        private const val KEY_BACKGROUND_ALPHA_PROGRESS = "background_alpha_progress"
        private val BACKGROUND_COLOR_DISCOVER_LIGHT = Color.parseColor("#F8F9FA")
        private val BACKGROUND_COLOR_DISCOVER_DARK = Color.parseColor("#1F1F20")
    }

    private val settings by inject<SettingsRepository>()

    private val isDarkMode by lazy {
        context.isDarkMode
    }

    private val discoverDefaultBackgroundColor by lazy {
        if(isDarkMode) BACKGROUND_COLOR_DISCOVER_DARK
        else BACKGROUND_COLOR_DISCOVER_LIGHT
    }

    private val toolbarHeight by lazy {
        resources.getDimension(R.dimen.rss_toolbar_height).toInt()
    }

    private val picasso = Picasso.Builder(context)
        .build()

    private var _viewModel: RssViewModel? = null

    private val viewModel: RssViewModel
        get() = _viewModel ?: throw NullPointerException("Accessing ViewModel outside of created time")

    private val scope = MainScope()

    private val adapter by lazy {
        RssAdapter(context) {
            viewModel.onRssItemClicked(context, it)
        }
    }

    private val monet by lazy {
        setupMonet()
        MonetCompat.setup(context).apply {
            defaultBackgroundColor = discoverDefaultBackgroundColor
            updateMonetColors()
        }
    }

    private val textColour by lazy {
        if(context.isDarkMode) Color.WHITE else Color.BLACK
    }

    private fun setupMonet(){
        MonetCompat.wallpaperColorPicker = {
            val selectedColor = settings.monetColor.getSync()
            if(selectedColor != Integer.MAX_VALUE && it?.contains(selectedColor) == true) selectedColor
            else it?.firstOrNull()
        }
    }

    private val backgroundColour by lazy {
        try {
            monet.getBackgroundColor(context)
        }catch (e: Resources.NotFoundException){
            discoverDefaultBackgroundColor
        }
    }

    private val toolbarColour by lazy {
        (monet.getBackgroundColorSecondary(context) ?: backgroundColour).let {
            ColorUtils.setAlphaComponent(it, 216)
        }
    }

    private var backgroundAlphaProgress = 0f

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        _viewModel = RssViewModel()
        backgroundAlphaProgress = bundle?.getFloat(KEY_BACKGROUND_ALPHA_PROGRESS, 0f) ?: 0f
        binding.root.setBackgroundColor(ColorUtils.setAlphaComponent(backgroundColour, 128))
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        setupInsets()
        setupListView()
        setupState()
        setupLoading()
        setupError()
        setupRefresh()
        setupConfigure()
        setupToolbar()
    }

    override fun onDestroy() {
        _viewModel = null
        scope.cancel()
        super.onDestroy()
    }

    private fun setupListView() = with(binding.rssList){
        adapter = this@RssOverlay.adapter
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        scope.launch {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.rssLoading.isVisible = true
                binding.rssList.isVisible = false
                binding.rssError.isVisible = false
                binding.rssTitle.isVisible = false
                binding.rssLogo.isVisible = false
            }
            is State.Loaded -> {
                binding.rssLoading.isVisible = false
                binding.rssList.isVisible = true
                binding.rssError.isVisible = false
                adapter.setItems(state.items)
                val url = settings.rssLogoUrl.getSync()
                val title = settings.rssTitle.getSync()
                when {
                    url.isNotEmpty() -> {
                        binding.rssLogo.isVisible = true
                        binding.rssTitle.isVisible = false
                        picasso.load(url).into(binding.rssLogo)
                    }
                    title.isNotEmpty() -> {
                        binding.rssLogo.isVisible = false
                        binding.rssTitle.isVisible = true
                        binding.rssTitle.text = title
                    }
                    else -> {
                        binding.rssLogo.isVisible = false
                        binding.rssTitle.isVisible = false
                    }
                }
            }
            is State.Error -> {
                binding.rssLoading.isVisible = false
                binding.rssList.isVisible = false
                binding.rssTitle.isVisible = false
                binding.rssLogo.isVisible = false
                binding.rssError.isVisible = true
                binding.rssErrorIcon.setImageResource(state.errorType.icon)
                binding.rssErrorText.setText(state.errorType.title)
                binding.rssError.setOnClickListener {
                    when(state.errorType){
                        State.ErrorType.FAILED, State.ErrorType.EMPTY -> {
                            viewModel.reloadItems(true)
                        }
                        State.ErrorType.UNSET -> {
                            viewModel.onConfigureClicked(this)
                        }
                    }
                }
            }
        }
    }

    private fun setupLoading() {
        binding.rssLoadingProgress.applyMonet()
        binding.rssErrorText.setTextColor(textColour)
    }

    private fun setupError() {
        binding.rssErrorText.setTextColor(textColour)
        binding.rssErrorIcon.imageTintList = ColorStateList.valueOf(textColour)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadItems(false)
        updateProgressViews(backgroundAlphaProgress, true)
        window?.decorView?.post {
            lifecycleScope.launchWhenResumed {
                //Fix case where *two* status bar backgrounds sometimes appear
                window?.decorView?.removeStatusNavBackgroundOnPreDraw()
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putFloat(KEY_BACKGROUND_ALPHA_PROGRESS, backgroundAlphaProgress)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        updateProgressViews(progress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false){
        if(backgroundAlphaProgress == progress && !force) return
        backgroundAlphaProgress = progress
        binding.root.alpha = progress
    }

    override fun onHomeOrBackPressed(i: Int) {
        super.onHomeOrBackPressed(i)
        if(i == 0){
            window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        }
    }

    private fun setupInsets() {
        binding.rssToolbar.onApplyInsets { _, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            optBinding?.rssToolbar?.updateLayoutParams<FrameLayout.LayoutParams> {
                height = toolbarHeight + topInset
            }
            optBinding?.rssToolbarInner?.updatePadding(top = topInset)
        }
        binding.rssList.onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val topPadding = resources.getDimension(R.dimen.rss_toolbar_height).toInt()
            val regularPadding = resources.getDimension(R.dimen.padding_16).toInt()
            view.updatePadding(bottom = bottomInset + regularPadding, top = topPadding + topInset)
        }
    }

    private fun setupRefresh() = with(binding.rssRefresh) {
        imageTintList = ColorStateList.valueOf(textColour)
        setOnClickListener {
            viewModel.reloadItems(true)
        }
    }

    private fun setupConfigure() = with(binding.rssConfigure) {
        imageTintList = ColorStateList.valueOf(textColour)
        setOnClickListener {
            viewModel.onConfigureClicked(context)
        }
    }

    private fun setupToolbar() {
        binding.rssToolbarBackground.setBackgroundColor(toolbarColour)
        binding.rssTitle.setTextColor(textColour)
        scope.launch {
            binding.rssList.isScrolled().collect {
                binding.rssToolbarBackground.isVisible = it
            }
        }
    }

}