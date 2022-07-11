package com.kieronquinn.app.discoverkiller.ui.screens.update

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.databinding.FragmentUpdateBinding
import com.kieronquinn.app.discoverkiller.ui.base.BackAvailable
import com.kieronquinn.app.discoverkiller.ui.base.BoundFragment
import com.kieronquinn.app.discoverkiller.ui.screens.update.UpdateViewModel.State
import com.kieronquinn.app.discoverkiller.utils.extensions.applyBottomMargins
import com.kieronquinn.app.discoverkiller.utils.extensions.applyBottomPadding
import com.kieronquinn.app.discoverkiller.utils.extensions.isDarkMode
import com.kieronquinn.app.discoverkiller.utils.extensions.onClicked
import com.kieronquinn.monetcompat.extensions.views.applyMonet
import com.kieronquinn.monetcompat.extensions.views.overrideRippleColor
import io.noties.markwon.Markwon
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.roundToInt

class UpdateFragment: BoundFragment<FragmentUpdateBinding>(FragmentUpdateBinding::inflate), BackAvailable {

    private val viewModel by viewModel<UpdateViewModel>()
    private val args by navArgs<UpdateFragmentArgs>()
    private val markwon by inject<Markwon>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupState()
        setupStartInstall()
        setupGitHubButton()
        setupFabState()
        setupFabClick()
        setupInsets()
        setupMonet()
        viewModel.setupWithRelease(args.release)
    }

    private fun setupMonet() {
        val accent = monet.getAccentColor(requireContext())
        binding.updatesDownloadCard.backgroundTintList = ColorStateList.valueOf(
            monet.getPrimaryColor(requireContext(), !requireContext().isDarkMode)
        )
        binding.updatesDownloadStartInstall.setTextColor(accent)
        binding.updatesDownloadStartInstall.overrideRippleColor(accent)
        binding.updatesDownloadProgress.applyMonet()
        binding.updatesDownloadProgressIndeterminate.applyMonet()
        binding.updatesDownloadIcon.imageTintList = ColorStateList.valueOf(accent)
        binding.updatesDownloadDownloadBrowser.setTextColor(accent)
        binding.updatesDownloadFab.backgroundTintList =
            ColorStateList.valueOf(monet.getPrimaryColor(requireContext()))
    }

    private fun setupInsets() {
        binding.updatesDownloadInfo.applyBottomPadding()
        binding.updatesDownloadFab.applyBottomMargins(resources.getDimension(R.dimen.padding_16))
    }

    private fun setupStartInstall() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.updatesDownloadStartInstall.onClicked().collect {
            viewModel.startInstall()
        }
    }

    private fun setupGitHubButton() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.updatesDownloadDownloadBrowser.onClicked().collect {
            viewModel.onDownloadBrowserClicked(args.release.gitHubUrl)
        }
    }

    private fun setupState() {
        handleState(viewModel.state.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.state.collect {
                handleState(it)
            }
        }
    }

    private fun handleState(state: State){
        when(state){
            is State.Loading -> setupWithLoading()
            is State.Info -> setupWithInfo(state)
            is State.StartDownload -> setupWithStartDownload()
            is State.Downloading -> setupWithDownloading(state)
            is State.Done, is State.StartInstall -> setupWithDone()
            is State.Failed -> setupWithFailed()
        }
    }

    private fun setupWithLoading() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = true
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadTitle.setText(R.string.updates_download_loading)
    }

    private fun setupWithInfo(info: State.Info){
        val release = info.release
        binding.updatesDownloadInfo.isVisible = true
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = false
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadHeading.text = getString(R.string.updates_download_heading, release.title, release.versionName)
        binding.updatesDownloadSubheading.text = getString(R.string.updates_download_subheading, release.installedVersion)
        binding.updatesDownloadSubheading.isVisible = !release.installedVersion.isNullOrEmpty()
        binding.updatesDownloadBody.text = markwon.toMarkdown(release.body)
        binding.updatesDownloadInfo.applyBottomPadding()
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.updatesDownloadDownloadBrowser.onClicked().collect {
                viewModel.onDownloadBrowserClicked(release.gitHubUrl)
            }
        }
    }

    private fun setupWithStartDownload() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = true
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDownloading(state: State.Downloading) {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = true
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = false
        binding.updatesDownloadStartInstall.isVisible = false
        binding.updatesDownloadProgress.progress = (state.progress * 100).roundToInt()
        binding.updatesDownloadTitle.setText(R.string.update_downloader_downloading_title)
    }

    private fun setupWithDone() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = true
        binding.updatesDownloadStartInstall.isVisible = true
        binding.updatesDownloadTitle.setText(R.string.updates_download_done)
        binding.updatesDownloadIcon.setImageResource(R.drawable.ic_update_download_done)
    }

    private fun setupWithFailed() {
        binding.updatesDownloadInfo.isVisible = false
        binding.updatesDownloadProgress.isVisible = false
        binding.updatesDownloadProgressIndeterminate.isVisible = false
        binding.updatesDownloadTitle.isVisible = true
        binding.updatesDownloadIcon.isVisible = true
        binding.updatesDownloadStartInstall.isVisible = true
        binding.updatesDownloadTitle.setText(R.string.updates_download_failed)
        binding.updatesDownloadIcon.setImageResource(R.drawable.ic_error_circle)
    }

    private fun setupFabState() {
        handleFabState(viewModel.showFab.value)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.showFab.collect {
                handleFabState(it)
            }
        }
    }

    private fun handleFabState(showFab: Boolean){
        binding.updatesDownloadFab.isVisible = showFab
    }

    private fun setupFabClick() = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
        binding.updatesDownloadFab.onClicked().collect {
            viewModel.startDownload()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

}