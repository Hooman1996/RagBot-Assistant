// The chat box: send a Persian request, answer the agent's question, watch the task state.
// Loaded by graph.html after graph_task.js, whose stepBlock shows the runner's steps. Every decision
// is the backend's. Its only buttons send the user's words, one of the options the agent offered, or
// a cancel, which stops the agent's work and never undoes or confirms anything.

let chatTimer = null;

function chatLine(line) {
  const block = document.createElement("pre");
  block.className = `chat ${line.from}`;
  block.textContent = line.text;
  return block;
}

function optionButton(option) {
  const button = document.createElement("button");
  button.type = "button";
  button.textContent = option.label;
  button.addEventListener("click", () => sendTurn({ option: option.ref }));
  return button;
}

function showChat(chat) {
  document.getElementById("chat-messages").replaceChildren(...chat.messages.map(chatLine));
  document.getElementById("chat-options").replaceChildren(...chat.options.map(optionButton));
  const state = chat.busy ? "Working…" : chat.task_state ?? "No task yet";
  document.getElementById("chat-state").textContent = `Task state: ${state}`;
  if (chat.report) {
    document.getElementById("steps").replaceChildren(...chat.report.steps.map(stepBlock));
    show("state", `Chat run: ${chat.report.state}${chat.report.reason ? " · " + chat.report.reason : ""}`);
  }
}

async function followChat() {
  const chat = await (await fetch("/graph/chat")).json();
  showChat(chat);
  chatTimer = chat.busy ? setTimeout(followChat, 500) : null;
}

async function sendTurn(body) {
  const reply = await fetch("/graph/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!reply.ok) {
    document.getElementById("chat-state").textContent = `Refused: ${JSON.stringify((await reply.json()).detail)}`;
    return;
  }
  document.getElementById("chat-text").value = "";
  if (chatTimer === null) followChat();
}

function sendText() {
  const text = document.getElementById("chat-text").value.trim();
  if (text !== "") sendTurn({ text });
}

async function cancelTask() {
  const reply = await fetch("/graph/chat/cancel", { method: "POST" });
  if (!reply.ok) {
    document.getElementById("chat-state").textContent = `Refused: ${JSON.stringify((await reply.json()).detail)}`;
    return;
  }
  if (chatTimer === null) followChat();
}

document.getElementById("chat-send").addEventListener("click", sendText);
document.getElementById("chat-cancel").addEventListener("click", cancelTask);
document.getElementById("chat-text").addEventListener("keydown", event => {
  if (event.key === "Enter") sendText();
});
followChat();
