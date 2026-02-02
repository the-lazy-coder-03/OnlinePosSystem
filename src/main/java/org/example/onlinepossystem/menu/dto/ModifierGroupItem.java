package org.example.onlinepossystem.menu.dto;

import java.util.List;

public record ModifierGroupItem(Integer groupId, String name, boolean required, Integer minSelect, Integer maxSelect, List<ModifierOptionItem> options) {
    public ModifierGroupItem {
        if (options == null) {
            options = List.of();
        }
    }
}
