(() => {
    "use strict";

    const STORAGE_KEY = "petesPizza.checkoutDraft.v1";
    const defaultsElement = document.getElementById("checkoutDefaults");
    const form = document.getElementById("checkoutForm");
    const deliveryFields = document.getElementById("deliveryFields");
    const deliveryButton = document.getElementById("deliveryButton");
    const pickupButton = document.getElementById("pickupButton");
    const placeOrderButton = document.getElementById("placeOrderButton");
    const notice = document.getElementById("checkoutNotice");
    const fieldIds = ["fullName", "phone", "houseNumber", "street", "area", "city", "postalCode", "complexName"];

    const serverCustomer = {
        fullName: defaultsElement?.dataset.fullName || "",
        email: defaultsElement?.dataset.email || "",
        phone: defaultsElement?.dataset.phone || "",
        houseNumber: defaultsElement?.dataset.houseNumber || "",
        street: defaultsElement?.dataset.street || "",
        area: defaultsElement?.dataset.area || "",
        city: defaultsElement?.dataset.city || "",
        postalCode: defaultsElement?.dataset.postalCode || "",
        complexName: defaultsElement?.dataset.complexName || ""
    };

    let draft = readDraft();
    let submitting = false;

    function readDraft() {
        try {
            const value = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || "null");
            if (!value || !Array.isArray(value.items)) return null;
            if (serverCustomer.email && value.accountEmail?.toLowerCase() !== serverCustomer.email.toLowerCase()) {
                sessionStorage.removeItem(STORAGE_KEY);
                return null;
            }
            return value;
        } catch (_error) {
            sessionStorage.removeItem(STORAGE_KEY);
            return null;
        }
    }

    function money(value) {
        return `R${Number(value || 0).toFixed(2)}`;
    }

    function itemDetails(item) {
        if (item.type === "special") {
            return (item.selections || []).map(selection =>
                `${selection.label || "Selection"}: ${selection.productName || "Item"}${selection.pizzaSizeCm ? ` (${selection.pizzaSizeCm}cm)` : ""}`
            ).join(" • ");
        }
        if (item.type === "pizza") {
            return [
                item.sizeCm ? `${item.sizeCm} cm` : "",
                item.pizzaBaseOptionName || "",
                ...(Array.isArray(item.selectedExtraToppingNames) ? item.selectedExtraToppingNames : [])
            ].filter(Boolean).join(" • ");
        }
        return Array.isArray(item.selectedModifierNames) ? item.selectedModifierNames.join(" • ") : "";
    }

    function renderItems() {
        const container = document.getElementById("checkoutItems");
        const emptyCart = document.getElementById("emptyCart");
        const totals = document.getElementById("summaryTotals");
        container.replaceChildren();

        const items = draft?.items || [];
        if (!items.length) {
            emptyCart.hidden = false;
            totals.hidden = true;
            updatePlaceOrderAvailability();
            return;
        }

        emptyCart.hidden = true;
        totals.hidden = false;

        let quantity = 0;
        let total = 0;
        items.forEach(item => {
            const itemQuantity = Number(item.quantity) || 1;
            const lineTotal = Number(item.pricing?.lineTotal) || 0;
            quantity += itemQuantity;
            total += lineTotal;

            const row = document.createElement("div");
            row.className = "cart-item";
            const quantityBadge = document.createElement("span");
            quantityBadge.className = "item-quantity";
            quantityBadge.textContent = `${itemQuantity}×`;
            const info = document.createElement("div");
            info.className = "cart-info";
            const name = document.createElement("strong");
            name.textContent = item.name || "Order item";
            info.appendChild(name);
            const details = itemDetails(item);
            if (details) {
                const detailLine = document.createElement("small");
                detailLine.textContent = details;
                info.appendChild(detailLine);
            }
            const price = document.createElement("strong");
            price.className = "item-price";
            price.textContent = money(lineTotal);
            row.append(quantityBadge, info, price);
            container.appendChild(row);
        });

        document.getElementById("itemCount").textContent = `${quantity} item${quantity === 1 ? "" : "s"}`;
        document.getElementById("orderTotal").textContent = money(total);
        updatePlaceOrderAvailability();
    }

    function customerValue(key) {
        const draftKey = key === "fullName" ? "name" : key;
        const fromDraft = draft?.customer?.[draftKey]?.trim();
        if (fromDraft) return fromDraft;
        const fromServer = serverCustomer[key]?.trim();
        if (fromServer) return fromServer;
        return "";
    }

    function populateCustomer() {
        fieldIds.forEach(id => {
            document.getElementById(id).value = customerValue(id);
        });
        document.getElementById("email").value = serverCustomer.email;
    }

    function updatePlaceOrderAvailability() {
        const hasItems = Boolean(draft?.items?.length);
        const hasOrderType = Boolean(document.getElementById("orderType").value);
        placeOrderButton.disabled = submitting || !hasItems || !hasOrderType;
    }

    function setOrderType(type) {
        const orderType = type === "delivery" || type === "pickup" ? type : "";
        document.getElementById("orderType").value = orderType;
        deliveryButton.classList.toggle("active", orderType === "delivery");
        pickupButton.classList.toggle("active", orderType === "pickup");
        deliveryButton.setAttribute("aria-pressed", String(orderType === "delivery"));
        pickupButton.setAttribute("aria-pressed", String(orderType === "pickup"));
        deliveryFields.hidden = orderType !== "delivery";
        document.getElementById("paymentLabel").textContent = orderType === "delivery"
            ? "Pay on delivery"
            : (orderType === "pickup" ? "Pay on collection" : "Choose an order type");
        ["street", "area", "city", "postalCode"].forEach(id => {
            document.getElementById(id).required = orderType === "delivery";
        });
        if (draft) {
            draft.orderType = orderType;
            saveDraftFromForm();
        }
        updatePlaceOrderAvailability();
    }

    function saveDraftFromForm() {
        if (!draft) return;
        draft.customer = {
            name: document.getElementById("fullName").value.trim(),
            phone: document.getElementById("phone").value.trim(),
            houseNumber: document.getElementById("houseNumber").value.trim(),
            street: document.getElementById("street").value.trim(),
            area: document.getElementById("area").value.trim(),
            city: document.getElementById("city").value.trim(),
            postalCode: document.getElementById("postalCode").value.trim(),
            complexName: document.getElementById("complexName").value.trim()
        };
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(draft));
    }

    function orderItems() {
        return draft.items.filter(item => item.type !== "special").map(item => {
            if (item.type === "menu") {
                const burgerToppings = (item.selectedBurgerToppingIds || []).map(id => ({ id: Number(id), quantity: 1, type: "burgerComponent" }));
                const burgerExtras = (item.selectedBurgerExtraToppingIds || []).map(id => ({ id: Number(id), quantity: 1, type: "burgerExtraComponent" }));
                const modifiers = (item.selectedModifierOptionIds || []).map(id => ({ id: Number(id), quantity: 1, type: "modifierOption" }));
                return { menuItemId: Number(item.menuItemId), quantity: Number(item.quantity) || 1, customizations: burgerToppings.concat(burgerExtras, modifiers) };
            }
            const selected = new Set((item.selectedToppingIds || []).map(Number));
            return {
                pizzaId: Number(item.pizzaId),
                sizeCm: Number(item.sizeCm),
                pizzaBaseOptionId: item.pizzaBaseOptionId ? Number(item.pizzaBaseOptionId) : null,
                quantity: Number(item.quantity) || 1,
                customizations: [
                    ...(item.selectedExtraToppingIds || []).map(id => ({ id: Number(id), quantity: 1 })),
                    ...(item.defaultToppingIds || []).filter(id => !selected.has(Number(id)))
                        .map(id => ({ id: Number(id), quantity: 1, type: "removedPizzaIngredient" }))
                ]
            };
        });
    }

    function specialItems() {
        return draft.items.filter(item => item.type === "special").map(item => ({...item.specialRequest, quantity: Number(item.quantity) || 1}));
    }

    function orderPayload() {
        const delivery = document.getElementById("orderType").value === "delivery";
        return {
            customerName: document.getElementById("fullName").value.trim(),
            phone: document.getElementById("phone").value.trim(),
            houseNumber: delivery ? document.getElementById("houseNumber").value.trim() : "",
            street: delivery ? document.getElementById("street").value.trim() : "",
            area: delivery ? document.getElementById("area").value.trim() : "",
            city: delivery ? document.getElementById("city").value.trim() : "",
            postalCode: delivery ? document.getElementById("postalCode").value.trim() : "",
            complexName: delivery ? document.getElementById("complexName").value.trim() : "",
            branchName: draft.branch.name,
            orderType: document.getElementById("orderType").value,
            items: orderItems(),
            specialItems: specialItems()
        };
    }

    function showNotice(message, type) {
        notice.textContent = message;
        notice.className = `checkout-notice ${type}`;
        notice.hidden = false;
    }

    async function placeOrder() {
        if (submitting || !draft?.items?.length) return;
        if (!document.getElementById("orderType").value) {
            showNotice("Choose collection or delivery before placing your order.", "error");
            updatePlaceOrderAvailability();
            return;
        }
        if (!form.checkValidity()) {
            form.reportValidity();
            showNotice("Please complete the required customer details and delivery address.", "error");
            return;
        }

        submitting = true;
        placeOrderButton.disabled = true;
        placeOrderButton.textContent = "Placing order…";
        saveDraftFromForm();

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const headers = { "Content-Type": "application/json" };
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
            const response = await fetch("/api/orders", { method: "POST", headers, body: JSON.stringify(orderPayload()) });
            if (!response.ok) {
                let message = `Could not place the order (${response.status}).`;
                try {
                    const problem = await response.json();
                    message = problem.message || problem.error || message;
                } catch (_error) {
                    const text = await response.text().catch(() => "");
                    if (text) message = text;
                }
                throw new Error(message);
            }

            const placedOrder = await response.json();
            sessionStorage.removeItem(STORAGE_KEY);
            showNotice(`Order #${placedOrder.id} has been placed. Status: ${placedOrder.status}.`, "success");
            placeOrderButton.textContent = "Order placed";
            document.getElementById("editOrderLink").hidden = true;
            document.getElementById("backToOrderTop").hidden = true;
            form.querySelectorAll("input").forEach(input => { input.disabled = true; });
            deliveryButton.disabled = true;
            pickupButton.disabled = true;
        } catch (error) {
            showNotice(error.message || "Could not place the order. Please try again.", "error");
            placeOrderButton.textContent = "Place order";
            submitting = false;
            updatePlaceOrderAvailability();
        }
    }

    populateCustomer();
    document.getElementById("branchName").textContent = draft?.branch?.name || "your selected branch";
    setOrderType(draft?.orderType === "delivery" || draft?.orderType === "pickup" ? draft.orderType : null);
    renderItems();
    deliveryButton.addEventListener("click", () => setOrderType("delivery"));
    pickupButton.addEventListener("click", () => setOrderType("pickup"));
    form.addEventListener("input", saveDraftFromForm);
    placeOrderButton.addEventListener("click", placeOrder);
})();
