package com.kieronquinn.app.discoverkiller.model

sealed class AppPickerItem(open val itemType: AppPickerItemType) {
    data class App(val packageName: String, val label: CharSequence): AppPickerItem(AppPickerItemType.APP)
    object SnackbarPadding: AppPickerItem(AppPickerItemType.SNACKBAR_PADDING)
}

enum class AppPickerItemType {
    APP, SNACKBAR_PADDING
}