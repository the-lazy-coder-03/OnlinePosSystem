package org.example.onlinepossystem.menu.dto;

public record ModifierGroupRow(Integer menuItemId, Integer groupId, String name, Boolean required, Integer minSelect, Integer maxSelect) {
}
