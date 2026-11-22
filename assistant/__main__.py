"""Starts the development app or the standalone API on loopback."""

from argparse import ArgumentParser
from pathlib import Path

import uvicorn

from assistant.app import create_app, create_standalone_app
from assistant.config import ConfigError


def main() -> None:
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("--graph", type=Path, help="Standalone profile JSON file")
    parser.add_argument("--services", type=Path, help="Standalone lookup bindings JSON file")
    parser.add_argument("--port", type=int, help="Loopback port (standalone 8101, development 8100)")
    args = parser.parse_args()
    if (args.graph is None) != (args.services is None):
        parser.error("--graph and --services are required together")
    try:
        if args.graph is not None:
            app, default_port = create_standalone_app(args.graph, args.services), 8101
        else:
            app, default_port = create_app(), 8100
    except ConfigError as error:
        parser.error(str(error))
    uvicorn.run(app, host="127.0.0.1", port=args.port if args.port is not None else default_port)


if __name__ == "__main__":
    main()
