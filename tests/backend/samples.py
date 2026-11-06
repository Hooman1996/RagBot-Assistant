"""Contract-shaped samples shared by the backend tests."""


def observation(version: int = 3, screen: str = "S-026") -> dict:
    return {
        "observation_version": version,
        "app_version": 1,
        "profile_version": 1,
        "screen": screen,
        "modal": None,
        "loading": False,
        "fields": [],
        "draft": None,
        "supported_operations": ["observe", "navigate"],
    }


def result(command_id: str, outcome: str = "succeeded") -> dict:
    return {
        "command_id": command_id,
        "outcome": outcome,
        "postcondition": "screen_is_S-174",
        "observation": observation(4, "S-174"),
        "draft_version": 0,
    }
