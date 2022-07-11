package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentRssConfigureBinding
import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItem
import com.kieronquinn.app.discoverkiller.model.settings.GenericSettingsItem
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.RssConfigureViewModel.State
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.onApplyInsets
import org.koin.androidx.viewmodel.ext.android.viewModel

class RssConfigureFragment: BoundFragment<FragmentRssConfigureBinding>(FragmentRssConfigureBinding::inflate) {

    private val viewModel by viewModel<RssConfigureViewModel>()

    private val adapter by lazy {
        RssConfigureAdapter(binding.rssConfigureRecyclerView, emptyList())
    }

    private val appBarTypeface by lazy {
        ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(monet.getBackgroundColor(requireContext()))
        setupAppBar()
        setupRecyclerView()
        setupState()
        setupStatusNav()
    }

    private fun setupRecyclerView() = with(binding.rssConfigureRecyclerView) {
        adapter = this@RssConfigureFragment.adapter
        layoutManager = LinearLayoutManager(context)
        val padding = resources.getDimension(R.dimen.padding_16).toInt()
        onApplyInsets { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = bottomInset + padding)
        }
    }

    private fun setupAppBar(){
        binding.appBar.onApplyInsets { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = topInset)
        }
        binding.collapsingToolbar.setContentScrimColor(
            monet.getBackgroundColorSecondary(requireContext())
                ?: monet.getBackgroundColor(requireContext())
        )
        binding.collapsingToolbar.setCollapsedTitleTypeface(appBarTypeface)
        binding.collapsingToolbar.setExpandedTitleTypeface(appBarTypeface)
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State) {
        when(state){
            is State.Loading -> {
                binding.rssConfigureLoading.isVisible = true
                binding.rssConfigureRecyclerView.isVisible = false
            }
            is State.Loaded -> {
                binding.rssConfigureLoading.isVisible = false
                binding.rssConfigureRecyclerView.isVisible = true
                adapter.update(loadItems(state), binding.rssConfigureRecyclerView)
            }
        }
    }

    private fun loadItems(state: State.Loaded): List<BaseSettingsItem> {
        val feedUrl = state.rssUrl.ifEmpty {
            getString(R.string.rss_configure_url_content_unset)
        }
        val feedTitle = when {
            state.rssTitle.isNotEmpty() -> state.rssTitle
            state.rssLogoUrl.isNotEmpty() -> {
                getString(R.string.rss_configure_title_content_disabled)
            }
            else -> {
                getString(R.string.rss_configure_title_content_unset)
            }
        }
        val feedLogo = state.rssLogoUrl.ifEmpty {
            getString(R.string.rss_configure_logo_content_unset)
        }
        return listOf(
            GenericSettingsItem.Setting(
                getString(R.string.rss_configure_url_title),
                feedUrl,
                R.drawable.ic_rss_configure_url,
                viewModel::onRssUrlClicked
            ),
            GenericSettingsItem.Setting(
                getString(R.string.rss_configure_title_title),
                feedTitle,
                R.drawable.ic_rss_configure_title,
                viewModel::onRssTitleClicked
            ),
            GenericSettingsItem.Setting(
                getString(R.string.rss_configure_logo_title),
                feedLogo,
                R.drawable.ic_rss_configure_logo,
                viewModel::onRssLogoClicked
            )
        )
    }

    private fun setupStatusNav(){
        val window = requireActivity().window
        window.decorView.post {
            WindowInsetsControllerCompat(window, window.decorView).run {
                isAppearanceLightNavigationBars = !requireContext().isDarkMode
                isAppearanceLightStatusBars = !requireContext().isDarkMode
            }
        }
    }

}