package com.kieronquinn.app.discoverkiller.ui.screens.rssconfigure

import com.kieronquinn.app.discoverkiller.model.settings.BaseSettingsItem
import com.kieronquinn.app.discoverkiller.ui.base.settings.BaseSettingsAdapter
import com.kieronquinn.app.discoverkiller.ui.views.LifecycleAwareRecyclerView

class RssConfigureAdapter(
    recyclerView: LifecycleAwareRecyclerView,
    items: List<BaseSettingsItem>
): BaseSettingsAdapter(recyclerView, items)