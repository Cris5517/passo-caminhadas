const api = "/api/walks";
const formatNumber = new Intl.NumberFormat("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const formatDate = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });
const formatShortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" });
const formatTime = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

const elements = {
	date: document.querySelector("#today-label"),
	gps: document.querySelector("#gps-status"),
	gpsText: document.querySelector("#gps-status span:last-child"),
	mapLabel: document.querySelector("#map-label"),
	sessionStatus: document.querySelector("#session-status"),
	sessionIndicator: document.querySelector("#session-indicator"),
	timer: document.querySelector("#timer"),
	distance: document.querySelector("#distance"),
	pace: document.querySelector("#pace"),
	actions: document.querySelector("#action-buttons"),
	feedback: document.querySelector("#feedback"),
	weekDistance: document.querySelector("#week-distance"),
	weekDistanceSide: document.querySelector("#week-distance-side"),
	weekProgress: document.querySelector("#week-progress"),
	walkCount: document.querySelector("#walk-count"),
	totalTime: document.querySelector("#total-time"),
	historyTotal: document.querySelector("#history-total"),
	history: document.querySelector("#history-list")
};

let map;
let routeLine;
let currentMarker;
let activeWalk = null;
let distanceMeters = 0;
let elapsedBeforeSegment = 0;
let segmentStartedAt = 0;
let lastPoint = null;
let watchId = null;
let timerId = null;
let savingProgress = false;
let lastSavedSecond = 0;

function formatDuration(totalSeconds) {
	const safeSeconds = Math.max(0, Math.floor(totalSeconds));
	const hours = String(Math.floor(safeSeconds / 3600)).padStart(2, "0");
	const minutes = String(Math.floor((safeSeconds % 3600) / 60)).padStart(2, "0");
	const seconds = String(safeSeconds % 60).padStart(2, "0");
	return `${hours}:${minutes}:${seconds}`;
}

function formatPace(secondsPerKm) {
	if (secondsPerKm == null || !Number.isFinite(secondsPerKm)) return "--:--";
	const roundedSeconds = Math.round(secondsPerKm);
	const minutes = String(Math.floor(roundedSeconds / 60)).padStart(2, "0");
	const seconds = String(roundedSeconds % 60).padStart(2, "0");
	return `${minutes}:${seconds}`;
}

function currentDuration() {
	return elapsedBeforeSegment + (segmentStartedAt ? Math.floor((Date.now() - segmentStartedAt) / 1000) : 0);
}

function setFeedback(message = "") {
	elements.feedback.textContent = message;
}

function setGpsStatus(state, message) {
	elements.gps.classList.toggle("is-found", state === "found");
	elements.gps.classList.toggle("is-error", state === "error");
	elements.gpsText.textContent = message;
}

function updateMetrics() {
	const distanceKm = distanceMeters / 1000;
	elements.timer.textContent = formatDuration(currentDuration());
	elements.distance.textContent = formatNumber.format(distanceKm);
	elements.pace.textContent = distanceMeters > 0 ? formatPace(currentDuration() * 1000 / distanceMeters) : "--:--";
}

function drawActions(status) {
	elements.actions.replaceChildren();
	elements.actions.classList.toggle("has-secondary", status === "ACTIVE" || status === "PAUSED");
	if (!status) {
		elements.sessionStatus.textContent = "PRONTO PARA SAIR";
		elements.sessionIndicator.className = "session-indicator";
		elements.actions.append(createButton("play", "Começar caminhada", "button-primary", startWalk));
	} else if (status === "ACTIVE") {
		elements.sessionStatus.textContent = "CAMINHADA EM ANDAMENTO";
		elements.sessionIndicator.className = "session-indicator is-active";
		elements.actions.append(createButton("pause", "Pausar", "button-secondary", pauseWalk));
		elements.actions.append(createButton("square", "Finalizar", "button-finish", finishWalk));
	} else if (status === "PAUSED") {
		elements.sessionStatus.textContent = "CAMINHADA PAUSADA";
		elements.sessionIndicator.className = "session-indicator is-paused";
		elements.actions.append(createButton("play", "Retomar", "button-primary", resumeWalk));
		elements.actions.append(createButton("square", "Finalizar", "button-finish", finishWalk));
	}
	if (window.lucide) window.lucide.createIcons();
}

function createButton(icon, label, className, action) {
	const button = document.createElement("button");
	button.type = "button";
	button.className = `button ${className}`;
	button.innerHTML = `<i data-lucide="${icon}"></i><span></span>`;
	button.querySelector("span").textContent = label;
	button.addEventListener("click", action);
	return button;
}

async function request(path = "", options = {}) {
	const response = await fetch(`${api}${path}`, {
		...options,
		headers: { "Content-Type": "application/json", ...options.headers }
	});
	if (response.status === 204) return null;
	const body = await response.json();
	if (!response.ok) throw new Error(body.message || "Não foi possível concluir a solicitação.");
	return body;
}

function haversineMeters(first, second) {
	const earthRadius = 6371000;
	const radians = (degrees) => degrees * Math.PI / 180;
	const latitudeDelta = radians(second.lat - first.lat);
	const longitudeDelta = radians(second.lng - first.lng);
	const a = Math.sin(latitudeDelta / 2) ** 2
		+ Math.cos(radians(first.lat)) * Math.cos(radians(second.lat)) * Math.sin(longitudeDelta / 2) ** 2;
	return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function initMap() {
	if (!window.L) {
		document.querySelector("#map").classList.add("map-unavailable");
		return;
	}
	map = L.map("map", { zoomControl: false, attributionControl: true }).setView([-14.2, -51.9], 4);
	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		maxZoom: 19,
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
	}).addTo(map);
	L.control.zoom({ position: "topright" }).addTo(map);
	routeLine = L.polyline([], { color: "#2c705a", weight: 5, opacity: .88, lineCap: "round", lineJoin: "round" }).addTo(map);
	document.querySelector("#center-map").addEventListener("click", centerMap);
}

