package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.logo

import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RssLogoBottomSheetFragment: BaseRssConfigureBottomSheet() {

    override val viewModel by viewModel<RssLogoBottomSheetViewModel>()

    override val title = R.string.rss_configure_logo_title
    override val hint = R.string.rss_configure_enter_url
    override val neutralText = R.string.rss_configure_clear

}