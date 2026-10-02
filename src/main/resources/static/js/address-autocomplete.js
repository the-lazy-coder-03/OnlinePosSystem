(() => {
    "use strict";

    const REGISTRY = new Map();
    const MIN_QUERY_LENGTH = 2;
    const SEARCH_DEBOUNCE_MS = 220;
    const MAX_RESULTS = 5;
    const NETWORK_ERROR_MESSAGE = "Google address search is unavailable. Check your connection and try again.";
    const SERVICE_ERROR_MESSAGE = "Google address search is temporarily unavailable. Please try again later.";
    let mapsLoader;

    function searchErrorMessage(error) {
        const code = Number(error?.code);
        const message = String(error?.message || "");
        const isNetworkFailure = error instanceof TypeError
            && /failed to fetch|network|load failed|could not be loaded/i.test(message);
        if (isNetworkFailure) return NETWORK_ERROR_MESSAGE;
        if ([3, 7, 8].includes(code)
            || /api key|referer|permission|denied|billing|quota|not configured/i.test(message)) {
            return SERVICE_ERROR_MESSAGE;
        }
        return SERVICE_ERROR_MESSAGE;
    }

    function validPlacesLibrary(places) {
        return Boolean(places?.AutocompleteSuggestion?.fetchAutocompleteSuggestions
            && places?.AutocompleteSessionToken);
    }

    function importPlaces() {
        return Promise.resolve().then(() => window.google.maps.importLibrary("places")).then(places => {
            if (!validPlacesLibrary(places)) {
                throw new Error("Google Places autocomplete is unavailable.");
            }
            return places;
        });
    }

    function loadPlaces(apiKey) {
        if (window.google?.maps?.importLibrary) {
            return importPlaces();
        }
        if (!apiKey) {
            return Promise.reject(new Error("Google Maps API key is not configured."));
        }
        if (!mapsLoader) {
            mapsLoader = new Promise((resolve, reject) => {
                const callback = `__petesMapsInit_${Date.now()}`;
                const script = document.createElement("script");
                const fail = () => {
                    delete window[callback];
                    reject(new TypeError("Google address search could not be loaded."));
                };

                window[callback] = () => {
                    importPlaces().then(resolve, reject).finally(() => delete window[callback]);
                };
                script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&v=weekly&libraries=places&loading=async&callback=${callback}`;
                script.async = true;
                script.onerror = fail;
                document.head.appendChild(script);
            });
        }
        return mapsLoader;
    }

    function field(root, name) {
        const selector = root.dataset[name];
        return selector ? document.querySelector(selector) : null;
    }

    function value(element) {
        return element?.value?.trim() || "";
    }

    function setValue(element, nextValue) {
        if (!element) return;
        element.value = nextValue ?? "";
        element.dispatchEvent(new Event("input", { bubbles: true }));
    }

    function componentText(component) {
        return component?.longText || component?.long_name || component?.shortText || component?.short_name || "";
    }

    function findComponent(components, ...types) {
        return components.find(component => types.some(type => component.types?.includes(type)));
    }

    function numericLocation(location, axis) {
        if (!location) return "";
        const raw = axis === "lat" ? location.lat : location.lng;
        return typeof raw === "function" ? raw.call(location) : raw;
    }

    function selectedAddress(place, complexName) {
        const components = Array.isArray(place.addressComponents) ? place.addressComponents : [];
        const houseNumber = componentText(findComponent(components, "street_number"));
        const street = componentText(findComponent(components, "route"));
        const area = componentText(findComponent(
            components,
            "sublocality_level_1",
            "sublocality",
            "neighborhood",
            "administrative_area_level_3"
        ));
        const city = componentText(findComponent(
            components,
            "locality",
            "postal_town",
            "administrative_area_level_2"
        ));
        const postalCode = componentText(findComponent(components, "postal_code"));
        const province = componentText(findComponent(components, "administrative_area_level_1"));
        const country = componentText(findComponent(components, "country"));
        return {
            googlePlaceId: place.id || place.placeId || "",
            formattedAddress: place.formattedAddress || place.formatted_address || "",
            latitude: numericLocation(place.location, "lat"),
            longitude: numericLocation(place.location, "lng"),
            houseNumber,
            street,
            area,
            city,
            postalCode,
            complexName: complexName || "",
            province,
            country
        };
    }

    function verified(address) {
        return Boolean(address.googlePlaceId && address.formattedAddress && address.latitude !== "" && address.longitude !== ""
            && address.street && (address.area || address.city) && address.country);
    }

    function displayAddress(address) {
        if (address.formattedAddress) return address.formattedAddress;
        return [
            [address.houseNumber, address.street].filter(Boolean).join(" "),
            address.area,
            address.city,
            address.postalCode
        ].filter(Boolean).join(", ");
    }

    function predictionText(prediction) {
        return prediction?.text?.toString?.() || prediction?.text || prediction?.mainText?.toString?.()
            || prediction?.mainText || "Address result";
    }

    function createSearchUi(root, widget, initialValue) {
        const searchId = `${root.id}-search`;
        const listId = `${root.id}-results`;
        const control = document.createElement("div");
        const input = document.createElement("input");
        const list = document.createElement("ul");
        const attribution = document.createElement("li");

        control.className = "address-search-control";
        input.id = searchId;
        input.className = "address-search-input";
        input.type = "search";
        input.placeholder = root.dataset.placeholder || "Search for your address";
        input.autocomplete = "off";
        input.spellcheck = false;
        input.disabled = true;
        input.value = initialValue || "";
        input.setAttribute("role", "combobox");
        input.setAttribute("aria-autocomplete", "list");
        input.setAttribute("aria-haspopup", "listbox");
        input.setAttribute("aria-controls", listId);
        input.setAttribute("aria-expanded", "false");

        list.id = listId;
        list.className = "address-results";
        list.setAttribute("role", "listbox");
        list.hidden = true;

        attribution.className = "address-attribution";
        attribution.setAttribute("role", "presentation");
        attribution.setAttribute("translate", "no");
        attribution.textContent = "Google Maps";

        control.append(input, list);
        widget?.replaceChildren(control);
        const label = root.querySelector("label");
        if (label) label.htmlFor = searchId;

        return { input, list, attribution };
    }

    function createInstance(root) {
        const id = root.id || `address-${REGISTRY.size + 1}`;
        root.id = id;
        const status = root.querySelector("[data-address-status]");
        const widget = root.querySelector("[data-address-widget]");
        const fields = {
            googlePlaceId: field(root, "fieldGooglePlaceId"),
            formattedAddress: field(root, "fieldFormattedAddress"),
            latitude: field(root, "fieldLatitude"),
            longitude: field(root, "fieldLongitude"),
            houseNumber: field(root, "fieldHouseNumber"),
            street: field(root, "fieldStreet"),
            area: field(root, "fieldArea"),
            city: field(root, "fieldCity"),
            postalCode: field(root, "fieldPostalCode"),
            complexName: field(root, "fieldComplexName"),
            province: field(root, "fieldProvince"),
            country: field(root, "fieldCountry")
        };
        let initialCore = coreFingerprint();
        let currentAddress = readAddressFromFields();
        let isVerified = verified(currentAddress);
        let applyingSelection = false;
        let placesLibrary;
        let sessionToken;
        let suggestions = [];
        let activeIndex = -1;
        let debounceTimer;
        let requestVersion = 0;
        const ui = createSearchUi(root, widget, displayAddress(currentAddress));

        if (status) {
            status.setAttribute("aria-live", "polite");
            status.setAttribute("aria-atomic", "true");
        }

        function readAddressFromFields() {
            return {
                googlePlaceId: value(field(root, "fieldGooglePlaceId")),
                formattedAddress: value(field(root, "fieldFormattedAddress")),
                latitude: value(field(root, "fieldLatitude")),
                longitude: value(field(root, "fieldLongitude")),
                houseNumber: value(field(root, "fieldHouseNumber")),
                street: value(field(root, "fieldStreet")),
                area: value(field(root, "fieldArea")),
                city: value(field(root, "fieldCity")),
                postalCode: value(field(root, "fieldPostalCode")),
                complexName: value(field(root, "fieldComplexName")),
                province: value(field(root, "fieldProvince")),
                country: value(field(root, "fieldCountry"))
            };
        }

        function coreFingerprint() {
            return ["fieldHouseNumber", "fieldStreet", "fieldArea", "fieldCity", "fieldPostalCode"]
                .map(name => value(field(root, name))).join("|");
        }

        function changedSinceLoad() {
            return coreFingerprint() !== initialCore;
        }

        function updateStatus(message, state) {
            root.dataset.addressState = state;
            if (status) {
                status.textContent = message || "";
                status.hidden = !message;
            }
            root.dispatchEvent(new CustomEvent("address:state", {
                bubbles: true,
                detail: { verified: isVerified, changed: changedSinceLoad(), address: currentAddress }
            }));
        }

        function closeResults() {
            suggestions = [];
            activeIndex = -1;
            ui.list.replaceChildren();
            ui.list.hidden = true;
            ui.input.setAttribute("aria-expanded", "false");
            ui.input.removeAttribute("aria-activedescendant");
        }

        function clearGoogleMetadata(message) {
            currentAddress = { ...readAddressFromFields(), googlePlaceId: "", formattedAddress: "", latitude: "", longitude: "", province: "", country: "" };
            ["googlePlaceId", "formattedAddress", "latitude", "longitude", "province", "country"].forEach(name => setValue(fields[name], ""));
            isVerified = false;
            updateStatus(message || "Select an address from Google to verify it.", "invalid");
        }

        function applyAddress(address) {
            applyingSelection = true;
            currentAddress = address;
            Object.entries(address).forEach(([name, nextValue]) => setValue(fields[name], nextValue));
            ui.input.value = address.formattedAddress || "";
            isVerified = verified(address);
            applyingSelection = false;
            updateStatus(isVerified ? "Address verified." : "Google returned an incomplete address. Choose a more specific result.", isVerified ? "verified" : "invalid");
        }

        function syncFromFields() {
            requestVersion += 1;
            clearTimeout(debounceTimer);
            closeResults();
            currentAddress = readAddressFromFields();
            isVerified = verified(currentAddress);
            initialCore = coreFingerprint();
            ui.input.value = displayAddress(currentAddress);
            const hasSavedAddress = Boolean(ui.input.value);
            updateStatus(
                isVerified
                    ? "Address verified."
                    : (hasSavedAddress
                        ? "Saved address loaded. Select a Google result to verify it."
                        : "Start typing to search for an address."),
                isVerified ? "verified" : "ready"
            );
            return currentAddress;
        }

        function requireValid() {
            const currentVerified = verified(readAddressFromFields());
            if (root.dataset.require === "true" && !currentVerified) {
                updateStatus("Select an address from Google before continuing.", "invalid");
                ui.input.focus();
                return false;
            }
            if (root.dataset.requireOnChange === "true" && changedSinceLoad() && !currentVerified) {
                updateStatus("Select an address from Google before saving address changes.", "invalid");
                ui.input.focus();
                return false;
            }
            return true;
        }

        function setActiveIndex(nextIndex) {
            const options = Array.from(ui.list.querySelectorAll("[role='option']"));
            if (!options.length) return;
            activeIndex = (nextIndex + options.length) % options.length;
            options.forEach((option, index) => option.setAttribute("aria-selected", String(index === activeIndex)));
            ui.input.setAttribute("aria-activedescendant", options[activeIndex].id);
            options[activeIndex].scrollIntoView({ block: "nearest" });
        }

        async function selectPrediction(index) {
            const prediction = suggestions[index];
            if (!prediction) return;
            const selectionVersion = ++requestVersion;
            clearTimeout(debounceTimer);
            closeResults();
            ui.input.setAttribute("aria-busy", "true");
            updateStatus("Loading address details…", "loading");
            try {
                const place = prediction.toPlace();
                await place.fetchFields({ fields: ["id", "formattedAddress", "location", "addressComponents"] });
                if (selectionVersion !== requestVersion) return;
                applyAddress(selectedAddress(place, value(fields.complexName)));
                sessionToken = new placesLibrary.AutocompleteSessionToken();
            } catch (_error) {
                if (selectionVersion === requestVersion) {
                    clearGoogleMetadata("Address details could not be loaded. Choose the result again or try another address.");
                }
            } finally {
                ui.input.removeAttribute("aria-busy");
            }
        }

        function renderSuggestions(nextSuggestions) {
            closeResults();
            suggestions = nextSuggestions.map(suggestion => suggestion.placePrediction).filter(Boolean).slice(0, MAX_RESULTS);

            if (!suggestions.length) {
                const empty = document.createElement("li");
                empty.className = "address-results-empty";
                empty.setAttribute("role", "presentation");
                empty.textContent = "No matching addresses found. Try adding a street number or suburb.";
                ui.list.append(empty, ui.attribution);
                ui.list.hidden = false;
                ui.input.setAttribute("aria-expanded", "true");
                updateStatus("No address suggestions found.", "ready");
                return;
            }

            suggestions.forEach((prediction, index) => {
                const option = document.createElement("li");
                option.id = `${id}-option-${index}`;
                option.className = "address-result";
                option.setAttribute("role", "option");
                option.setAttribute("aria-selected", "false");
                option.textContent = predictionText(prediction);
                option.addEventListener("pointerdown", event => event.preventDefault());
                option.addEventListener("click", () => selectPrediction(index));
                ui.list.append(option);
            });
            ui.list.append(ui.attribution);
            ui.list.hidden = false;
            ui.input.setAttribute("aria-expanded", "true");
            updateStatus(`${suggestions.length} address suggestion${suggestions.length === 1 ? "" : "s"} available.`, "ready");
        }

        async function search(query, version) {
            ui.input.setAttribute("aria-busy", "true");
            updateStatus("Searching for addresses…", "loading");
            try {
                const response = await placesLibrary.AutocompleteSuggestion.fetchAutocompleteSuggestions({
                    input: query,
                    includedRegionCodes: ["za"],
                    language: "en",
                    region: "za",
                    sessionToken
                });
                if (version !== requestVersion || value(ui.input) !== query) return;
                renderSuggestions(Array.isArray(response?.suggestions) ? response.suggestions : []);
            } catch (error) {
                if (version !== requestVersion) return;
                closeResults();
                updateStatus(searchErrorMessage(error), "error");
            } finally {
                if (version === requestVersion) ui.input.removeAttribute("aria-busy");
            }
        }

        function scheduleSearch() {
            clearTimeout(debounceTimer);
            ui.input.removeAttribute("aria-busy");
            const query = value(ui.input);
            const version = ++requestVersion;
            closeResults();
            if (!applyingSelection) clearGoogleMetadata("Select an address from Google to verify it.");
            if (query.length < MIN_QUERY_LENGTH) {
                updateStatus(query ? "Type at least 2 characters to search for an address." : "Start typing to search for an address.", "ready");
                return;
            }
            debounceTimer = window.setTimeout(() => search(query, version), SEARCH_DEBOUNCE_MS);
        }

        Object.values(fields).forEach(input => {
            input?.addEventListener("input", () => {
                if (!applyingSelection && ["street", "area", "city", "postalCode", "houseNumber"].some(name => fields[name] === input)) {
                    clearGoogleMetadata("Address edited. Select a Google result to verify it again.");
                }
            });
        });

        ui.input.addEventListener("input", scheduleSearch);
        ui.input.addEventListener("keydown", event => {
            if (event.key === "ArrowDown" && suggestions.length) {
                event.preventDefault();
                setActiveIndex(activeIndex + 1);
            } else if (event.key === "ArrowUp" && suggestions.length) {
                event.preventDefault();
                setActiveIndex(activeIndex < 0 ? suggestions.length - 1 : activeIndex - 1);
            } else if (event.key === "Enter" && activeIndex >= 0) {
                event.preventDefault();
                selectPrediction(activeIndex);
            } else if (event.key === "Escape") {
                requestVersion += 1;
                clearTimeout(debounceTimer);
                ui.input.removeAttribute("aria-busy");
                closeResults();
            } else if (event.key === "Tab") {
                closeResults();
            }
        });
        ui.input.addEventListener("blur", () => window.setTimeout(() => {
            if (document.activeElement !== ui.input) {
                requestVersion += 1;
                clearTimeout(debounceTimer);
                ui.input.removeAttribute("aria-busy");
                closeResults();
            }
        }, 100));

        const instance = {
            getAddress: () => readAddressFromFields(),
            isVerified: () => verified(readAddressFromFields()),
            hasChanged: changedSinceLoad,
            requireValid,
            syncFromFields
        };
        REGISTRY.set(id, instance);

        loadPlaces(root.dataset.googleMapsKey || "")
            .then(places => {
                placesLibrary = places;
                sessionToken = new places.AutocompleteSessionToken();
                ui.input.disabled = false;
                updateStatus(isVerified ? "Address verified." : "Start typing to search for an address.", isVerified ? "verified" : "ready");
            })
            .catch(error => {
                ui.input.disabled = true;
                updateStatus(searchErrorMessage(error), "error");
            });

        return instance;
    }

    function initialize() {
        document.querySelectorAll("[data-address-autocomplete]").forEach(root => {
            if (!REGISTRY.has(root.id)) createInstance(root);
        });
    }

    window.PetesAddressAutocomplete = {
        get(target) {
            const id = typeof target === "string" ? target : target?.id;
            return REGISTRY.get(id);
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initialize);
    } else {
        initialize();
    }
})();
