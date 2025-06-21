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
