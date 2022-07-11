package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.url

import com.kieronquinn.app.discoverkiller.components.navigation.RssNavigation
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheetViewModelImpl

class RssUrlBottomSheetViewModel(
    navigation: RssNavigation,
    settings: SettingsRepository
): BaseRssConfigureBottomSheetViewModelImpl(navigation) {

    override val setting = settings.rssUrl

}