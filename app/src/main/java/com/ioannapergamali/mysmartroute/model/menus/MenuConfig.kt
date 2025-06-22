package com.ioannapergamali.mysmartroute.model.menus

/** Δομή για το json των μενού ρόλων. */
data class MenuConfig(
    val title: String,
    val options: List<OptionConfig>
)

data class OptionConfig(
    val title: String,
    val route: String
)

/** Δομή για τα μενού που αντιστοιχούν σε έναν ρόλο. */
data class RoleMenuConfig(
    val inheritsFrom: String? = null,
    val menus: List<MenuConfig> = emptyList()
)