function centerMap() {
	if (currentMarker && map) map.setView(currentMarker.getLatLng(), 16, { animate: true });
}

function startLocationWatch() {
	if (!navigator.geolocation) {
		setGpsStatus("error", "GPS indisponível");
		return;
	}
	setGpsStatus("waiting", "Buscando sinal GPS");
	watchId = navigator.geolocation.watchPosition(onLocation, onLocationError, {
		enableHighAccuracy: true,
		maximumAge: 2000,
		timeout: 20000
	});
}

function onLocation(position) {
	if (!activeWalk || activeWalk.status !== "ACTIVE") return;
	if (position.coords.accuracy > 70) {
		setGpsStatus("waiting", "Ajustando sinal GPS");
		return;
	}
	const point = { lat: position.coords.latitude, lng: position.coords.longitude };
	setGpsStatus("found", "GPS conectado");
	elements.mapLabel.textContent = `${point.lat.toFixed(3)}, ${point.lng.toFixed(3)}`;
	if (lastPoint) {
		const segment = haversineMeters(lastPoint, point);
		if (segment <= 80) distanceMeters += segment;
	}
	lastPoint = point;
	if (map) {
		if (!currentMarker) {
			currentMarker = L.marker(point, { icon: L.divIcon({ className: "route-dot", iconSize: [15, 15], iconAnchor: [7, 7] }) }).addTo(map);
			map.setView(point, 16, { animate: true });
		} else {
			currentMarker.setLatLng(point);
		}
		routeLine.addLatLng(point);
	}
	updateMetrics();
}

function onLocationError(error) {
	const messages = {
		1: "Permita o acesso ao GPS",
		2: "Sinal GPS indisponível",
		3: "GPS demorou para responder"
	};
	setGpsStatus("error", messages[error.code] || "Falha no GPS");
}

function stopLocationWatch() {
	if (watchId !== null) navigator.geolocation.clearWatch(watchId);
	watchId = null;
	lastPoint = null;
}

function startTimer() {
	segmentStartedAt = Date.now();
	clearInterval(timerId);
	timerId = window.setInterval(() => {
		updateMetrics();
		const seconds = currentDuration();
		if (seconds - lastSavedSecond >= 10) saveProgress();
	}, 1000);
}

function stopTimer() {
	clearInterval(timerId);
	timerId = null;
	segmentStartedAt = 0;
	updateMetrics();
}

async function saveProgress() {
	if (!activeWalk || savingProgress) return;
	savingProgress = true;
	try {
		await request(`/${activeWalk.id}/progress`, {
			method: "PUT",
			body: JSON.stringify({ durationSeconds: currentDuration(), distanceMeters })
		});
		lastSavedSecond = currentDuration();
	} catch (error) {
		setFeedback(error.message);
	} finally {
		savingProgress = false;
	}
}

function resetRoute() {
	if (routeLine) routeLine.setLatLngs([]);
	if (currentMarker && map) map.removeLayer(currentMarker);
	currentMarker = null;
	lastPoint = null;
	elements.mapLabel.textContent = "BRASIL";
}

async function startWalk() {
	setFeedback("");
	const button = elements.actions.querySelector("button");
	button.disabled = true;
	try {
		activeWalk = await request("", { method: "POST" });
		distanceMeters = 0;
		elapsedBeforeSegment = 0;
		lastSavedSecond = 0;
		resetRoute();
		startTimer();
		startLocationWatch();
		drawActions("ACTIVE");
	} catch (error) {
		button.disabled = false;
		setFeedback(error.message);
	}
}

async function pauseWalk() {
	elapsedBeforeSegment = currentDuration();
	stopTimer();
	stopLocationWatch();
	try {
		await saveProgress();
		activeWalk = await request(`/${activeWalk.id}/pause`, { method: "POST" });
		drawActions("PAUSED");
	} catch (error) {
		setFeedback(error.message);
	}
}

