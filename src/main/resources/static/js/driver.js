(() => {
    "use strict";
    const csrf = document.querySelector('meta[name="_csrf"]')?.content || "";
    const money = value => new Intl.NumberFormat("en-ZA", {style:"currency",currency:"ZAR"}).format(Number(value || 0));
    const esc = value => String(value ?? "").replace(/[&<>"']/g, character => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"})[character]);
    const element = id => document.getElementById(id);
    let state = null;
    let busy = false;
    let reconnectDelay = 1000;

    function showError(message) {
        const box = element("driverError");
        box.textContent = message;
        box.classList.toggle("d-none", !message);
    }

    function setBusy(value) {
        busy = value;
        document.querySelectorAll('.driver-actions button, .driver-orders button[data-action]').forEach(button => {
            button.disabled = value || (button.dataset.action === 'pickup' && button.dataset.pickupAllowed !== 'true');
        });
    }

    async function request(path, method = "GET") {
        const response = await fetch(path, {method, headers: method === "GET" ? {} : {"X-CSRF-TOKEN": csrf}});
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
        return data;
    }

    async function action(path) {
        if (busy) return;
        setBusy(true);
        showError("");
        try { await request(path, "POST"); await refresh(); }
        catch (error) { showError(error.message); }
        finally { setBusy(false); }
    }

    function orderCard(delivery) {
        const order = delivery.order;
        const address = order.formattedAddress || [order.houseNumber,order.street,order.area,order.city,order.postalCode].filter(Boolean).join(", ");
        const lines = [...(order.menuItems || []),...(order.pizzaItems || []),...(order.specialItems || [])]
            .map(item => `<li>${esc(item.quantity || 1)} × ${esc(item.name || item.pizzaName || "Item")}</li>`).join("");
        const pickupAllowed = ["Pending", "Preparing", "Completed"].includes(order.status);
        const button = delivery.state === "ASSIGNED" ? `<button type="button" class="btn btn-primary" data-action="pickup" data-order="${order.id}" data-pickup-allowed="${pickupAllowed}" ${pickupAllowed ? "" : "disabled"}><i class="bi bi-bag-check me-1"></i>${pickupAllowed ? "Confirm pickup" : "Pickup unavailable"}</button>`
            : delivery.state === "PICKED_UP" ? `<button type="button" class="btn btn-primary" data-action="delivered" data-order="${order.id}"><i class="bi bi-check2-circle me-1"></i>Mark delivered</button>` : "";
        const phone = String(order.phone || "").replace(/[^+0-9]/g, "");
        return `<article class="driver-order"><div class="driver-order-head"><h3>Order #${esc(order.id)}</h3><span class="badge ${delivery.state === "DELIVERED" ? "text-bg-success" : "text-bg-primary"}">${esc(delivery.state.replaceAll("_"," "))}</span></div><p><strong>${esc(order.customerName)}</strong>${phone ? ` · <a href="tel:${esc(phone)}">${esc(order.phone)}</a>` : ""}</p><p>${esc(address)}</p>${order.complexName ? `<p>Unit: ${esc(order.complexName)}</p>` : ""}${order.gateAccessCode ? `<p>Gate code: ${esc(order.gateAccessCode)}</p>` : ""}${order.notes ? `<p>Notes: ${esc(order.notes)}</p>` : ""}<details><summary>Order contents · ${money(delivery.total)}</summary><ul>${lines}</ul></details>${button}</article>`;
    }

    function render(data) {
        state = data;
        const shift = data.shift;
        const deliveries = data.deliveries || [];
        const branchName = data.profile.branchId === 1 ? "Kenridge" : data.profile.branchId === 2 ? "Uitzicht" : `Branch ${data.profile.branchId}`;
        element("driverBranch").textContent = `${branchName} · ${data.profile.firstName} ${data.profile.lastName}`;
        element("driverShiftState").textContent = shift ? shift.state === "ACTIVE" ? "On shift" : "Clocked out · awaiting cash-up" : "Off shift";
        element("driverShiftMeta").textContent = shift ? `${shift.businessDate} · Started ${new Date(shift.clockedInAt).toLocaleTimeString("en-ZA",{hour:"2-digit",minute:"2-digit"})}${shift.returningAt ? " · Returning" : ""}` : "Clock in to start accepting deliveries.";
        element("driverClockIn").classList.toggle("d-none", !!shift);
        element("driverClockOut").classList.toggle("d-none", !shift || shift.state !== "ACTIVE");
        element("driverClaim").classList.toggle("d-none", !shift || shift.state !== "ACTIVE");
        element("driverReturning").classList.toggle("d-none", !shift || shift.state !== "ACTIVE" || !!shift.returningAt || deliveries.some(item => ["ASSIGNED","PICKED_UP"].includes(item.state)));
        const delivered = deliveries.filter(item => item.state === "DELIVERED");
        element("driverDelivered").textContent = String(delivered.length);
        element("driverSales").textContent = money(delivered.reduce((sum,item) => sum + Number(item.total || 0),0));
        const minutes = shift ? Math.max(0,(new Date(shift.clockedOutAt || Date.now()) - new Date(shift.clockedInAt))/60000) : 0;
        element("driverPay").textContent = money((shift ? Number(shift.hourlyRate)*minutes/60 + Number(shift.perDeliveryRate)*delivered.length : 0));
        element("driverOrders").innerHTML = deliveries.length ? deliveries.map(orderCard).join("") : '<div class="driver-empty">No deliveries assigned yet.</div>';
        if (busy) setBusy(true);
        element("driverSettlements").innerHTML = data.settlements?.length ? data.settlements.map(item => `<div class="driver-order"><strong>${esc(new Date(item.settledAt).toLocaleDateString("en-ZA"))}</strong><p>Sales ${money(item.orderSales)} · Pay ${money(Number(item.hourlyPay) + Number(item.deliveryPay))} · Cash due ${money(item.expectedCashDue)}</p>${Number(item.variance) ? `<p>Variance ${money(item.variance)} · ${esc(item.varianceReason)}</p>` : ""}</div>`).join("") : '<div class="driver-empty">No completed cash-ups.</div>';
    }

    async function refresh() {
        try { render(await request("/api/driver")); showError(""); }
        catch (error) { showError(error.message); }
    }

    function connect() {
        if (typeof createSimpleStompClient !== "function") return;
        const client = createSimpleStompClient({
            onConnect: () => {reconnectDelay=1000;element("driverConnection").textContent="Live";client.subscribe("/user/queue/driver",refresh);refresh();},
            onDisconnect: () => {element("driverConnection").textContent="Reconnecting";setTimeout(connect,reconnectDelay);reconnectDelay=Math.min(reconnectDelay*2,15000);}
        });
        client.connect();
    }

    document.addEventListener("DOMContentLoaded", () => {
        element("driverClockIn").addEventListener("click", () => action("/api/driver/clock-in"));
        element("driverClockOut").addEventListener("click", () => action("/api/driver/clock-out"));
        element("driverClaim").addEventListener("click", () => action("/api/driver/claim"));
        element("driverReturning").addEventListener("click", () => action("/api/driver/returning"));
        element("driverOrders").addEventListener("click", event => {
            const button = event.target.closest("button[data-action]");
            if (button?.dataset.action === "pickup" && !window.confirm(`Have you collected order #${button.dataset.order}?`)) return;
            if (button) action(`/api/driver/orders/${button.dataset.order}/${button.dataset.action}`);
        });
        refresh();connect();setInterval(refresh,15000);
    });
})();
