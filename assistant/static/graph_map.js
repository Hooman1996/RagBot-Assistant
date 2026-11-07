// Draws the map and keeps it in step with the replica. Loaded by graph.html.
// It only reads the backend. A click is passed to graph_task.js, which decides what to show.

const NODE_WIDTH = 270, NODE_HEIGHT = 20;
const svgSpace = "http://www.w3.org/2000/svg";
const world = document.getElementById("world");
const map = document.getElementById("map");
const elements = new Map();
let deviceId = "";
let view = { x: 20, y: 20, scale: 0.6 };

function make(name, attributes) {
  const element = document.createElementNS(svgSpace, name);
  for (const key in attributes) element.setAttribute(key, attributes[key]);
  return element;
}

function applyView() {
  world.setAttribute("transform", `translate(${view.x} ${view.y}) scale(${view.scale})`);
}

function drawEdge(edge, places) {
  const start = places.get(edge.from), end = places.get(edge.to);
  const line = make("line", {
    class: edge.walked ? "edge walked" : "edge",
    x1: start.x + NODE_WIDTH, y1: start.y + NODE_HEIGHT / 2,
    x2: end.x, y2: end.y + NODE_HEIGHT / 2,
  });
  world.append(line);
}

function drawNode(node) {
  const group = make("g", { class: node.built ? "node" : "node grey", transform: `translate(${node.x} ${node.y})` });
  group.append(make("rect", { width: NODE_WIDTH, height: NODE_HEIGHT, rx: 4 }));
  const label = make("text", { x: 6, y: 14 });
  label.textContent = `${node.id} ${node.name}`.slice(0, 44);
  group.append(label);
  group.addEventListener("click", () => clicked(node));
  world.append(group);
  elements.set(node.id, { group, node });
}

function markRoute(screens) {
  for (const { group } of elements.values()) group.classList.remove("route");
  for (const screen of screens) elements.get(screen)?.group.classList.add("route");
}

function describe(result) {
  if (!result) return "None yet";
  const lines = [`${result.outcome} at ${result.observation.screen}`];
  if (result.needs) lines.push(`Needs: ${result.needs.join(", ")}`);
  if (result.error_code) lines.push(`Error: ${result.error_code}`);
  for (const step of result.trace || []) lines.push(`${step.from} → ${step.to}: ${step.action}`);
  return lines.join("\n");
}

async function refresh() {
  const state = await (await fetch("/graph/state")).json();
  const screen = state.observation ? state.observation.screen : null;
  for (const { group } of elements.values()) group.classList.remove("here");
  if (elements.has(screen)) elements.get(screen).group.classList.add("here");
  document.getElementById("now").textContent = screen
    ? `${screen} ${elements.get(screen)?.node.name ?? ""}${state.waiting ? "\nA command is waiting" : ""}`
    : "Waiting for the replica";
  document.getElementById("result").textContent = describe(state.last_result);
  document.getElementById("revision").textContent = state.source_revision;
}

function find(text) {
  const wanted = text.trim().toLowerCase();
  for (const { group, node } of elements.values()) {
    const hit = wanted !== "" && `${node.id} ${node.name}`.toLowerCase().includes(wanted);
    group.classList.toggle("found", hit);
  }
}

function enablePanAndZoom() {
  let last = null;
  map.addEventListener("pointerdown", event => { last = event; });
  window.addEventListener("pointerup", () => { last = null; });
  window.addEventListener("pointermove", event => {
    if (!last) return;
    view.x += event.clientX - last.clientX;
    view.y += event.clientY - last.clientY;
    last = event;
    applyView();
  });
  map.addEventListener("wheel", event => {
    event.preventDefault();
    const factor = event.deltaY < 0 ? 1.15 : 0.87;
    view.x = event.offsetX - (event.offsetX - view.x) * factor;
    view.y = event.offsetY - (event.offsetY - view.y) * factor;
    view.scale *= factor;
    applyView();
  }, { passive: false });
}

async function start() {
  const data = await (await fetch("/graph/data")).json();
  deviceId = data.device_id;
  const places = new Map(data.nodes.map(node => [node.id, node]));
  data.edges.forEach(edge => drawEdge(edge, places));
  data.nodes.forEach(drawNode);
  applyView();
  enablePanAndZoom();
  document.getElementById("search").addEventListener("input", event => find(event.target.value));
  setInterval(refresh, 500);
  refresh();
}

start();
