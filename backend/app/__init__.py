"""WishTrace backend package."""

import asyncio
import sys


def configure_event_loop() -> None:
    """Use the Windows loop implementation supported by psycopg async connections."""

    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())


configure_event_loop()
