"""Lets the evaluation tests import the agent fakes from tests/backend, as the backend tests do.

Used by pytest only. A site package named `tests` hides this repository's `tests` folder, so
`tests.backend.agent_fakes` cannot be imported; adding the folder to the path is the plain way.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "backend"))
