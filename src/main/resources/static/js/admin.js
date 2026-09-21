(() => {
    "use strict";

    const root = document.body;
    const adminLevel = Number(root.dataset.adminLevel || 0);
    const fixedBranchId = root.dataset.adminBranchId ? Number(root.dataset.adminBranchId) : null;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
    const csrfParameter = document.querySelector('meta[name="_csrf_parameter"]')?.content || "_csrf";
    const validPanels = new Set(Array.from(document.querySelectorAll(".admin-panel")).map(panel => panel.id));
    let liveClient = null;
    let reconnectTimer = null;
    let reconnectDelay = 1000;

    function selectPanel(panelId, updateHash = true) {
        const target = validPanels.has(panelId) ? panelId : "overview";
        document.querySelectorAll(".admin-panel").forEach(panel => {
            panel.classList.toggle("active", panel.id === target);
        });
        document.querySelectorAll("[data-admin-panel]").forEach(button => {
            const active = button.dataset.adminPanel === target;
            button.classList.toggle("active", active);
            button.setAttribute("aria-selected", String(active));
        });
        const title = document.querySelector(`[data-panel-title="${target}"]`)?.textContent || "Admin Dashboard";
        const pageTitle = document.getElementById("currentPanelTitle");
        if (pageTitle) pageTitle.textContent = title;
        if (updateHash) history.replaceState(null, "", `#${target}`);
        document.querySelectorAll(".offcanvas.show").forEach(element => {
            bootstrap.Offcanvas.getInstance(element)?.hide();
        });
    }

    function setupNavigation() {
        document.querySelectorAll("[data-admin-panel]").forEach(button => {
            button.addEventListener("click", event => {
                event.preventDefault();
                selectPanel(button.dataset.adminPanel);
            });
        });
        window.addEventListener("hashchange", () => selectPanel(location.hash.slice(1), false));
        selectPanel(location.hash.slice(1) || "overview", false);
    }

    function selectedBranchId() {
        if (fixedBranchId) return fixedBranchId;
        const select = document.getElementById("overviewBranch");
        return select?.value ? Number(select.value) : null;
    }

    function reportingQuery() {
        const params = new URLSearchParams();
        const range = document.getElementById("overviewRange")?.value || "today";
        params.set("range", range);
        const branchId = selectedBranchId();
        if (branchId) params.set("branchId", String(branchId));
        if (range === "custom") {
            const start = document.getElementById("overviewStart")?.value;
            const end = document.getElementById("overviewEnd")?.value;
            if (start) params.set("start", start);
            if (end) params.set("end", end);
        }
        return params;
    }

    async function refreshOverview() {
        const error = document.getElementById("overviewError");
        try {
            const response = await fetch(`/api/admin/overview?${reportingQuery()}`);
            if (!response.ok) throw new Error(`Overview request failed with ${response.status}`);
            const data = await response.json();
            setText("metricPending", data.pendingOrders);
            setText("metricPreparing", data.preparingOrders);
            setText("metricCompleted", data.completedOrders);
            setText("metricRejected", data.rejectedOrders);
            setText("metricRevenue", currency(data.revenue));
            setText("metricAverage", currency(data.averageOrderValue));
            setText("overviewPeriod", `${data.startDate} to ${data.endDate}`);
            renderBranchSummary(data.branches || []);
            if (error) error.classList.add("d-none");
        } catch (requestError) {
            console.error(requestError);
            if (error) error.classList.remove("d-none");
        }
    }

    function renderBranchSummary(branches) {
        const body = document.getElementById("branchSummaryBody");
        if (!body) return;
        body.replaceChildren();
        branches.forEach(branch => {
            const row = document.createElement("tr");
            [branch.branchName, branch.pendingOrders, branch.preparingOrders, branch.completedOrders,
                branch.rejectedOrders, currency(branch.revenue)].forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value;
                row.appendChild(cell);
            });
            body.appendChild(row);
        });
    }

    function setupOverviewFilters() {
        const range = document.getElementById("overviewRange");
        const customFields = document.getElementById("customDateFields");
        range?.addEventListener("change", () => {
            customFields?.classList.toggle("d-none", range.value !== "custom");
            if (range.value !== "custom") refreshOverview();
        });
        document.getElementById("overviewApply")?.addEventListener("click", () => {
            refreshOverview();
            refreshOrders();
        });
        document.getElementById("overviewBranch")?.addEventListener("change", () => {
            refreshOverview();
            refreshOrders();
        });
        document.getElementById("adminOrdersRefresh")?.addEventListener("click", () => {
            refreshOrders();
            refreshOverview();
        });
    }

    async function refreshOrders() {
        const container = document.getElementById("adminOrderList");
        if (!container) return;
        try {
            const params = new URLSearchParams();
            const branchId = selectedBranchId();
            if (branchId) params.set("branchId", String(branchId));
            const response = await fetch(`/api/admin/orders?${params}`);
            if (!response.ok) throw new Error(`Orders request failed with ${response.status}`);
            renderOrders(await response.json());
        } catch (error) {
            console.error(error);
            container.innerHTML = '<div class="empty-state">Orders could not be loaded.</div>';
        }
    }

    function renderOrders(orders) {
        const container = document.getElementById("adminOrderList");
        container.replaceChildren();
        if (!orders.length) {
            container.innerHTML = '<div class="empty-state">No orders are available for this branch.</div>';
            return;
        }
        orders.forEach(order => container.appendChild(orderCard(order)));
    }

    function orderCard(order) {
        const article = document.createElement("article");
        article.className = "order-card";
        const status = normalizedStatus(order.status);
        const header = document.createElement("div");
        header.className = "order-card-header";
        const title = document.createElement("h3");
        title.textContent = `Order #${order.id} · ${order.branchName || "Branch"}`;
        const badge = document.createElement("span");
        badge.className = `status-badge status-${status.toLowerCase()}`;
        badge.textContent = status;
        header.append(title, badge);

        const body = document.createElement("div");
        body.className = "order-card-body";
        appendLine(body, "Customer", order.customerName || "Guest");
        appendLine(body, "Type", order.orderType || "N/A");
        appendLine(body, "Items", itemSummary(order));
        if ((order.orderType || "").toLowerCase() === "delivery") {
            appendLine(body, "Address", addressSummary(order));
        }
        if (order.notes) appendLine(body, "Notes", order.notes);

        const footer = document.createElement("div");
        footer.className = "order-card-footer";
        const created = document.createElement("small");
        created.className = "text-secondary";
        created.textContent = order.createdAt ? new Date(order.createdAt).toLocaleString("en-ZA") : "";
        footer.appendChild(created);
        const actions = document.createElement("div");
        actions.className = "table-actions";
        if (status === "Pending") actions.appendChild(statusButton(order.id, "Preparing", "Prepare"));
        if (status === "Preparing") actions.appendChild(statusButton(order.id, "Completed", "Complete"));
        footer.appendChild(actions);
        article.append(header, body, footer);
        return article;
    }

    function statusButton(orderId, status, label) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "btn btn-sm btn-primary";
        button.textContent = label;
        button.addEventListener("click", async () => {
            button.disabled = true;
            try {
                const response = await fetch(`/api/admin/orders/${orderId}/status`, {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json",
                        "X-CSRF-TOKEN": csrfToken
                    },
                    body: JSON.stringify({status})
                });
                if (!response.ok) throw new Error(`Status update failed with ${response.status}`);
                await Promise.all([refreshOrders(), refreshOverview()]);
            } catch (error) {
                console.error(error);
                alert("The order status could not be updated.");
            } finally {
                button.disabled = false;
            }
        });
        return button;
    }

    function connectLiveOrders() {
        if (typeof createSimpleStompClient !== "function") return;
        setConnection("Connecting", false);
        liveClient = createSimpleStompClient({
            onConnect: () => {
                reconnectDelay = 1000;
                setConnection("Live", true);
                const topic = adminLevel === 3
                    ? "/topic/admin/orders"
                    : `/topic/admin/branches/${fixedBranchId}/orders`;
                liveClient.subscribe(topic, () => {
                    refreshOrders();
                    refreshOverview();
                });
            },
            onDisconnect: () => {
                setConnection("Reconnecting", false);
                clearTimeout(reconnectTimer);
                reconnectTimer = setTimeout(connectLiveOrders, reconnectDelay);
                reconnectDelay = Math.min(reconnectDelay * 2, 15000);
            },
            onError: error => console.error("Live order connection failed", error)
        });
        liveClient.connect();
    }

    function setConnection(label, live) {
        document.querySelectorAll("[data-live-status]").forEach(element => {
            element.classList.toggle("is-live", live);
            const text = element.querySelector("span:last-child");
            if (text) text.textContent = label;
        });
    }

    function setupCatalogModals() {
        document.querySelectorAll("[data-pizza-edit]").forEach(button => {
            button.addEventListener("click", () => populatePizzaModal(button.dataset));
        });
        document.getElementById("addPizzaButton")?.addEventListener("click", () => populatePizzaModal({}));
        document.querySelectorAll("[data-menu-edit]").forEach(button => {
            button.addEventListener("click", () => populateMenuModal(button.dataset));
        });
        document.getElementById("addMenuButton")?.addEventListener("click", () => populateMenuModal({}));
        document.querySelectorAll("[data-price-edit]").forEach(button => {
            button.addEventListener("click", () => populatePriceModal(button.dataset));
        });
        document.querySelectorAll("[data-taxonomy-edit]").forEach(button => {
            button.addEventListener("click", () => populateTaxonomyModal(button.dataset));
        });
        document.querySelectorAll("[data-ingredient-edit]").forEach(button => {
            button.addEventListener("click", () => populateIngredientModal(button.dataset));
        });
        document.querySelectorAll("[data-modifier-group-edit]").forEach(button => {
            button.addEventListener("click", () => populateModifierGroupModal(button.dataset));
        });
        document.querySelectorAll("[data-modifier-option-edit]").forEach(button => {
            button.addEventListener("click", () => populateModifierOptionModal(button.dataset));
        });
        document.querySelectorAll("[data-delete-action]").forEach(button => {
            button.addEventListener("click", () => {
                document.getElementById("deleteForm").action = button.dataset.deleteAction;
                setText("deleteItemName", button.dataset.deleteName || "this item");
            });
        });
    }

    function populatePizzaModal(data) {
        setValue("pizzaId", data.id || "");
        setValue("pizzaName", data.name || "");
        setValue("pizzaCategory", data.categoryId || "");
        setValue("pizzaDescription", data.description || "");
        setValue("pizzaSortOrder", data.sortOrder || "0");
        setChecked("pizzaActive", data.active !== "false");
        setCheckedValues(".pizza-ingredient", data.ingredientIds || "");
        setText("pizzaModalTitle", data.id ? "Edit pizza" : "Add pizza");
    }

    function populateMenuModal(data) {
        setValue("menuItemId", data.id || "");
        setValue("menuItemName", data.name || "");
        setValue("menuItemCategory", data.categoryId || "");
        setValue("menuItemDescription", data.description || "");
        setValue("menuItemSortOrder", data.sortOrder || "0");
        setChecked("menuItemActive", data.active !== "false");
        setChecked("menuItem300ml", data.is300ml === "true");
        setChecked("menuItem2l", data.is2l === "true");
        setCheckedValues(".menu-modifier-group", data.modifierGroupIds || "");
        setText("menuItemModalTitle", data.id ? "Edit menu item" : "Add menu item");
    }

    function populatePriceModal(data) {
        const form = document.getElementById("priceForm");
        form.action = data.action;
        setText("priceModalTitle", data.title || "Set price");
        setValue("priceBranchId", data.branchId || fixedBranchId || "");
        setValue("pricePizzaId", data.pizzaId || "");
        setValue("priceMenuItemId", data.menuItemId || "");
        setValue("priceCategoryId", data.priceCategoryId || "");
        setValue("pricePizzaSizeId", data.pizzaSizeId || "");
        setValue("priceValue", data.price || "");
        toggle("pricePizzaIdGroup", data.type === "pizza");
        toggle("priceMenuItemIdGroup", data.type === "menu");
        toggle("priceCategoryGroup", Boolean(data.priceCategoryId) || data.type === "topping");
        toggle("priceSizeGroup", data.type !== "menu");
    }

    function populateTaxonomyModal(data) {
        const form = document.getElementById("taxonomyForm");
        form.action = data.action;
        setText("taxonomyModalTitle", data.title || "Edit catalog setting");
        setValue("taxonomyId", data.id || "");
        setValue("taxonomyName", data.name || "");
        setValue("taxonomyCm", data.cm || "");
        setValue("taxonomySortOrder", data.sortOrder || "0");
        setChecked("taxonomyActive", data.active !== "false");
        toggle("taxonomyNameGroup", data.kind !== "size");
        toggle("taxonomyCmGroup", data.kind === "size");
    }

    function populateIngredientModal(data) {
        setValue("ingredientId", data.id || "");
        setValue("ingredientName", data.name || "");
        setValue("ingredientPriceCategory", data.priceCategoryId || "");
        setChecked("ingredientActive", data.active !== "false");
        setChecked("ingredientSeasonal", data.seasonal === "true");
        setText("ingredientModalTitle", data.id ? "Edit ingredient" : "Add ingredient");
    }

    function populateModifierGroupModal(data) {
        setValue("modifierGroupId", data.id || "");
        setValue("modifierGroupName", data.name || "");
        setValue("modifierGroupMin", data.minSelect || "0");
        setValue("modifierGroupMax", data.maxSelect || "1");
        setChecked("modifierGroupRequired", data.required === "true");
        setText("modifierGroupModalTitle", data.id ? "Edit modifier group" : "Add modifier group");
    }

    function populateModifierOptionModal(data) {
        setValue("modifierOptionId", data.id || "");
        setValue("modifierOptionGroup", data.groupId || "");
        setValue("modifierOptionName", data.name || "");
        setValue("modifierOptionMenuItem", data.menuItemId || "");
        setValue("modifierOptionPrice", data.additionalPrice || "0.00");
        setText("modifierOptionModalTitle", data.id ? "Edit modifier option" : "Add modifier option");
    }

    function setupAccountSearch() {
        const form = document.getElementById("accountSearchForm");
        if (!form) return;
        form.addEventListener("submit", event => {
            event.preventDefault();
            searchAccounts();
        });
        searchAccounts();
    }

    async function searchAccounts() {
        const body = document.getElementById("accountResults");
        const query = document.getElementById("accountQuery")?.value || "";
        body.innerHTML = '<tr><td colspan="5" class="text-center text-secondary py-4">Loading accounts...</td></tr>';
        try {
            const response = await fetch(`/api/admin/accounts?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error(`Account search failed with ${response.status}`);
            const accounts = await response.json();
            body.replaceChildren();
            accounts.forEach(account => body.appendChild(accountRow(account)));
            if (!accounts.length) {
                body.innerHTML = '<tr><td colspan="5" class="text-center text-secondary py-4">No matching accounts.</td></tr>';
            }
        } catch (error) {
            console.error(error);
            body.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-4">Accounts could not be loaded.</td></tr>';
        }
    }

    function accountRow(account) {
        const row = document.createElement("tr");
        [account.name, account.email || "—", account.phone || "—", account.accessLabel].forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });
        const actionCell = document.createElement("td");
        const form = document.createElement("form");
        form.method = "post";
        form.action = `/admin/accounts/${account.id}/access-level`;
        form.className = "d-flex gap-2";
        const csrf = document.createElement("input");
        csrf.type = "hidden";
        csrf.name = csrfParameter;
        csrf.value = csrfToken;
        const select = document.createElement("select");
        select.name = "accessLevel";
        select.className = "form-select form-select-sm";
        [[0, "Normal user"], [1, "Kenridge admin"], [2, "Uitzicht admin"],
            [3, "Super admin"], [4, "Delivery driver"]].forEach(([value, label]) => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = `${value} · ${label}`;
            option.selected = value === account.accessLevel;
            select.appendChild(option);
        });
        const submit = document.createElement("button");
        submit.type = "submit";
        submit.className = "btn btn-sm btn-primary";
        submit.textContent = "Save";
        form.append(csrf, select, submit);
        actionCell.appendChild(form);
        row.appendChild(actionCell);
        return row;
    }

    function appendLine(parent, label, value) {
        const line = document.createElement("p");
        const strong = document.createElement("strong");
        strong.textContent = `${label}: `;
        line.append(strong, document.createTextNode(value));
        parent.appendChild(line);
    }

    function itemSummary(order) {
        const items = [];
        (order.menuItems || []).forEach(item => items.push(`${item.qty || 1}× ${item.menuItemName || "Menu item"}`));
        (order.pizzaItems || []).forEach(item => items.push(`${item.qty || 1}× ${item.pizzaName || "Pizza"}`));
        return items.join(", ") || "No items";
    }

    function addressSummary(order) {
        return [order.houseNumber, order.street, order.area, order.city, order.postalCode]
            .filter(Boolean).join(", ") || "No address supplied";
    }

    function normalizedStatus(status) {
        if (!status || status.toLowerCase() === "created") return "Pending";
        return status;
    }

    function currency(value) {
        return new Intl.NumberFormat("en-ZA", {style: "currency", currency: "ZAR"}).format(Number(value || 0));
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (element) element.textContent = value;
    }

    function setValue(id, value) {
        const element = document.getElementById(id);
        if (element) element.value = value;
    }

    function setChecked(id, checked) {
        const element = document.getElementById(id);
        if (element) element.checked = checked;
    }

    function setCheckedValues(selector, rawValues) {
        const values = new Set(String(rawValues).split(",").filter(Boolean));
        document.querySelectorAll(selector).forEach(input => input.checked = values.has(input.value));
    }

    function toggle(id, visible) {
        document.getElementById(id)?.classList.toggle("d-none", !visible);
    }

    document.addEventListener("DOMContentLoaded", () => {
        setupNavigation();
        setupOverviewFilters();
        setupCatalogModals();
        setupAccountSearch();
        refreshOverview();
        refreshOrders();
        connectLiveOrders();
    });
})();
