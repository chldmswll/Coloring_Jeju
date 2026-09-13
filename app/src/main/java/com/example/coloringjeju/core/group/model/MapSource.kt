package com.example.coloringjeju.core.group.model

/**
 * Which map/스탬프 list is currently in view — the traveler's own MY 지도, or one joined
 * [TravelGroup]'s shared one. Picked via the `MY 지도 ▾` dropdown on 홈·지도 and 스탬프
 * ([com.example.coloringjeju.ui.components.MapSourceDropdown]), and carried along when a 스탬프
 * mission starts so its completion writes the color back to the right place
 * ([com.example.coloringjeju.presentation.MainTabsScreen]).
 */
sealed interface MapSource {
    data object Personal : MapSource
    data class Group(val group: TravelGroup) : MapSource
}

fun MapSource.label(): String = when (this) {
    MapSource.Personal -> "MY 지도"
    is MapSource.Group -> group.name
}
