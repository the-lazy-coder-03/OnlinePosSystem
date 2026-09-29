(() => {
    "use strict";

    const REGISTRY = new Map();
    let mapsLoader;

    function loadPlaces(apiKey) {
        if (window.google?.maps?.places?.PlaceAutocompleteElement) {
            return Promise.resolve(window.google.maps.places);
        }
        if (!apiKey) {
            return Promise.reject(new Error("Google Maps API key is not configured."));
        }
        if (!mapsLoader) {
            mapsLoader = new Promise((resolve, reject) => {
                const callback = `__petesMapsInit_${Date.now()}`;
                window[callback] = async () => {
                    try {
                        const places = await window.google.maps.importLibrary("places");
                        resolve(places);
                    } catch (error) {
                        reject(error);
                    } finally {
                        delete window[callback];
                    }
                };
                const script = document.createElement("script");
                script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&v=weekly&libraries=places&loading=async&callback=${callback}`;
                script.async = true;
                script.onerror = () => reject(new Error("Google address lookup could not be loaded."));
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
        element.value = nextValue || "";
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
        const initialCore = coreFingerprint();
        let currentAddress = readAddressFromFields();
        let isVerified = verified(currentAddress);
        let applyingSelection = false;

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
            isVerified = verified(address);
            applyingSelection = false;
            updateStatus(isVerified ? "Address verified." : "Google returned an incomplete address. Choose a more specific result.", isVerified ? "verified" : "invalid");
        }

        function requireValid() {
            const currentVerified = verified(readAddressFromFields());
            if (root.dataset.require === "true" && !currentVerified) {
                updateStatus("Select an address from Google before continuing.", "invalid");
                return false;
            }
            if (root.dataset.requireOnChange === "true" && changedSinceLoad() && !currentVerified) {
                updateStatus("Select an address from Google before saving address changes.", "invalid");
                return false;
            }
            return true;
        }

        Object.values(fields).forEach(input => {
            input?.addEventListener("input", () => {
                if (!applyingSelection && ["street", "area", "city", "postalCode", "houseNumber"].some(name => fields[name] === input)) {
                    clearGoogleMetadata("Address edited. Select a Google result to verify it again.");
                }
            });
        });

        const instance = {
            getAddress: () => readAddressFromFields(),
            isVerified: () => verified(readAddressFromFields()),
            hasChanged: changedSinceLoad,
            requireValid
        };
        REGISTRY.set(id, instance);

        loadPlaces(root.dataset.googleMapsKey || "")
            .then(places => {
                const autocomplete = new places.PlaceAutocompleteElement();
                autocomplete.includedRegionCodes = ["za"];
                autocomplete.requestedRegion = "za";
                autocomplete.requestedLanguage = "en";
                autocomplete.placeholder = root.dataset.placeholder || "Search for your address";
                autocomplete.addEventListener("input", () => {
                    if (!applyingSelection) clearGoogleMetadata("Select a Google result to verify the address.");
                });
                autocomplete.addEventListener("gmp-select", async event => {
                    try {
                        const prediction = event.placePrediction || event.detail?.placePrediction;
                        const place = prediction?.toPlace ? prediction.toPlace() : event.place || event.detail?.place;
                        if (!place?.fetchFields) throw new Error("No place details were returned.");
                        await place.fetchFields({ fields: ["id", "formattedAddress", "location", "addressComponents"] });
                        applyAddress(selectedAddress(place, value(fields.complexName)));
                    } catch (error) {
                        clearGoogleMetadata(error.message || "Address selection failed. Try another result.");
                    }
                });
                widget?.replaceChildren(autocomplete);
                updateStatus(isVerified ? "Address verified." : "", isVerified ? "verified" : "ready");
            })
            .catch(error => {
                updateStatus(error.message || "Google address lookup is unavailable.", "error");
            });

        return instance;
    }

    window.PetesAddressAutocomplete = {
        get(target) {
            const id = typeof target === "string" ? target : target?.id;
            return REGISTRY.get(id);
        }
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("[data-address-autocomplete]").forEach(createInstance);
    });
})();
