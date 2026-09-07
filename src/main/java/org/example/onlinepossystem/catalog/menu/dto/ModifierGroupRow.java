package org.example.onlinepossystem.catalog.menu.dto;

public record ModifierGroupRow(Integer menuItemId, Integer groupId, String name, Boolean required, Integer minSelect, Integer maxSelect) {
}
