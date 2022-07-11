package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.logo

import androidx.lifecycle.viewModelScope
import com.kieronquinn.app.discoverkiller.components.navigation.RssNavigation
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository
import com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure.bottomsheet.BaseRssConfigureBottomSheetViewModelImpl
import kotlinx.coroutines.launch

class RssLogoBottomSheetViewModel(
    private val navigation: RssNavigation,
    settings: SettingsRepository
): BaseRssConfigureBottomSheetViewModelImpl(navigation) {

    override val setting = settings.rssLogoUrl

    override fun onNeutralClicked() {
        viewModelScope.launch {
            setting.set("")
            navigation.navigateBack()
        }
    }

}