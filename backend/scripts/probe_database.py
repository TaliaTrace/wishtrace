import asyncio
import json

from app.config import get_settings
from app.database import create_database_engine, probe_database


async def main() -> None:
    settings = get_settings()
    engine = create_database_engine(settings)
    try:
        result = await probe_database(engine)
    finally:
        await engine.dispose()
    print(
        json.dumps(
            {
                "connected": result.connected,
                "tls": result.tls,
                "server_version": result.server_version,
            }
        )
    )


if __name__ == "__main__":
    asyncio.run(main())
