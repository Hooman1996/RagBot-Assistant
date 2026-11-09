"""Picks the feature a sentence means: TEI narrows the features, then the model chooses one.

Called by agent_graph's understand node. The model sees only the user's words and the offered
features (intent, Persian title, description and slot names). It never sees a screen, an edge, a
form or the user-only list. Its answer is held to an enum of the offered values, and code still
refuses anything else.
"""

from assistant.models import Chat, Embeddings

CLOSEST = 8
KNOWLEDGE = "knowledge_question"
NOT_SURE = "not_sure"


def feature_text(feature: dict) -> str:
    """What is embedded for a feature: its Persian title and its description."""
    return f"{feature['title']} — {feature['description']}"


class FeatureIndex:
    """The features' vectors, computed on first use and kept for the life of the process."""

    def __init__(self, features: list[dict], embeddings: Embeddings) -> None:
        self.features = features
        self.embeddings = embeddings
        self.vectors: list[list[float]] | None = None

    def closest(self, text: str) -> list[dict]:
        """The CLOSEST features to the text, nearest first."""
        if self.vectors is None:
            self.vectors = self.embeddings.embed([feature_text(item) for item in self.features])
        query = self.embeddings.embed([text], query=True)[0]
        # The vectors have unit length, so the dot product is the cosine similarity.
        scores = [sum(a * b for a, b in zip(query, vector, strict=True)) for vector in self.vectors]
        ranked = sorted(range(len(self.features)), key=lambda index: scores[index], reverse=True)
        return [self.features[index] for index in ranked[:CLOSEST]]


def choice_prompt(text: str, offered: list[dict]) -> str:
    """The question for the model: the offered features and the user's words, nothing else."""
    lines = [
        "You choose which feature of a banking app the user's Persian message asks for.",
        "Features:",
    ]
    for item in offered:
        slots = ", ".join(item["required_slots"]) or "none"
        lines.append(f"- {item['intent']}: «{item['title']}» — {item['description']} Values: {slots}")
    lines += [
        f"Answer {KNOWLEDGE} when the message asks for information or advice, not for something to do.",
        f"Answer {NOT_SURE} when no feature clearly fits.",
        f"User message: «{text}»",
    ]
    return "\n".join(lines)


def choose(chat: Chat, text: str, offered: list[dict]) -> str:
    """One offered intent, KNOWLEDGE or NOT_SURE. Anything else becomes NOT_SURE."""
    allowed = [item["intent"] for item in offered] + [KNOWLEDGE, NOT_SURE]
    schema = {
        "type": "object",
        "properties": {"choice": {"enum": allowed}},
        "required": ["choice"],
        "additionalProperties": False,
    }
    choice = chat.json(choice_prompt(text, offered), schema).get("choice")
    return choice if choice in allowed else NOT_SURE
