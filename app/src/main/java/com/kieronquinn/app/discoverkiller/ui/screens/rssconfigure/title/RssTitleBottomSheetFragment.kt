package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.title

import com.kieronquinn.app.discoverkiller.R
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class RssTitleBottomSheetFragment: BaseRssConfigureBottomSheet() {

    override val viewModel by viewModel<RssTitleBottomSheetViewModel>()

    override val title = R.string.rss_configure_title_title
    override val hint: Int? = null

}