async function resumeWalk() {
	setFeedback("");
	try {
		activeWalk = await request(`/${activeWalk.id}/resume`, { method: "POST" });
		startTimer();
		startLocationWatch();
		drawActions("ACTIVE");
	} catch (error) {
		setFeedback(error.message);
	}
}

async function finishWalk() {
	setFeedback("");
	const buttons = [...elements.actions.querySelectorAll("button")];
	buttons.forEach((button) => { button.disabled = true; });
	const finalDuration = currentDuration();
	elapsedBeforeSegment = finalDuration;
	stopTimer();
	stopLocationWatch();
	try {
		await request(`/${activeWalk.id}/finish`, {
			method: "POST",
			body: JSON.stringify({ durationSeconds: finalDuration, distanceMeters })
		});
		activeWalk = null;
		elapsedBeforeSegment = 0;
		distanceMeters = 0;
		updateMetrics();
		drawActions(null);
		await refreshHistory();
		setGpsStatus("waiting", "Aguardando GPS");
		setFeedback("Caminhada salva no histórico.");
	} catch (error) {
		setFeedback(error.message);
		if (activeWalk) {
			activeWalk.status = "ACTIVE";
			startTimer();
			startLocationWatch();
			drawActions("ACTIVE");
		}
	}
}

function renderHistory(walks) {
	elements.history.replaceChildren();
	elements.historyTotal.textContent = `${walks.length} ${walks.length === 1 ? "sessão" : "sessões"}`;
	if (!walks.length) {
		const empty = document.createElement("div");
		empty.className = "empty-state";
		empty.innerHTML = '<span class="empty-mark"><i data-lucide="footprints"></i></span><strong>A primeira começa quando você quiser.</strong><span>Suas caminhadas concluídas aparecerão aqui.</span>';
		elements.history.append(empty);
		if (window.lucide) window.lucide.createIcons();
		return;
	}
	for (const walk of walks) {
		const entry = document.createElement("article");
		entry.className = "history-entry";
		const date = new Date(walk.startedAt);
		const hours = Math.floor(walk.durationSeconds / 3600);
		const remainingMinutes = Math.floor((walk.durationSeconds % 3600) / 60);
		const duration = hours ? `${hours}h ${String(remainingMinutes).padStart(2, "0")}min` : `${remainingMinutes} min`;
		entry.innerHTML = `
			<div class="history-date"><span class="history-date-mark"><i data-lucide="footprints"></i></span><span class="history-date-copy"><strong></strong><small></small></span></div>
			<div class="history-metric"><span>DISTÂNCIA</span><strong>${formatNumber.format(walk.distanceMeters / 1000)} km</strong></div>
			<div class="history-metric"><span>DURAÇÃO</span><strong>${duration}</strong></div>
			<div class="history-metric"><span>RITMO</span><strong>${walk.averagePaceSecondsPerKm == null ? "--:--" : formatPace(walk.averagePaceSecondsPerKm)} /km</strong></div>
			<span class="status-chip">CONCLUÍDA</span>`;
		entry.querySelector(".history-date-copy strong").textContent = formatDate.format(date);
		entry.querySelector(".history-date-copy small").textContent = formatTime.format(date);
		elements.history.append(entry);
	}
	if (window.lucide) window.lucide.createIcons();
}

async function refreshHistory() {
	const walks = await request();
	const completed = walks.filter((walk) => walk.status === "COMPLETED");
	renderHistory(completed);
	const now = Date.now();
	const weekDistance = completed
		.filter((walk) => now - new Date(walk.startedAt).getTime() <= 7 * 24 * 60 * 60 * 1000)
		.reduce((total, walk) => total + walk.distanceMeters, 0);
	const totalSeconds = completed.reduce((total, walk) => total + walk.durationSeconds, 0);
	elements.weekDistance.textContent = formatNumber.format(weekDistance / 1000);
	elements.weekDistanceSide.textContent = formatNumber.format(weekDistance / 1000);
	elements.weekProgress.style.width = `${Math.min(100, weekDistance / 15000 * 100)}%`;
	elements.walkCount.textContent = String(completed.length);
	elements.totalTime.textContent = `${Math.floor(totalSeconds / 3600)}h ${String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, "0")}`;
}

async function restoreActiveWalk() {
	activeWalk = await request("/active");
	if (!activeWalk) {
		drawActions(null);
		return;
	}
	distanceMeters = activeWalk.distanceMeters;
	elapsedBeforeSegment = activeWalk.durationSeconds;
	lastSavedSecond = elapsedBeforeSegment;
	updateMetrics();
	if (activeWalk.status === "ACTIVE") {
		startTimer();
		startLocationWatch();
	}
	drawActions(activeWalk.status);
}

async function initialize() {
	elements.date.textContent = formatDate.format(new Date());
	initMap();
	if (window.lucide) window.lucide.createIcons();
	try {
		await Promise.all([refreshHistory(), restoreActiveWalk()]);
	} catch (error) {
		setFeedback(error.message || "Não foi possível carregar seus registros.");
	}
}

initialize();