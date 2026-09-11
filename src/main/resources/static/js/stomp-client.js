(function () {
    function createFrame(command, headers, body) {
        const lines = [command];
        Object.entries(headers || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                lines.push(`${key}:${value}`);
            }
        });
        return `${lines.join("\n")}\n\n${body || ""}\0`;
    }

    function parseFrame(rawFrame) {
        const cleanFrame = rawFrame.replace(/\0+$/g, "");
        const separatorIndex = cleanFrame.indexOf("\n\n");
        const headerBlock = separatorIndex === -1 ? cleanFrame : cleanFrame.slice(0, separatorIndex);
        const body = separatorIndex === -1 ? "" : cleanFrame.slice(separatorIndex + 2);
        const lines = headerBlock.split("\n").filter(Boolean);
        const command = lines.shift() || "";
        const headers = {};

        lines.forEach(line => {
            const separator = line.indexOf(":");
            if (separator > -1) {
                headers[line.slice(0, separator)] = line.slice(separator + 1);
            }
        });

        return { command, headers, body };
    }

    function websocketUrl(path) {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        return `${protocol}//${window.location.host}${path}`;
    }

    window.createSimpleStompClient = function createSimpleStompClient(options) {
        const url = options.url || websocketUrl("/ws");
        let socket = null;
        let connected = false;
        let subscriptionCounter = 0;
        const subscriptions = new Map();

        function sendFrame(command, headers, body) {
            if (!socket || socket.readyState !== WebSocket.OPEN) {
                return false;
            }
            socket.send(createFrame(command, headers, body));
            return true;
        }

        function handleFrame(rawFrame) {
            if (!rawFrame || rawFrame === "\n") {
                return;
            }

            rawFrame.split("\0").filter(Boolean).forEach(part => {
                const frame = parseFrame(part);
                if (frame.command === "CONNECTED") {
                    connected = true;
                    if (typeof options.onConnect === "function") {
                        options.onConnect();
                    }
                    return;
                }

                if (frame.command === "MESSAGE") {
                    const subscription = subscriptions.get(frame.headers.subscription);
                    if (subscription) {
                        subscription(frame);
                    }
                    return;
                }

                if (frame.command === "ERROR" && typeof options.onError === "function") {
                    options.onError(frame);
                }
            });
        }

        return {
            connect() {
                socket = new WebSocket(url);
                socket.onopen = () => {
                    sendFrame("CONNECT", {
                        "accept-version": "1.2",
                        "heart-beat": "10000,10000"
                    });
                };
                socket.onmessage = event => handleFrame(String(event.data || ""));
                socket.onerror = event => {
                    if (typeof options.onError === "function") {
                        options.onError(event);
                    }
                };
                socket.onclose = event => {
                    connected = false;
                    subscriptions.clear();
                    if (typeof options.onDisconnect === "function") {
                        options.onDisconnect(event);
                    }
                };
            },
            disconnect() {
                if (socket && socket.readyState === WebSocket.OPEN) {
                    sendFrame("DISCONNECT", {});
                }
                if (socket) {
                    socket.close();
                }
            },
            subscribe(destination, callback) {
                if (!connected) {
                    return null;
                }
                const id = `sub-${++subscriptionCounter}`;
                subscriptions.set(id, callback);
                sendFrame("SUBSCRIBE", {
                    id,
                    destination
                });
                return id;
            },
            send(destination, body, headers) {
                return sendFrame("SEND", {
                    destination,
                    ...(headers || {})
                }, body);
            },
            isConnected() {
                return connected;
            }
        };
    };
})();
