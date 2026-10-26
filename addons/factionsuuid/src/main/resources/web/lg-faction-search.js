// Faction search for squaremap, installed by squaremap-factionsuuid.
// Type a faction name to outline every claim and raid claim it has, with a clickable list per world.
// Reads tiles/<world>/faction-claims.json, which the addon rewrites on every claim update.
// Works on squaremap's own page and on a LiveAtlas page that loads this file from squaremap's web folder.
(() => {
    "use strict";

    const STORAGE_KEY = "lg-faction-search";
    const REFRESH_MS = 30000;
    const HIGHLIGHT = "#ffe14d";
    const RAID = "#ff8800";
    const SVG_NS = "http://www.w3.org/2000/svg";
    // tiles/ sits next to this script in squaremap's web folder, whichever page loaded it
    const BASE = new URL(".", document.currentScript.src).href;
    const LIVE_ATLAS = !!window.liveAtlasConfig;

    let map = null;
    let svg = null;
    let worlds = []; // [{name, display, scale, zoom}]
    let factions = new Map(); // id -> {id, tag, color, worlds: {worldName: {claims, raid}}}
    let lastFetch = 0;
    let selected = null; // faction id
    let shownWorld = null;
    let shapes = []; // what's highlighted in the shown world, in map coordinates
    let ui = null;

    if (LIVE_ATLAS) {
        // LiveAtlas bundles its own Leaflet without a window.L, but its Map component keeps the map in its setup state
        setInterval(() => {
            const found = liveAtlasMap();
            if (found && found !== map) {
                attach(found);
            }
        }, 500);
    } else {
        // squaremap keeps its Leaflet map inside a module, but Leaflet 1.x always publishes itself as window.L,
        // so catch that assignment and register an init hook before squaremap creates the map
        const onMap = (m) => {
            if (!map) {
                attach(m);
            }
        };
        const grabOnNextEvent = (L) => {
            const fire = L.Map.prototype.fire;
            L.Map.prototype.fire = function (...args) {
                onMap(this);
                return fire.apply(this, args);
            };
        };
        if (window.L && window.L.Map) {
            grabOnNextEvent(window.L); // loaded after squaremap, the map already exists
        } else {
            let leaflet;
            Object.defineProperty(window, "L", {
                configurable: true,
                enumerable: true,
                get: () => leaflet,
                set: (value) => {
                    leaflet = value;
                    if (value && value.Map && !value.Map.__lgFactionSearch) {
                        value.Map.__lgFactionSearch = true;
                        value.Map.addInitHook(function () {
                            onMap(this);
                        });
                    }
                },
            });
        }
    }

    function liveAtlasStore() {
        const app = document.getElementById("app");
        return app && app.__vue_app__ && app.__vue_app__.config.globalProperties.$store;
    }

    function liveAtlasMap() {
        const app = document.getElementById("app");
        const seen = new Set();
        const stack = [app && app._vnode];
        while (stack.length) {
            const vnode = stack.pop();
            if (!vnode || typeof vnode !== "object") {
                continue;
            }
            const component = vnode.component;
            if (component && !seen.has(component)) {
                seen.add(component);
                const leaflet = component.setupState && component.setupState.leaflet;
                if (leaflet && typeof leaflet.latLngToLayerPoint === "function") {
                    return leaflet;
                }
                stack.push(component.subTree);
            }
            if (Array.isArray(vnode.children)) {
                stack.push(...vnode.children);
            }
        }
        return null;
    }

    // LiveAtlas can switch between several servers; only show the search on the one this script came from
    function active() {
        if (!LIVE_ATLAS) {
            return true;
        }
        const store = liveAtlasStore();
        const server = store && store.state.currentServer;
        if (!server || !server.squaremap) {
            return false;
        }
        return new URL(server.squaremap.replace(/\/?$/, "/"), location.href).pathname === new URL(BASE).pathname;
    }

    const fetchJson = (url) =>
        fetch(url, { cache: "no-store" })
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null);

    start();

    async function start() {
        const settings = await fetchJson(BASE + "tiles/settings.json");
        if (!settings) {
            return;
        }
        worlds = await Promise.all(
            settings.worlds
                .slice()
                .sort((a, b) => a.order - b.order)
                .map(async (w) => {
                    const ws = await fetchJson(`${BASE}tiles/${w.name}/settings.json`);
                    const max = ws ? ws.zoom.max : 3;
                    return { name: w.name, display: w.display_name || w.name, scale: 1 / Math.pow(2, max), zoom: max };
                }),
        );
        buildUi();
        if (map) {
            attach(map);
        }
        await refresh(true);
        let restore = null;
        try {
            restore = sessionStorage.getItem(STORAGE_KEY);
            sessionStorage.removeItem(STORAGE_KEY);
        } catch (e) {
            // storage blocked, nothing to restore
        }
        if (restore && factions.has(restore)) {
            select(restore, false);
        }
        setInterval(() => {
            const on = map && active();
            ui.root.hidden = !on;
            // LiveAtlas re-adds its own controls when the server changes; keep the panel below them
            if (ui.root.nextElementSibling && !ui.root.contains(document.activeElement)) {
                ui.root.parentElement.append(ui.root);
            }
            if (!on) {
                if (shownWorld) {
                    shownWorld = null;
                    shapes = [];
                    render();
                }
            } else if (selected && currentWorld() !== shownWorld) {
                draw(false);
            }
        }, 1000);
        setInterval(() => {
            if (selected && document.visibilityState === "visible") {
                refresh(false).then(() => (factions.has(selected) ? draw(false) : clear()));
            }
        }, REFRESH_MS);
    }

    // hooks the panel and the highlight layer onto a map; LiveAtlas may replace its map, so this can run again
    function attach(m) {
        map = m;
        if (!ui) {
            return; // start() attaches once the panel exists
        }
        // the highlight sits above the claims; let clicks and hovers through to them
        const pane = map.createPane("lgFactionSearch");
        pane.style.zIndex = 650;
        pane.style.pointerEvents = "none";
        // drawn in layer coordinates, which only change on zoom, so it pans with the map for free
        svg = document.createElementNS(SVG_NS, "svg");
        svg.setAttribute("class", "leaflet-zoom-hide");
        svg.setAttribute("width", "1");
        svg.setAttribute("height", "1");
        svg.style.cssText = "position:absolute;left:0;top:0;overflow:visible";
        pane.append(svg);
        map.on("zoomend viewreset", render);

        const corner = map._controlCorners && map._controlCorners.topleft;
        (corner || map.getContainer()).append(ui.root);
        ui.root.hidden = !active();
        shownWorld = null;
        render();
    }

    async function refresh(force) {
        if (!force && Date.now() - lastFetch < REFRESH_MS / 2) {
            return;
        }
        lastFetch = Date.now();
        const results = await Promise.all(worlds.map((w) => fetchJson(`${BASE}tiles/${w.name}/faction-claims.json`)));
        const next = new Map();
        results.forEach((json, i) => {
            if (!json) {
                return;
            }
            for (const f of json.factions) {
                let entry = next.get(f.id);
                if (!entry) {
                    entry = { id: f.id, tag: f.tag, color: f.color, worlds: {}, chunks: 0 };
                    next.set(f.id, entry);
                }
                entry.worlds[worlds[i].name] = { claims: f.claims, raid: f.raid };
                entry.chunks += f.claims.length + f.raid.length;
            }
        });
        factions = next;
    }

    function currentWorld() {
        if (LIVE_ATLAS) {
            const store = liveAtlasStore();
            return store && store.state.currentWorld ? store.state.currentWorld.name : null;
        }
        let name = null;
        map.eachLayer((layer) => {
            const m = typeof layer._url === "string" && /tiles\/([^/]+)\//.exec(layer._url);
            if (m) {
                name = m[1];
            }
        });
        if (!name) {
            name = new URLSearchParams(location.search).get("world");
        }
        return name || (worlds[0] && worlds[0].name);
    }

    // splits chunks into groups that touch edge to edge, the same way the addon merges claim outlines
    function groups(chunks) {
        const key = (c) => `${c[0]},${c[1]}`;
        const left = new Map(chunks.map((c) => [key(c), c]));
        const out = [];
        for (const start of chunks) {
            if (!left.has(key(start))) {
                continue;
            }
            left.delete(key(start));
            const group = [start];
            for (let i = 0; i < group.length; i++) {
                const [x, z] = group[i];
                for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
                    const k = `${x + dx},${z + dz}`;
                    if (left.has(k)) {
                        group.push(left.get(k));
                        left.delete(k);
                    }
                }
            }
            out.push(group);
        }
        return out;
    }

    function areas(faction, worldName) {
        const data = faction.worlds[worldName];
        if (!data) {
            return [];
        }
        const result = [];
        for (const [list, raid] of [[data.claims, false], [data.raid, true]]) {
            for (const group of groups(list)) {
                const xs = group.map((c) => c[0]);
                const zs = group.map((c) => c[1]);
                result.push({
                    raid,
                    chunks: group,
                    expires: raid ? Math.min(...group.map((c) => c[2])) : 0,
                    // block coords of the middle of the group
                    x: Math.round(((Math.min(...xs) + Math.max(...xs) + 1) * 16) / 2),
                    z: Math.round(((Math.min(...zs) + Math.max(...zs) + 1) * 16) / 2),
                });
            }
        }
        // raid claims first, then biggest areas
        return result.sort((a, b) => b.raid - a.raid || b.chunks.length - a.chunks.length);
    }

    // squaremap and LiveAtlas both map block x/z to Leaflet's simple CRS this way
    function latLng(world, x, z) {
        return [-z * world.scale, x * world.scale];
    }

    function timeLeft(expires) {
        if (expires < 0) {
            return "raid claim";
        }
        const minutes = Math.max(0, Math.round((expires - Date.now()) / 60000));
        return `raid claim, ${minutes >= 60 ? `${Math.floor(minutes / 60)}h ${minutes % 60}m` : `${minutes}m`} left`;
    }

    function select(id, fit = true) {
        selected = id;
        const faction = factions.get(id);
        ui.input.value = faction ? faction.tag : "";
        ui.suggestions.replaceChildren();
        ui.clear.hidden = false;
        draw(fit);
    }

    function clear() {
        selected = null;
        shownWorld = null;
        ui.input.value = "";
        ui.clear.hidden = true;
        ui.details.replaceChildren();
        ui.suggestions.replaceChildren();
        shapes = [];
        render();
    }

    function draw(fit) {
        const faction = factions.get(selected);
        shapes = [];
        if (!faction || !map) {
            render();
            return;
        }
        const worldName = currentWorld();
        const world = worlds.find((w) => w.name === worldName);
        shownWorld = worldName;

        if (world) {
            const bounds = [];
            for (const area of areas(faction, worldName)) {
                const colour = area.raid ? RAID : HIGHLIGHT;
                for (const [cx, cz] of area.chunks) {
                    const corners = [latLng(world, cx * 16, cz * 16), latLng(world, cx * 16 + 16, cz * 16 + 16)];
                    shapes.push({ colour, corners });
                    bounds.push(...corners);
                }
                // a fixed-size ring so even a single chunk stands out when zoomed right out
                shapes.push({ colour, ring: latLng(world, area.x, area.z) });
            }
            if (fit && bounds.length) {
                // keep claims out from under the search panel on the left
                const panel = Math.min(ui.root.offsetWidth + 30, map.getSize().x / 2);
                map.fitBounds(bounds, {
                    paddingTopLeft: [panel, 40],
                    paddingBottomRight: [40, 40],
                    maxZoom: world.zoom,
                });
            }
        }
        render();
        renderDetails(faction, worldName);
    }

    function render() {
        if (!svg) {
            return;
        }
        const node = (tag, attrs) => {
            const e = document.createElementNS(SVG_NS, tag);
            for (const [k, v] of Object.entries(attrs)) {
                e.setAttribute(k, v);
            }
            return e;
        };
        const nodes = [];
        // chunks first so the rings stay on top
        for (const s of shapes) {
            if (s.corners) {
                const a = map.latLngToLayerPoint(s.corners[0]);
                const b = map.latLngToLayerPoint(s.corners[1]);
                nodes.push(
                    node("rect", {
                        x: Math.min(a.x, b.x),
                        y: Math.min(a.y, b.y),
                        width: Math.abs(b.x - a.x),
                        height: Math.abs(b.y - a.y),
                        fill: s.colour,
                        "fill-opacity": 0.45,
                        stroke: s.colour,
                        "stroke-width": 1,
                    }),
                );
            }
        }
        for (const s of shapes) {
            if (s.ring) {
                const p = map.latLngToLayerPoint(s.ring);
                nodes.push(
                    node("circle", { cx: p.x, cy: p.y, r: 9, fill: s.colour, "fill-opacity": 0.15, stroke: s.colour, "stroke-width": 3 }),
                );
            }
        }
        svg.replaceChildren(...nodes);
    }

    function renderDetails(faction, currentWorldName) {
        const el = (tag, cls, text) => {
            const e = document.createElement(tag);
            if (cls) {
                e.className = cls;
            }
            if (text !== undefined) {
                e.textContent = text;
            }
            return e;
        };
        const parts = [];
        const title = el("div", "lgfs-title");
        const swatch = el("span", "lgfs-swatch");
        swatch.style.background = faction.color;
        title.append(swatch, el("span", null, faction.tag));
        parts.push(title);

        let any = false;
        for (const world of worlds) {
            const list = areas(faction, world.name);
            if (!list.length) {
                continue;
            }
            any = true;
            const chunks = list.reduce((n, a) => n + a.chunks.length, 0);
            const head = el(
                "div",
                "lgfs-world" + (world.name === currentWorldName ? " lgfs-current" : ""),
                `${world.display}: ${list.length} area${list.length === 1 ? "" : "s"}, ${chunks} chunk${chunks === 1 ? "" : "s"}`,
            );
            parts.push(head);
            const ul = el("ul", "lgfs-areas");
            for (const area of list) {
                const li = el("li", area.raid ? "lgfs-raid" : null);
                li.append(
                    el("span", "lgfs-coords", `${area.x}, ${area.z}`),
                    el(
                        "span",
                        "lgfs-size",
                        area.raid ? timeLeft(area.expires) : `${area.chunks.length} chunk${area.chunks.length === 1 ? "" : "s"}`,
                    ),
                );
                li.title = world.name === currentWorldName ? "Zoom to this area" : `Open ${world.display} at this area`;
                li.onclick = () => goTo(world, area);
                ul.append(li);
            }
            parts.push(ul);
        }
        if (!any) {
            parts.push(el("div", "lgfs-empty", "No claims"));
        }
        ui.details.replaceChildren(...parts);
    }

    function goTo(world, area) {
        if (world.name === currentWorld()) {
            map.setView(latLng(world, area.x, area.z), world.zoom);
        } else if (LIVE_ATLAS) {
            // LiveAtlas follows its #world;map;x,y,z;zoom hash, and the selection survives since the page stays loaded
            location.hash = `#${world.name};flat;${area.x},64,${area.z};${world.zoom}`;
        } else {
            try {
                sessionStorage.setItem(STORAGE_KEY, selected);
            } catch (e) {
                // the selection just won't survive the reload
            }
            location.search = `?world=${encodeURIComponent(world.name)}&zoom=${world.zoom}&x=${area.x}&z=${area.z}`;
            return;
        }
        // on a phone the list covers most of the map; tapping the search box brings it back
        if (window.innerWidth < 600) {
            ui.root.classList.add("lgfs-collapsed");
        }
    }

    function suggest() {
        const query = ui.input.value.trim().toLowerCase();
        ui.suggestions.replaceChildren();
        if (!query) {
            return [];
        }
        const matches = [...factions.values()]
            .map((f) => ({ f, at: f.tag.toLowerCase().indexOf(query) }))
            .filter((m) => m.at >= 0)
            // names starting with the query first, then the biggest factions
            .sort((a, b) => (b.at === 0) - (a.at === 0) || b.f.chunks - a.f.chunks)
            .slice(0, 8)
            .map((m) => m.f);
        for (const f of matches) {
            const li = document.createElement("li");
            const swatch = document.createElement("span");
            swatch.className = "lgfs-swatch";
            swatch.style.background = f.color;
            const name = document.createElement("span");
            name.textContent = f.tag;
            const count = document.createElement("span");
            count.className = "lgfs-size";
            count.textContent = `${f.chunks}`;
            li.append(swatch, name, count);
            li.onmousedown = (e) => {
                e.preventDefault();
                select(f.id);
            };
            ui.suggestions.append(li);
        }
        if (!matches.length) {
            const li = document.createElement("li");
            li.className = "lgfs-empty";
            li.textContent = "No faction found";
            ui.suggestions.append(li);
        }
        return matches;
    }

    function buildUi() {
        const style = document.createElement("style");
        style.textContent = `
            .lgfs { --lgfs-bg: #fff; --lgfs-text: #222; --lgfs-muted: #777; --lgfs-heading: #555; --lgfs-line: #ddd; --lgfs-hover: #f0f0f0; --lgfs-raid-text: #c45f00;
                background: var(--lgfs-bg); color: var(--lgfs-text); border-radius: 4px; box-shadow: 0 1px 5px rgba(0,0,0,.65); width: 250px; font: 13px/1.4 "Helvetica Neue", Arial, sans-serif; }
            .lgfs.lgfs-liveatlas { --lgfs-bg: var(--background-base, #222); --lgfs-text: var(--text-emphasis, #eee); --lgfs-muted: var(--text-subtle, #999); --lgfs-heading: var(--text-base, #bbb);
                --lgfs-line: var(--border-color, #333); --lgfs-hover: var(--background-hover, #363636); --lgfs-raid-text: #ffa040;
                border-radius: var(--border-radius, 4px); box-shadow: var(--box-shadow, 0 1px 5px rgba(0,0,0,.65)); font-size: 14px; }
            .lgfs[hidden] { display: none; }
            .lgfs-bar { display: flex; align-items: center; }
            .lgfs input { flex: 1; min-width: 0; border: 0; background: transparent; padding: 8px 10px; font: inherit; color: inherit; outline: none; }
            .lgfs input::placeholder { color: var(--lgfs-muted); }
            .lgfs button { border: 0; background: transparent; font-size: 18px; line-height: 1; padding: 4px 10px; cursor: pointer; color: var(--lgfs-muted); }
            .lgfs button[hidden] { display: none; }
            .lgfs ul { display: block; list-style: none; margin: 0; padding: 0; }
            .lgfs-suggestions li, .lgfs-areas li { display: flex; align-items: center; gap: 8px; padding: 5px 10px; cursor: pointer; }
            .lgfs-suggestions li:hover, .lgfs-areas li:hover { background: var(--lgfs-hover); }
            .lgfs-suggestions:not(:empty), .lgfs-details:not(:empty) { border-top: 1px solid var(--lgfs-line); }
            .lgfs-details { max-height: 50vh; overflow-y: auto; padding-bottom: 4px; }
            .lgfs-title { display: flex; align-items: center; gap: 8px; padding: 8px 10px 2px; font-weight: bold; }
            .lgfs-world { padding: 6px 10px 2px; font-weight: bold; color: var(--lgfs-heading); font-size: 12px; }
            .lgfs-world.lgfs-current { color: var(--lgfs-text); }
            .lgfs-swatch { width: 10px; height: 10px; border-radius: 2px; flex: none; box-shadow: inset 0 0 0 1px rgba(0,0,0,.25); }
            .lgfs-coords { font-variant-numeric: tabular-nums; }
            .lgfs-size { margin-left: auto; color: var(--lgfs-muted); font-size: 12px; white-space: nowrap; }
            .lgfs-raid .lgfs-size { color: var(--lgfs-raid-text); font-weight: bold; }
            .lgfs-raid { box-shadow: inset 3px 0 0 ${RAID}; }
            .lgfs-empty { color: var(--lgfs-muted); padding: 6px 10px; cursor: default !important; }
            .lgfs-collapsed .lgfs-details { display: none; }
            @media (max-width: 599px) {
                .lgfs { width: min(250px, calc(100vw - 70px)); }
                .lgfs-details { max-height: 35vh; }
            }
        `;
        document.head.append(style);

        const root = document.createElement("div");
        root.className = "lgfs leaflet-control" + (LIVE_ATLAS ? " lgfs-liveatlas" : "");
        root.hidden = true;
        root.innerHTML =
            '<div class="lgfs-bar"><input type="text" placeholder="Find faction" autocomplete="off" spellcheck="false">' +
            '<button type="button" title="Clear" hidden>&times;</button></div><ul class="lgfs-suggestions"></ul><div class="lgfs-details"></div>';
        // keep clicks, drags, scrolling and typing in the panel away from the map and the page's shortcuts
        root._leaflet_disable_click = true;
        for (const type of ["mousedown", "pointerdown", "touchstart", "click", "dblclick", "contextmenu", "wheel", "keydown"]) {
            root.addEventListener(type, (e) => e.stopPropagation());
        }
        ui = {
            root,
            input: root.querySelector("input"),
            clear: root.querySelector("button"),
            suggestions: root.querySelector(".lgfs-suggestions"),
            details: root.querySelector(".lgfs-details"),
        };
        ui.input.addEventListener("focus", () => {
            root.classList.remove("lgfs-collapsed");
            // the refresh can finish after a faction was picked; don't bring the suggestions back then
            refresh(false).then(() => document.activeElement === ui.input && suggest());
        });
        ui.input.addEventListener("input", suggest);
        ui.input.addEventListener("blur", () => ui.suggestions.replaceChildren());
        ui.input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
                const first = suggest()[0];
                if (first) {
                    select(first.id);
                    ui.input.blur();
                }
            } else if (e.key === "Escape") {
                ui.suggestions.replaceChildren();
                ui.input.blur();
            }
        });
        ui.clear.onclick = clear;
    }
})();
