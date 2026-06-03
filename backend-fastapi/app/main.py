from fastapi import FastAPI

from app.routers import bible, csrf, health, livekit, reports


def create_app() -> FastAPI:
    app = FastAPI(
        title="YouTube Rating API",
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )
    app.include_router(health.router)
    app.include_router(csrf.router)
    app.include_router(reports.router)
    app.include_router(bible.router)
    app.include_router(livekit.router)
    return app


app = create_app()
