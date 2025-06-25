package com.ioannapergamali.mysmartroute.model.menus

/** Δομή για το json των μενού ρόλων. */
data class MenuConfig(
    /** Κλειδί πόρου για τον τίτλο του μενού */
    val titleKey: String,
    val options: List<OptionConfig>
)

data class OptionConfig(
    /** Κλειδί πόρου για τον τίτλο της επιλογής */
    val titleKey: String,
    val route: String
)

/** Δομή για τα μενού που αντιστοιχούν σε έναν ρόλο. */
data class RoleMenuConfig(
    val inheritsFrom: String? = null,
    val menus: List<MenuConfig> = emptyList()
)
