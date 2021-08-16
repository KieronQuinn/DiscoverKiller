package com.kieronquinn.app.discoverkiller.model

sealed class ConfigurationItem(open val itemType: ConfigurationItemType){
    object Picker: ConfigurationItem(ConfigurationItemType.PICKER)
    data class SnapshotSettings(val settingsItems: List<SettingsItem>): ConfigurationItem(ConfigurationItemType.SNAPSHOT_SETTINGS)
    data class AppSettings(val settingsItems: List<SettingsItem>): ConfigurationItem(ConfigurationItemType.APP_SETTINGS)
    object About: ConfigurationItem(ConfigurationItemType.ABOUT)
}

enum class ConfigurationItemType {
    PICKER, SNAPSHOT_SETTINGS, APP_SETTINGS, ABOUT
}