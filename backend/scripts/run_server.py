"""Run the local WishTrace API with a psycopg-compatible Windows event loop."""

import asyncio
import os


def configure_event_loop() -> None:
    """Select the Windows event loop implementation supported by psycopg async."""

    if os.name == "nt":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())


def main() -> None:
    configure_event_loop()

    import uvicorn

    uvicorn.run(
        "app.main:create_app",
        factory=True,
        host="127.0.0.1",
        # Uvicorn otherwise selects Proactor on Windows, which psycopg async rejects.
        # With no factory override, asyncio honors the Selector policy configured above.
        loop="none",
        port=8000,
    )


if __name__ == "__main__":
    main()
