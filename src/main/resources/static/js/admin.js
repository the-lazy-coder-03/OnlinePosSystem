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
        appendOrderItems(body, order);
        if ((order.orderType || "").toLowerCase() === "delivery") {
            appendLine(body, "Address", addressSummary(order));
            const accessCode = gateAccessSummary(order);
            if (accessCode) appendLine(body, "Gate access", accessCode);
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

    function setupCatalogFilters() {
        document.querySelectorAll("[data-catalog-filter]").forEach(toolbar => {
            const filterName = toolbar.dataset.catalogFilter;
            const tableBody = document.querySelector(`[data-filter-table="${filterName}"]`);
            if (!tableBody) return;

            const rows = [...tableBody.querySelectorAll("[data-catalog-row]")];
            const search = toolbar.querySelector("[data-filter-search]");
            const category = toolbar.querySelector("[data-filter-category]");
            const status = toolbar.querySelector("[data-filter-status]");
            const format = toolbar.querySelector("[data-filter-format]");
            const clear = toolbar.querySelector("[data-filter-clear]");
            const count = toolbar.querySelector("[data-filter-count]");
            const empty = tableBody.querySelector("[data-filter-empty]");

            const applyFilters = () => {
                const query = normalizedFilterText(search?.value);
                const selectedCategory = category?.value || "";
                const selectedStatus = status?.value || "";
                const selectedFormat = format?.value || "";
                let visible = 0;

                rows.forEach(row => {
                    const searchableText = normalizedFilterText(
                        `${row.dataset.filterName || ""} ${row.dataset.filterDescription || ""}`
                    );
                    const formats = new Set((row.dataset.filterFormats || "").split(/\s+/).filter(Boolean));
                    const matches = (!query || searchableText.includes(query))
                        && (!selectedCategory || row.dataset.filterCategory === selectedCategory)
                        && (!selectedStatus || row.dataset.filterStatus === selectedStatus)
                        && (!selectedFormat || formats.has(selectedFormat));
                    row.classList.toggle("d-none", !matches);
                    if (matches) visible += 1;
                });

                empty?.classList.toggle("d-none", visible !== 0);
                if (count) {
                    const label = toolbar.dataset.filterLabel || "items";
                    count.textContent = `Showing ${visible} of ${rows.length} ${label}`;
                }
            };

            search?.addEventListener("input", applyFilters);
            [category, status, format].forEach(control => control?.addEventListener("change", applyFilters));
            clear?.addEventListener("click", () => {
                if (search) search.value = "";
                [category, status, format].forEach(control => {
                    if (control) control.value = "";
                });
                applyFilters();
                search?.focus();
            });
            applyFilters();
        });
    }

    function normalizedFilterText(value) {
        return String(value || "").trim().toLocaleLowerCase();
    }

    function setupCatalogModals() {
        document.querySelectorAll("[data-category-price-form]").forEach(form => {
            form.addEventListener("submit", event => {
                const label = name => form.elements.namedItem(name)?.selectedOptions[0]?.textContent || "";
                const scope = [label("categoryId"), label("pizzaSizeId"), label("branchId")].filter(Boolean).join(" / ");
                const price = Number(form.elements.namedItem("price").value).toFixed(2);
                if (!window.confirm(`Set all existing prices for ${scope} to R${price}? Previous individual adjustments will be overwritten.`)) {
                    event.preventDefault();
                }
            });
        });

        document.querySelectorAll("[data-pizza-edit]").forEach(button => {
            button.addEventListener("click", () => populatePizzaModal(button.dataset));
        });
        document.getElementById("addPizzaButton")?.addEventListener("click", () => populatePizzaModal({}));
        document.querySelectorAll("[data-pizza-size-toggle]").forEach(input => {
            input.addEventListener("change", updatePizzaPriceInputs);
        });
        document.querySelector("#pizzaModal form")?.addEventListener("submit", event => {
            if (document.getElementById("pizzaId").value) return;
            const sizes = [...document.querySelectorAll("[data-pizza-size-toggle]")];
            if (sizes.some(input => input.checked)) return;
            event.preventDefault();
            if (sizes.length) {
                sizes[0].setCustomValidity("Select at least one pizza size.");
                sizes[0].reportValidity();
            }
        });
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
        setChecked("pizzaActive", data.active !== "false");
        setCheckedValues(".pizza-ingredient", data.ingredientIds || "");
        document.querySelectorAll("[data-pizza-size-toggle]").forEach(input => input.checked = false);
        document.querySelectorAll("[data-pizza-price-size-id]").forEach(input => input.value = "");
        toggle("pizzaCreationPrices", !data.id);
        updatePizzaPriceInputs();
        setText("pizzaModalTitle", data.id ? "Edit pizza" : "Add pizza");
    }

    function updatePizzaPriceInputs() {
        const creating = !document.getElementById("pizzaId").value;
        const selected = new Set([...document.querySelectorAll("[data-pizza-size-toggle]:checked")]
            .map(input => input.dataset.sizeId));
        document.querySelectorAll("[data-pizza-size-toggle]").forEach(input => {
            input.disabled = !creating;
            input.setCustomValidity("");
        });
        document.querySelectorAll("[data-pizza-price-size-id]").forEach(input => {
            const enabled = creating && selected.has(input.dataset.pizzaPriceSizeId);
            input.disabled = !enabled;
            input.required = enabled;
        });
        document.querySelectorAll("[data-pizza-size-price-row]").forEach(row => {
            row.classList.toggle("d-none", !creating || !selected.has(row.dataset.pizzaSizePriceRow));
        });
    }

    function populateMenuModal(data) {
        setValue("menuItemId", data.id || "");
        setValue("menuItemName", data.name || "");
        setValue("menuItemCategory", data.categoryId || "");
        setValue("menuItemDescription", data.description || "");
        setChecked("menuItemActive", data.active !== "false");
        setChecked("menuItem300ml", data.is300ml === "true");
        setChecked("menuItem2l", data.is2l === "true");
        setCheckedValues(".menu-modifier-group", data.modifierGroupIds || "");
        toggle("menuItemCreationPrices", !data.id);
        document.querySelectorAll("[data-create-menu-price]").forEach(input => {
            input.value = "";
            input.disabled = Boolean(data.id);
            input.required = !data.id;
        });
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

    let usersQuery = "";
    let usersPage = 0;
    let selectedUserId = null;

    function setupUsers() {
        const form = document.getElementById("usersSearchForm");
        if (!form) return;
        form.addEventListener("submit", event => {
            event.preventDefault();
            usersQuery = document.getElementById("usersQuery").value.trim();
            usersPage = 0;
            loadUsers();
        });
        document.getElementById("userBackButton").addEventListener("click", () => {
            selectedUserId = null;
            toggle("usersListView", true);
            toggle("userDetailView", false);
            loadUsers();
        });
        document.getElementById("userNoteForm").addEventListener("submit", addUserNote);
        loadUsers();
    }

    async function getAdminJson(url) {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Request failed with ${response.status}`);
        return response.json();
    }

    function showTableMessage(body, message, className = "text-secondary") {
        body.replaceChildren();
        const row = body.insertRow();
        const cell = row.insertCell();
        cell.colSpan = 6;
        cell.className = `text-center ${className} py-4`;
        cell.textContent = message;
    }

    async function loadUsers() {
        const body = document.getElementById("usersResults");
        showTableMessage(body, "Loading users...");
        document.getElementById("usersPagination").replaceChildren();
        try {
            const params = new URLSearchParams({query: usersQuery, page: String(usersPage)});
            const result = await getAdminJson(`/api/admin/customers?${params}`);
            body.replaceChildren();
            if (!result.items.length) showTableMessage(body, "No matching users.");
            result.items.forEach(user => body.appendChild(userRow(user)));
            renderPagination("usersPagination", result, page => {
                usersPage = page;
                loadUsers();
            });
        } catch (error) {
            console.error(error);
            showTableMessage(body, "Users could not be loaded.", "text-danger");
        }
    }

    function userRow(user) {
        const row = document.createElement("tr");
        row.className = "user-result-row";
        row.addEventListener("click", () => openUser(user.id));
        const name = [user.firstName, user.lastName].filter(Boolean).join(" ") || "Unnamed user";
        [name, user.email || "—", user.phone1 || "—", user.accessLabel,
            displayDate(user.lastOrderedAt)].forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });
        const action = document.createElement("td");
        action.className = "text-end";
        const button = document.createElement("button");
        button.type = "button";
        button.className = "btn btn-sm btn-outline-primary";
        button.textContent = "View";
        button.setAttribute("aria-label", `View ${name}`);
        button.addEventListener("click", event => {
            event.stopPropagation();
            openUser(user.id);
        });
        action.appendChild(button);
        row.appendChild(action);
        return row;
    }

    function renderPagination(containerId, result, onPage) {
        const container = document.getElementById(containerId);
        container.replaceChildren();
        if (result.totalItems === 0) return;
        const label = document.createElement("span");
        label.className = "text-secondary small";
        label.textContent = `Page ${result.page + 1} of ${result.totalPages} · ${result.totalItems} total`;
        const controls = document.createElement("div");
        controls.className = "btn-group btn-group-sm";
        [["Previous", result.page - 1, result.page === 0],
            ["Next", result.page + 1, result.page + 1 >= result.totalPages]].forEach(([text, page, disabled]) => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "btn btn-outline-secondary";
            button.textContent = text;
            button.disabled = disabled;
            button.addEventListener("click", () => onPage(page));
            controls.appendChild(button);
        });
        container.append(label, controls);
    }

    async function openUser(id) {
        selectedUserId = id;
        toggle("usersListView", false);
        toggle("userDetailView", true);
        setText("userDetailName", "Loading user...");
        document.getElementById("userProfile").replaceChildren();
        document.getElementById("userOrders").replaceChildren();
        document.getElementById("userNotes").replaceChildren();
        setText("userNoteError", "");
        document.getElementById("userNoteBody").value = "";
        try {
            const user = await getAdminJson(`/api/admin/customers/${id}`);
            if (selectedUserId !== id) return;
            const name = [user.firstName, user.lastName].filter(Boolean).join(" ") || "Unnamed user";
            setText("userDetailName", name);
            setText("userDetailAccess", `${user.accessLabel} · Level ${user.accessLevel}`);
            const profile = document.getElementById("userProfile");
            [["Email", user.email], ["Primary phone", user.phone1], ["Secondary phone", user.phone2],
                ["House number", user.houseNumber], ["Street", user.street], ["Area", user.area],
                ["Complex", user.complexName], ["City", user.city], ["Postal code", user.postalCode],
                ["Preferred store", user.preferredStore], ["Last order", displayDate(user.lastOrderedAt)]].forEach(([label, value]) => {
                const field = document.createElement("div");
                const term = document.createElement("dt");
                term.textContent = label;
                const description = document.createElement("dd");
                description.textContent = value || "—";
                field.append(term, description);
                profile.appendChild(field);
            });
            loadUserOrders(0);
            loadUserNotes(0);
        } catch (error) {
            console.error(error);
            setText("userDetailName", "User could not be loaded");
        }
    }

    async function loadUserOrders(page) {
        const id = selectedUserId;
        const container = document.getElementById("userOrders");
        container.textContent = "Loading orders...";
        try {
            const result = await getAdminJson(`/api/admin/customers/${id}/orders?page=${page}`);
            if (selectedUserId !== id) return;
            container.replaceChildren();
            if (!result.items.length) container.textContent = "No linked orders in your branch scope.";
            result.items.forEach(entry => container.appendChild(historicalOrder(entry)));
            renderPagination("userOrdersPagination", result, loadUserOrders);
        } catch (error) {
            console.error(error);
            container.textContent = "Orders could not be loaded.";
        }
    }

    function historicalOrder(entry) {
        const order = entry.order;
        const details = document.createElement("details");
        details.className = "user-order";
        const summary = document.createElement("summary");
        const identity = document.createElement("span");
        identity.textContent = `#${order.id} · ${order.branchName || "Branch"} · ${displayDate(order.createdAt)}`;
        const status = document.createElement("span");
        status.className = "text-secondary";
        status.textContent = `${normalizedStatus(order.status)} · ${currency(entry.total)}`;
        summary.append(identity, status);
        details.appendChild(summary);
        const content = document.createElement("div");
        content.className = "user-order-content";
        appendLine(content, "Type", order.orderType || "—");
        appendOrderItems(content, order);
        appendLine(content, "Order address", [order.houseNumber, order.street, order.area,
            order.complexName, order.city, order.postalCode].filter(Boolean).join(", ") || "—");
        const accessCode = gateAccessSummary(order);
        if (accessCode) appendLine(content, "Gate access", accessCode);
        if (order.notes) appendLine(content, "Order notes", order.notes);
        details.appendChild(content);
        return details;
    }

    async function loadUserNotes(page) {
        const id = selectedUserId;
        const container = document.getElementById("userNotes");
        container.textContent = "Loading notes...";
        try {
            const result = await getAdminJson(`/api/admin/customers/${id}/notes?page=${page}`);
            if (selectedUserId !== id) return;
            container.replaceChildren();
            if (!result.items.length) container.textContent = "No staff notes yet.";
            result.items.forEach(note => {
                const item = document.createElement("article");
                item.className = "user-note";
                const meta = document.createElement("div");
                meta.className = "small text-secondary";
                meta.textContent = `${note.authorUsername} · ${displayDate(note.createdAt)}`;
                const body = document.createElement("p");
                body.textContent = note.body;
                item.append(meta, body);
                container.appendChild(item);
            });
            renderPagination("userNotesPagination", result, loadUserNotes);
        } catch (error) {
            console.error(error);
            container.textContent = "Notes could not be loaded.";
        }
    }

    async function addUserNote(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const body = document.getElementById("userNoteBody").value.trim();
        setText("userNoteError", "");
        if (!body || body.length > 2000) {
            setText("userNoteError", "Enter a note of 1 to 2,000 characters.");
            return;
        }
        const submit = form.querySelector('button[type="submit"]');
        submit.disabled = true;
        try {
            const response = await fetch(`/admin/customers/${selectedUserId}/notes`, {
                method: "POST",
                headers: {"Content-Type": "application/json", "X-CSRF-TOKEN": csrfToken},
                body: JSON.stringify({body})
            });
            if (!response.ok) throw new Error(`Note save failed with ${response.status}`);
            document.getElementById("userNoteBody").value = "";
            await loadUserNotes(0);
        } catch (error) {
            console.error(error);
            setText("userNoteError", "Note could not be saved.");
        } finally {
            submit.disabled = false;
        }
    }

    function displayDate(value) {
        return value ? String(value).replace("T", " ").slice(0, 16) : "—";
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

    function appendOrderItems(parent, order) {
        const section = document.createElement("div");
        section.className = "order-items";

        const label = document.createElement("strong");
        label.className = "order-items-label";
        label.textContent = "Items";
        section.appendChild(label);

        const list = document.createElement("div");
        list.className = "order-item-list";

        const items = [
            ...(order.specialItems || []).map(renderSpecialOrderItem),
            ...(order.menuItems || []).map(renderMenuOrderItem),
            ...(order.pizzaItems || []).map(renderPizzaOrderItem)
        ];

        if (items.length) {
            items.forEach(item => list.appendChild(item));
        } else {
            const empty = document.createElement("p");
            empty.className = "order-items-empty";
            empty.textContent = "No items";
            list.appendChild(empty);
        }

        section.appendChild(list);
        parent.appendChild(section);
    }

    function renderSpecialOrderItem(item) {
        const details = (item.selections || []).map(selection => {
            const size = selection.pizzaSizeCm ? ` (${selection.pizzaSizeCm}cm)` : "";
            return `${selection.label || "Selection"}: ${selection.productName || "Item"}${size}`;
        });
        details.push(`Bundle total: ${currency(item.finalLineTotalAtTime)}`);
        return renderOrderItem(`${item.quantity || 1}× ${item.name || "Special"}`, details);
    }

    function renderMenuOrderItem(item) {
        const details = (item.extras || [])
            .filter(extra => extra && extra.name)
            .map(extra => quantityLabel(extra.qty, extra.name));
        if (item.notes) details.push(`Notes: ${item.notes}`);

        return renderOrderItem(
            `${item.qty || 1}× ${item.menuItemName || "Menu item"}`,
            details
        );
    }

    function renderPizzaOrderItem(item) {
        const size = item.pizzaSizeCm ? `${item.pizzaSizeCm}cm` : null;
        const title = [
            `${item.qty || 1}× ${item.pizzaName || "Pizza"}`,
            size ? `(${size})` : null
        ].filter(Boolean).join(" ");

        const details = [];
        if (item.pizzaBaseOptionName) {
            details.push(`Base: ${item.pizzaBaseOptionName}`);
        }
        (item.extras || [])
            .filter(extra => extra && extra.ingredientName)
            .forEach(extra => details.push(`Extra topping: ${quantityLabel(extra.qty, extra.ingredientName)}`));
        (item.removedIngredients || []).forEach(name => details.push(`No ${name}`));
        if (item.notes) details.push(`Notes: ${item.notes}`);

        return renderOrderItem(title, details);
    }

    function renderOrderItem(title, details) {
        const item = document.createElement("div");
        item.className = "order-item";

        const heading = document.createElement("div");
        heading.className = "order-item-title";
        heading.textContent = title;
        item.appendChild(heading);

        if (details.length) {
            const detailList = document.createElement("ul");
            detailList.className = "order-item-details";
            details.forEach(detail => {
                const row = document.createElement("li");
                row.textContent = detail;
                detailList.appendChild(row);
            });
            item.appendChild(detailList);
        }

        return item;
    }

    let specialCatalog = null;
    let specialRows = [];

    async function setupSpecials() {
        if (adminLevel !== 3 || !document.getElementById("specialsTableBody")) return;
        document.getElementById("addSpecialButton")?.addEventListener("click", () => openSpecialEditor());
        document.getElementById("addSpecialComponent")?.addEventListener("click", () => addSpecialComponentRow());
        document.getElementById("addSpecialAddon")?.addEventListener("click", () => addSpecialAddonRow());
        document.getElementById("specialForm")?.addEventListener("submit", saveSpecial);
        const dayNames = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
        const days = document.getElementById("specialDays");
        dayNames.forEach((name, index) => {
            const wrapper = document.createElement("div");
            wrapper.className = "form-check";
            wrapper.innerHTML = `<input class="form-check-input" type="checkbox" value="${index + 1}" id="specialDay${index + 1}"><label class="form-check-label" for="specialDay${index + 1}">${name}</label>`;
            days.appendChild(wrapper);
        });
        try {
            const [catalogResponse, specialsResponse] = await Promise.all([
                fetch("/api/admin/specials/catalog"), fetch("/api/admin/specials")
            ]);
            if (!catalogResponse.ok || !specialsResponse.ok) throw new Error("Could not load specials.");
            specialCatalog = await catalogResponse.json();
            specialRows = await specialsResponse.json();
            renderSpecialsTable();
        } catch (error) {
            showSpecialNotice(error.message, "danger");
        }
    }

    function renderSpecialsTable() {
        const body = document.getElementById("specialsTableBody");
        body.replaceChildren();
        const branches = new Map((specialCatalog?.branches || []).map(value => [value.id, value.name]));
        const dayNames = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
        specialRows.forEach(special => {
            const row = document.createElement("tr");
            const status = special.archived ? "Archived" : (special.active ? "Active" : "Inactive");
            [special.name, branches.get(special.branchId) || special.branchId,
                (special.days || []).sort().map(day => dayNames[day - 1]).join(", "), currency(special.price), status]
                .forEach(value => { const cell = document.createElement("td"); cell.textContent = value; row.appendChild(cell); });
            const actions = document.createElement("td");
            const edit = document.createElement("button");
            edit.type = "button"; edit.className = "btn btn-sm btn-outline-primary me-2"; edit.innerHTML = '<i class="bi bi-pencil"></i><span class="visually-hidden">Edit</span>';
            edit.addEventListener("click", () => openSpecialEditor(special));
            const archive = document.createElement("button");
            archive.type = "button"; archive.className = `btn btn-sm ${special.archived ? "btn-outline-success" : "btn-outline-danger"}`;
            archive.textContent = special.archived ? "Restore" : "Archive";
            archive.addEventListener("click", () => changeSpecialArchive(special));
            actions.append(edit, archive); row.appendChild(actions); body.appendChild(row);
        });
        if (!specialRows.length) body.innerHTML = '<tr><td colspan="6" class="text-center text-secondary py-4">No specials configured.</td></tr>';
    }

    function optionList(values, selected, placeholder = "None") {
        const selectedSet = new Set((selected || []).map(Number));
        return `<option value="">${placeholder}</option>` + (values || []).map(value =>
            `<option value="${value.id}" ${selectedSet.has(Number(value.id)) ? "selected" : ""}>${escapeAdmin(value.name)}</option>`).join("");
    }

    function productOptions(values, selected) {
        const selectedSet = new Set((selected || []).map(Number));
        return (values || []).map(value => `<option value="${value.id}" ${selectedSet.has(Number(value.id)) ? "selected" : ""}>${escapeAdmin(value.name)}</option>`).join("");
    }

    function openSpecialEditor(special = null) {
        const form = document.getElementById("specialForm");
        form.reset();
        document.getElementById("specialFormError").classList.add("d-none");
        setValue("specialId", special?.id || "");
        setValue("specialCode", special?.code || "");
        setValue("specialName", special?.name || "");
        setValue("specialDescriptionInput", special?.description || "");
        setValue("specialPrice", special?.price ?? "");
        setValue("specialStartsOn", special?.startsOn || "");
        setValue("specialEndsOn", special?.endsOn || "");
        setValue("specialSortOrder", special?.sortOrder ?? 0);
        setChecked("specialActive", Boolean(special?.active));
        document.getElementById("specialBranch").innerHTML = optionList(specialCatalog.branches, special ? [special.branchId] : [1], "Choose branch");
        document.querySelectorAll("#specialDays input").forEach(input => input.checked = (special?.days || []).includes(Number(input.value)));
        document.getElementById("specialComponents").replaceChildren();
        document.getElementById("specialAddons").replaceChildren();
        (special?.components || []).forEach(addSpecialComponentRow);
        (special?.addons || []).forEach(addSpecialAddonRow);
        if (!special) addSpecialComponentRow();
        document.getElementById("specialModalTitle").textContent = special ? "Edit special" : "Add special";
        bootstrap.Modal.getOrCreateInstance(document.getElementById("specialModal")).show();
    }

    function addSpecialComponentRow(component = {}) {
        const row = document.createElement("div");
        row.className = "border rounded p-3 mb-3 special-component-row";
        const optionIds = (component.options || []).map(value => value.id);
        row.innerHTML = `<div class="row g-2">
            <div class="col-md-2"><label class="form-label">Code</label><input class="form-control" data-field="code" value="${escapeAdmin(component.code || "")}" required></div>
            <div class="col-md-3"><label class="form-label">Label</label><input class="form-control" data-field="label" value="${escapeAdmin(component.label || "")}" required></div>
            <div class="col-md-2"><label class="form-label">Product type</label><select class="form-select" data-field="productType"><option value="MENU_ITEM">Menu item</option><option value="PIZZA">Pizza</option></select></div>
            <div class="col-md-2"><label class="form-label">Quantity</label><input class="form-control" data-field="quantity" type="number" min="1" max="20" value="${component.quantity || 1}" required></div>
            <div class="col-md-2"><label class="form-label">Mode</label><select class="form-select" data-field="selectionMode"><option value="CUSTOMER_CHOICE">Customer choice</option><option value="INCLUDED">Included</option></select></div>
            <div class="col-md-1 d-flex align-items-end"><button class="btn btn-outline-danger" type="button" data-remove><i class="bi bi-trash"></i></button></div>
            <div class="col-md-3 menu-rule"><label class="form-label">Menu category</label><select class="form-select" data-field="menuCategoryId">${optionList(specialCatalog.menuCategories, component.menuCategoryId ? [component.menuCategoryId] : [])}</select></div>
            <div class="col-md-3 pizza-rule"><label class="form-label">Pizza category</label><select class="form-select" data-field="pizzaCategoryId">${optionList(specialCatalog.pizzaCategories, component.pizzaCategoryId ? [component.pizzaCategoryId] : [])}</select></div>
            <div class="col-md-2 pizza-rule"><label class="form-label">Locked size</label><select class="form-select" data-field="pizzaSizeId">${optionList(specialCatalog.pizzaSizes, component.pizzaSizeId ? [component.pizzaSizeId] : [], "Choose size")}</select></div>
            <div class="col-md-4 menu-rule"><label class="form-label">Eligible menu items</label><select class="form-select" data-field="menuItemIds" multiple size="4">${productOptions(specialCatalog.menuItems, optionIds)}</select></div>
            <div class="col-md-4 pizza-rule"><label class="form-label">Eligible pizzas</label><select class="form-select" data-field="pizzaIds" multiple size="4">${productOptions(specialCatalog.pizzas, optionIds)}</select></div>
            <div class="col-md-2"><label class="form-label">Sort</label><input class="form-control" data-field="sortOrder" type="number" min="0" value="${component.sortOrder || 0}"></div>
            <div class="col-md-2 d-flex align-items-center"><div class="form-check"><input class="form-check-input" data-field="allowRepeats" type="checkbox" ${component.allowRepeats ? "checked" : ""}><label class="form-check-label">Allow repeats</label></div></div>
            <div class="col-md-2 d-flex align-items-center"><div class="form-check"><input class="form-check-input" data-field="allowCustomization" type="checkbox" ${component.allowCustomization ? "checked" : ""}><label class="form-check-label">Customizable</label></div></div>
        </div>`;
        row.querySelector('[data-field="productType"]').value = component.productType || "MENU_ITEM";
        row.querySelector('[data-field="selectionMode"]').value = component.selectionMode || "CUSTOMER_CHOICE";
        const refresh = () => row.querySelectorAll(".pizza-rule").forEach(el => el.classList.toggle("d-none", row.querySelector('[data-field="productType"]').value !== "PIZZA"))
            || row.querySelectorAll(".menu-rule").forEach(el => el.classList.toggle("d-none", row.querySelector('[data-field="productType"]').value !== "MENU_ITEM"));
        row.querySelector('[data-field="productType"]').addEventListener("change", refresh);
        row.querySelector("[data-remove]").addEventListener("click", () => row.remove());
        document.getElementById("specialComponents").appendChild(row); refresh();
    }

    function addSpecialAddonRow(addon = {}) {
        const row = document.createElement("div");
        row.className = "border rounded p-3 mb-3 special-addon-row";
        row.innerHTML = `<div class="row g-2">
            <div class="col-md-2"><label class="form-label">Code</label><input class="form-control" data-field="code" value="${escapeAdmin(addon.code || "")}" required></div>
            <div class="col-md-3"><label class="form-label">Label</label><input class="form-control" data-field="label" value="${escapeAdmin(addon.label || "")}" required></div>
            <div class="col-md-2"><label class="form-label">Type</label><select class="form-select" data-field="productType"><option value="MENU_ITEM">Menu item</option><option value="PIZZA">Pizza</option></select></div>
            <div class="col-md-3 menu-rule"><label class="form-label">Menu item</label><select class="form-select" data-field="menuItemId">${optionList(specialCatalog.menuItems, addon.productType === "MENU_ITEM" ? [addon.productId] : [])}</select></div>
            <div class="col-md-3 pizza-rule"><label class="form-label">Pizza</label><select class="form-select" data-field="pizzaId">${optionList(specialCatalog.pizzas, addon.productType === "PIZZA" ? [addon.productId] : [])}</select></div>
            <div class="col-md-2 pizza-rule"><label class="form-label">Size</label><select class="form-select" data-field="pizzaSizeId">${optionList(specialCatalog.pizzaSizes, addon.pizzaSizeId ? [addon.pizzaSizeId] : [])}</select></div>
            <div class="col-md-2"><label class="form-label">Price</label><input class="form-control" data-field="price" type="number" min="0" step="0.01" value="${addon.price ?? 0}" required></div>
            <div class="col-md-2"><label class="form-label">Limit</label><input class="form-control" data-field="maxQuantity" type="number" min="1" value="${addon.maxQuantity || 1}"></div>
            <div class="col-md-2"><label class="form-label">Sort</label><input class="form-control" data-field="sortOrder" type="number" min="0" value="${addon.sortOrder || 0}"></div>
            <div class="col-md-2 d-flex align-items-center"><div class="form-check"><input class="form-check-input" data-field="allowCustomization" type="checkbox" ${addon.allowCustomization ? "checked" : ""}><label class="form-check-label">Customizable</label></div></div>
            <div class="col-md-1 d-flex align-items-center"><div class="form-check"><input class="form-check-input" data-field="active" type="checkbox" ${addon.active !== false ? "checked" : ""}><label class="form-check-label">Active</label></div></div>
            <div class="col-md-1"><button class="btn btn-outline-danger" type="button" data-remove><i class="bi bi-trash"></i></button></div>
        </div>`;
        row.querySelector('[data-field="productType"]').value = addon.productType || "PIZZA";
        const refresh = () => { const pizza = row.querySelector('[data-field="productType"]').value === "PIZZA"; row.querySelectorAll(".pizza-rule").forEach(el => el.classList.toggle("d-none", !pizza)); row.querySelectorAll(".menu-rule").forEach(el => el.classList.toggle("d-none", pizza)); };
        row.querySelector('[data-field="productType"]').addEventListener("change", refresh);
        row.querySelector("[data-remove]").addEventListener("click", () => row.remove());
        document.getElementById("specialAddons").appendChild(row); refresh();
    }

    function field(row, name) { return row.querySelector(`[data-field="${name}"]`); }
    function nullableNumber(value) { return value === "" || value == null ? null : Number(value); }
    function selectedNumbers(select) { return Array.from(select.selectedOptions).map(option => Number(option.value)); }

    async function saveSpecial(event) {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const components = Array.from(document.querySelectorAll(".special-component-row")).map(row => ({
            code: field(row, "code").value, label: field(row, "label").value, productType: field(row, "productType").value,
            quantity: Number(field(row, "quantity").value), selectionMode: field(row, "selectionMode").value,
            menuCategoryId: nullableNumber(field(row, "menuCategoryId").value), pizzaCategoryId: nullableNumber(field(row, "pizzaCategoryId").value),
            pizzaSizeId: nullableNumber(field(row, "pizzaSizeId").value), allowRepeats: field(row, "allowRepeats").checked,
            allowCustomization: field(row, "allowCustomization").checked, sortOrder: Number(field(row, "sortOrder").value || 0),
            menuItemIds: selectedNumbers(field(row, "menuItemIds")), pizzaIds: selectedNumbers(field(row, "pizzaIds"))
        }));
        const addons = Array.from(document.querySelectorAll(".special-addon-row")).map(row => ({
            code: field(row, "code").value, label: field(row, "label").value, productType: field(row, "productType").value,
            menuItemId: nullableNumber(field(row, "menuItemId").value), pizzaId: nullableNumber(field(row, "pizzaId").value),
            pizzaSizeId: nullableNumber(field(row, "pizzaSizeId").value), price: Number(field(row, "price").value),
            maxQuantity: Number(field(row, "maxQuantity").value), allowCustomization: field(row, "allowCustomization").checked,
            active: field(row, "active").checked, sortOrder: Number(field(row, "sortOrder").value || 0)
        }));
        const payload = {code: document.getElementById("specialCode").value, branchId: Number(document.getElementById("specialBranch").value),
            name: document.getElementById("specialName").value, description: document.getElementById("specialDescriptionInput").value,
            bundlePrice: Number(document.getElementById("specialPrice").value), active: document.getElementById("specialActive").checked,
            startsOn: document.getElementById("specialStartsOn").value || null, endsOn: document.getElementById("specialEndsOn").value || null,
            days: Array.from(document.querySelectorAll("#specialDays input:checked")).map(input => Number(input.value)),
            sortOrder: Number(document.getElementById("specialSortOrder").value || 0), components, addons};
        const id = document.getElementById("specialId").value;
        try {
            const response = await fetch(id ? `/api/admin/specials/${id}` : "/api/admin/specials", {method: id ? "PUT" : "POST",
                headers: {"Content-Type": "application/json", "X-CSRF-TOKEN": csrfToken}, body: JSON.stringify(payload)});
            if (!response.ok) throw new Error(await response.text() || "Could not save special.");
            bootstrap.Modal.getInstance(document.getElementById("specialModal"))?.hide();
            const updated = await response.json();
            specialRows = id ? specialRows.map(value => value.id === updated.id ? updated : value) : [...specialRows, updated];
            renderSpecialsTable(); showSpecialNotice("Special saved.", "success");
        } catch (error) { const box = document.getElementById("specialFormError"); box.textContent = error.message; box.classList.remove("d-none"); }
    }

    async function changeSpecialArchive(special) {
        const action = special.archived ? "restore" : "archive";
        const response = await fetch(`/api/admin/specials/${special.id}/${action}`, {method: "POST", headers: {"X-CSRF-TOKEN": csrfToken}});
        if (!response.ok) { showSpecialNotice(await response.text() || `Could not ${action} special.`, "danger"); return; }
        const updated = await response.json(); specialRows = specialRows.map(value => value.id === updated.id ? updated : value); renderSpecialsTable();
        showSpecialNotice(special.archived ? "Special restored as inactive." : "Special archived.", "success");
    }

    function showSpecialNotice(message, type) {
        const notice = document.getElementById("specialsNotice");
        if (!notice) return;
        notice.textContent = message; notice.className = `alert alert-${type}`;
    }

    function escapeAdmin(value) {
        return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;");
    }

    function quantityLabel(qty, name) {
        const quantity = Number(qty || 1);
        return quantity > 1 ? `${quantity}× ${name}` : name;
    }

    function addressSummary(order) {
        return [order.houseNumber, order.street, order.area, order.city, order.postalCode]
            .filter(Boolean).join(", ") || "No address supplied";
    }

    function gateAccessSummary(order) {
        if ((order.orderType || "").toLowerCase() !== "delivery") return "";
        return String(order.gateAccessCode || "").trim();
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
        setupCatalogFilters();
        setupCatalogModals();
        setupUsers();
        setupAccountSearch();
        setupSpecials();
        refreshOverview();
        refreshOrders();
        connectLiveOrders();
    });
})();
