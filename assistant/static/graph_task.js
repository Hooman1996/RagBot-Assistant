// A click on a node: its route, its questions, and the run. Loaded by graph.html after graph_map.js.
// Every decision is the backend's. This file shows the plan, collects the answers and shows the
// steps. Its only buttons fill the inputs and start a run, which stops at the review.

const STOPPED = {
  review: "Stopped at the review. Only the user confirms, on the phone.",
  arrived: "Arrived. This screen has no review.",
};
const NOT_PLANNED = {
  no_route: "No route the assistant may take leads here: it sits behind the user's own step.",
  user_only: "The only route crosses an action only the user may perform.",
};
let plan = null;
let target = null;

function show(id, text) {
  document.getElementById(id).textContent = text;
}

function routeLines(route) {
  if (route.length === 0) return "Already here";
  return route.map(step => `${step.from} → ${step.to}: ${step.action}`).join("\n");
}

function questionFor(item) {
  const label = document.createElement("label");
  label.textContent = item.ask;
  const hint = document.createElement("small");
  hint.textContent = `${item.description ?? item.ref} · ${item.screen} · ${item.type}${item.lookup ? " · " + item.lookup : ""}`;
  const input = document.createElement("input");
  input.dataset.ref = item.ref;
  label.append(hint, input);
  return label;
}

function showQuestions() {
  document.getElementById("task").hidden = false;
  document.getElementById("questions").replaceChildren(...plan.requirements.map(questionFor));
}

function inputs() {
  return [...document.querySelectorAll("#questions input")];
}

function fillDemo() {
  for (const input of inputs()) input.value = plan.demo[input.dataset.ref] ?? "";
}

async function moveTo(node) {
  show("clicked", `Moving to ${node.id} ${node.name}`);
  const reply = await fetch("/test-harness/v1/commands", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ device_id: deviceId, destination: node.id }),
  });
  if (!reply.ok) show("clicked", `${node.id}: ${(await reply.json()).detail}`);
}

async function clicked(node) {
  document.getElementById("task").hidden = true;
  markRoute([]);
  if (!node.built) {
    show("clicked", `${node.id} is not built`);
    show("route", "None");
    return;
  }
  show("clicked", `${node.id} ${node.name}`);
  plan = await (await fetch(`/graph/plan/${node.id}`)).json();
  target = node.id;
  if (plan.status !== "ok") {
    show("route", NOT_PLANNED[plan.status]);
    return;
  }
  show("route", routeLines(plan.route));
  markRoute(plan.route.map(step => step.to));
  if (plan.requirements.length === 0) await moveTo(node);
  else showQuestions();
}

function stepBlock(step) {
  const block = document.createElement("pre");
  block.className = "step";
  const lines = [`${step.operation} ${JSON.stringify(step.arguments)}`, `→ ${step.outcome} at ${step.screen}`];
  for (const field of step.fields) lines.push(`  ${field.ref}: ${field.value}`);
  if (step.needs.length > 0) lines.push(`  needs: ${step.needs.join(", ")}`);
  if (step.error_code) lines.push(`  error: ${step.error_code}`);
  block.textContent = lines.join("\n");
  return block;
}

function showProgress(progress) {
  document.getElementById("steps").replaceChildren(...progress.steps.map(stepBlock));
  const text = progress.state === "running" ? "Running" : STOPPED[progress.state] ?? `Stopped: ${progress.reason}`;
  show("state", text);
}

async function follow() {
  const progress = await (await fetch("/graph/run")).json();
  showProgress(progress);
  if (progress.state === "running") setTimeout(follow, 400);
}

async function run() {
  const values = Object.fromEntries(inputs().map(input => [input.dataset.ref, input.value]));
  const reply = await fetch("/graph/run", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ screen: target, values }),
  });
  if (reply.ok) follow();
  else show("state", `Refused: ${(await reply.json()).detail}`);
}

document.getElementById("demo").addEventListener("click", fillDemo);
document.getElementById("run").addEventListener("click", run);
