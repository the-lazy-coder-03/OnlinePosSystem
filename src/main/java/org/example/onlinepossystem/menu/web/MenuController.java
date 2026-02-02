package org.example.onlinepossystem.menu.web;

import org.example.onlinepossystem.menu.dto.MenuItemDetail;
import org.example.onlinepossystem.menu.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches/{branchId}/menu-items")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public List<MenuItemDetail> listMenuItems(@PathVariable Integer branchId) {
        return menuService.getMenuForBranch(branchId);
    }
}
