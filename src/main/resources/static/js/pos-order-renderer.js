/* Order values are untrusted, including values received over WebSocket. */
const PosOrderRenderer = (() => {
    const statuses = new Set(['Pending', 'Preparing', 'Completed', 'Rejected']);

    function element(tag, text, className) {
        const node = document.createElement(tag);
        if (text !== undefined) node.textContent = String(text);
        if (className) node.className = className;
        return node;
    }

    function statusOf(order) {
        if (!order.status || order.status === 'created') return 'Pending';
        return statuses.has(order.status) ? order.status : 'Unknown';
    }

    function address(order) {
        return [
            [order.houseNumber, order.street].filter(Boolean).join(' '),
            order.complexName,
            [order.area, order.city].filter(Boolean).join(', '),
            order.postalCode
        ].filter(Boolean).join(', ') || 'N/A';
    }

    function gateAccess(order) {
        if ((order.orderType || '').toLowerCase() !== 'delivery') return '';
        return String(order.gateAccessCode || '').trim();
    }

    function itemLines(order, withExtras) {
        const specials = (order.specialItems || []).map(item => {
            const selections = (item.selections || []).map(selection => {
                const size = selection.pizzaSizeCm ? ` (${selection.pizzaSizeCm}cm)` : '';
                return `${selection.label || 'Selection'}: ${selection.productName || 'Item'}${size}`;
            });
            return `${item.quantity || 1}x ${item.name || 'Special'}${withExtras && selections.length ? ` (${selections.join(', ')})` : ''}`;
        });
        const menu = (order.menuItems || []).map(item => {
            const extras = (item.extras || []).map(extra => extra.name).filter(Boolean);
            return `${item.qty}x ${item.menuItemName}${withExtras && extras.length ? ` (Extras: ${extras.join(', ')})` : ''}`;
        });
        const pizzas = (order.pizzaItems || []).map(item => {
            const extras = (item.extras || []).map(extra => extra.ingredientName).filter(Boolean);
            if (item.pizzaBaseOptionName) extras.unshift(item.pizzaBaseOptionName);
            for (const removed of (item.removedIngredients || [])) extras.push(`No ${removed}`);
            const size = item.pizzaSizeCm ? ` (${item.pizzaSizeCm}cm)` : '';
            const detail = withExtras ? (extras.length ? ` (Extras: ${extras.join(', ')})` : '')
                : (item.pizzaBaseOptionName ? `, ${item.pizzaBaseOptionName}` : '');
            return `${item.qty}x ${item.pizzaName}${size}${detail}`;
        });
        return [...specials, ...menu, ...pizzas];
    }

    function actions(order, onStatusChange, className) {
        const container = element('div', undefined, 'orderActions');
        const status = statusOf(order);
        if (status === 'Pending' || status === 'Preparing') {
            const button = element('button', status === 'Pending' ? 'Prepare' : 'Complete', className);
            button.type = 'button';
            button.addEventListener('click', () => onStatusChange(status === 'Pending' ? 'Preparing' : 'Completed'));
            container.appendChild(button);
        }
        return container;
    }

    function queueRow(order, {showBranch = false, onStatusChange}) {
        const row = element('li', undefined, 'orderItem');
        const main = element('div', undefined, 'orderMain');
        const branch = showBranch && order.branchName ? ` - ${order.branchName}` : '';
        main.appendChild(element('span', `Order #${order.id}${branch} - ${order.customerName || 'Guest'} (${order.orderType || 'N/A'})`));
        const status = statusOf(order);
        main.appendChild(element('span', status, `status ${status}`));
        const items = element('div', itemLines(order, false).join(', ') || 'No items');
        items.style.cssText = 'font-size:0.95rem;color:#003366;';
        const location = element('div', `Address: ${address(order)}`);
        location.style.cssText = 'font-size:0.9rem;color:#003366;';
        row.append(main, items, location);
        const accessCode = gateAccess(order);
        if (accessCode) {
            const access = element('div', `Gate access: ${accessCode}`);
            access.style.cssText = 'font-size:0.9rem;color:#003366;font-weight:700;';
            row.appendChild(access);
        }
        row.appendChild(actions(order, onStatusChange));
        return row;
    }

    function orderCard(order, {onStatusChange}) {
        const card = element('div', undefined, 'orderCard');
        const lines = [
            [`Order #${order.id}`, ''], ['Customer:', order.customerName || 'Guest'],
            ['Type:', order.orderType || 'N/A'], ['Address:', address(order)]
        ];
        const accessCode = gateAccess(order);
        if (accessCode) lines.push(['Gate access:', accessCode]);
        lines.push(['Items:', '']);
        for (const [label, value] of lines) {
            const line = element('p');
            line.append(element('strong', label), document.createTextNode(` ${value}`));
            card.appendChild(line);
        }
        const list = element('ul');
        const items = itemLines(order, true);
        for (const text of items.length ? items : ['No items']) list.appendChild(element('li', text));
        card.append(list, actions(order, onStatusChange, 'acceptBtn'));
        return card;
    }

    return {queueRow, orderCard};
})();
