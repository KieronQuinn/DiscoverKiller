package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.url

import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RssUrlBottomSheetFragment: BaseRssConfigureBottomSheet() {

    override val viewModel by viewModel<RssUrlBottomSheetViewModel>()

    override val title = R.string.rss_configure_url_title
    override val hint = R.string.rss_configure_enter_url

}