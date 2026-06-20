package org.example.onlinepossystem.menu.repository;

import org.example.onlinepossystem.menu.dto.BurgerComponentRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BurgerComponentReadRepository {
    private static final Logger logger = LoggerFactory.getLogger(BurgerComponentReadRepository.class);
    private static final List<String> REQUIRED_BURGER_TABLES = List.of(
            "burger_component",
            "burger_recipe_assignment",
            "burger_recipe_component",
            "burger_item_default_component",
            "branch_burger_component_price"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private boolean warnedMissingBurgerTables;

    public BurgerComponentReadRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BurgerComponentRow> findComponentsForMenuItems(Integer branchId, List<Integer> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) {
            return List.of();
        }
        if (!normalizedBurgerTablesExist()) {
            warnMissingBurgerTables();
            return List.of();
        }

        String sql = """
                SELECT *
                FROM (
                    SELECT
                        assignment.burger_id,
                        component.component_id,
                        component.name,
                        component.component_type,
                        TRUE AS default_selected,
                        COALESCE(price.price, 0) AS price,
                        recipe_component.is_removable AS removable,
                        recipe_component.sort_order,
                        assignment.protein_quantity_required
                    FROM burger_recipe_assignment assignment
                    JOIN burger_recipe_component recipe_component
                        ON recipe_component.recipe_id = assignment.recipe_id
                    JOIN burger_component component
                        ON component.component_id = recipe_component.component_id
                    LEFT JOIN branch_burger_component_price price
                        ON price.component_id = component.component_id
                       AND price.branch_id = :branchId
                    WHERE assignment.burger_id IN (:menuItemIds)
                      AND component.active = TRUE

                    UNION ALL

                    SELECT
                        assignment.burger_id,
                        component.component_id,
                        component.name,
                        component.component_type,
                        TRUE AS default_selected,
                        COALESCE(price.price, 0) AS price,
                        item_component.is_removable AS removable,
                        item_component.sort_order,
                        assignment.protein_quantity_required
                    FROM burger_recipe_assignment assignment
                    JOIN burger_item_default_component item_component
                        ON item_component.burger_id = assignment.burger_id
                    JOIN burger_component component
                        ON component.component_id = item_component.component_id
                    LEFT JOIN branch_burger_component_price price
                        ON price.component_id = component.component_id
                       AND price.branch_id = :branchId
                    WHERE assignment.burger_id IN (:menuItemIds)
                      AND component.active = TRUE

                    UNION ALL

                    SELECT
                        assignment.burger_id,
                        component.component_id,
                        component.name,
                        component.component_type,
                        FALSE AS default_selected,
                        COALESCE(price.price, 0) AS price,
                        TRUE AS removable,
                        2000 + component.component_id AS sort_order,
                        assignment.protein_quantity_required
                    FROM burger_recipe_assignment assignment
                    JOIN burger_component component
                        ON component.component_type IN ('protein', 'extra_topping')
                       AND component.active = TRUE
                    LEFT JOIN branch_burger_component_price price
                        ON price.component_id = component.component_id
                       AND price.branch_id = :branchId
                    WHERE assignment.burger_id IN (:menuItemIds)
                ) burger_components
                ORDER BY
                    burger_id,
                    CASE component_type
                        WHEN 'protein' THEN 0
                        WHEN 'default_topping' THEN 1
                        ELSE 2
                    END,
                    default_selected DESC,
                    sort_order,
                    name
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("menuItemIds", menuItemIds);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new BurgerComponentRow(
                rs.getInt("burger_id"),
                rs.getInt("component_id"),
                rs.getString("name"),
                rs.getString("component_type"),
                rs.getBoolean("default_selected"),
                rs.getDouble("price"),
                rs.getBoolean("removable"),
                rs.getInt("sort_order"),
                rs.getInt("protein_quantity_required")
        ));
    }

    public Optional<BurgerConfig> findBurgerConfig(Integer burgerId) {
        if (burgerId == null) {
            return Optional.empty();
        }
        if (!normalizedBurgerTablesExist()) {
            warnMissingBurgerTables();
            return Optional.empty();
        }

        String sql = """
                SELECT burger_id, protein_quantity_required
                FROM burger_recipe_assignment
                WHERE burger_id = :burgerId
                """;

        List<BurgerConfig> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("burgerId", burgerId),
                (rs, rowNum) -> new BurgerConfig(
                        rs.getInt("burger_id"),
                        rs.getInt("protein_quantity_required")
                )
        );
        return rows.stream().findFirst();
    }

    public record BurgerConfig(Integer burgerId, Integer proteinQuantityRequired) {
    }

    private boolean normalizedBurgerTablesExist() {
        String sql = """
                SELECT COUNT(DISTINCT LOWER(table_name))
                FROM information_schema.tables
                WHERE LOWER(table_name) IN (:tableNames)
                """;

        Integer tableCount = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("tableNames", REQUIRED_BURGER_TABLES),
                Integer.class
        );
        return tableCount != null && tableCount == REQUIRED_BURGER_TABLES.size();
    }

    private void warnMissingBurgerTables() {
        if (warnedMissingBurgerTables) {
            return;
        }
        warnedMissingBurgerTables = true;
        logger.warn(
                "Normalized burger tables are missing. Run src/main/resources/migration.sql to enable burger proteins, defaults, and extras."
        );
    }
}
