"""Starts the development backend: `python -m assistant`.

Port 8100, because host port 8000 belongs to another service. It binds to the local address only.
"""

import uvicorn

from assistant.app import create_app

if __name__ == "__main__":
    uvicorn.run(create_app(), host="127.0.0.1", port=8100)
