import os
import sys
import asyncio
from typing import Any, Optional
from fastapi import FastAPI, Request, Form, HTTPException, Depends
from fastapi.responses import HTMLResponse
from fastapi.exceptions import RequestValidationError
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, Field, ValidationError
import httpx
from dotenv import load_dotenv
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

load_dotenv()

CLIENT_ID = os.getenv("TWITCH_CLIENT_ID")
CLIENT_SECRET = os.getenv("TWITCH_CLIENT_SECRET")

if not CLIENT_ID or not CLIENT_SECRET:
    sys.exit("Lack of necessary environment variables")

limiter = Limiter(key_func=get_remote_address)
app = FastAPI(
    title="Twitch Game Comparator",
    description="Simple game's Twitch stats comparator using IGDB as information source",
    version="1.0.0"
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

templates = Jinja2Templates(directory="templates")


@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded):
    return templates.TemplateResponse(
        request,
        "index.html",
        {
            "error": "You exceeded your rate limit. Wait 1 minute and try again later.",
        },
        status_code=429
    )

@app.exception_handler(404)
async def not_found_handler(request: Request, exc):
    return templates.TemplateResponse(
        request,
        "index.html",
        {"error": "There is no such page."},
        status_code=404
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    error_msg = f"Validation error: {errors[0]['msg']} (field: {errors[0]['loc'][-1]})"

    return templates.TemplateResponse(
        request,
        "index.html",
        {"error": error_msg},
        status_code=422
    )

@app.exception_handler(405)
async def not_found_handler(request: Request, exc):
    return templates.TemplateResponse(
        request,
        "index.html",
        {"error": "Method not allowed. Fill the form below."},
        status_code=405
    )


class TopStream(BaseModel):
    user_name: str
    title: str
    viewers: int
    url: str


class GameMetrics(BaseModel):
    count: int
    count_str: str
    viewers: int
    avg: float
    top_stream: Optional[TopStream]


class GameInfo(BaseModel):
    name: str
    summary: str
    rating: str


class FullGameData(BaseModel):
    name: str
    cover: str
    stats: GameMetrics
    info: GameInfo


class ComparisonReport(BaseModel):
    game1: FullGameData
    game2: FullGameData
    winner: str
    percent: float



class TwitchAuth:
    def __init__(self):
        self.token = None

    async def get_token(self):
        if self.token: return self.token
        async with httpx.AsyncClient() as client:
            resp = await client.post("https://id.twitch.tv/oauth2/token", params={
                "client_id": CLIENT_ID,
                "client_secret": CLIENT_SECRET,
                "grant_type": "client_credentials"
            })
            if resp.status_code != 200:
                raise HTTPException(status_code=502, detail="Auth error in Twitch API.")
            self.token = resp.json()["access_token"]
            return self.token


auth_manager = TwitchAuth()


def calculate_metrics(stream_data: dict) -> GameMetrics:
    streams = stream_data.get("data", [])
    if not streams:
        return GameMetrics(count=0, count_str="0", viewers=0, avg=0.0, top_stream=None)

    total_viewers = sum(s["viewer_count"] for s in streams)
    count = len(streams)
    top = max(streams, key=lambda s: s["viewer_count"])

    top_obj = TopStream(
        user_name=top["user_name"],
        title=top["title"][:100],
        viewers=top["viewer_count"],
        url=f"https://twitch.tv/{top['user_login']}"
    )

    return GameMetrics(
        count=count,
        count_str="100+" if count == 100 else str(count),
        viewers=total_viewers,
        avg=round(total_viewers / count, 2),
        top_stream=top_obj
    )


async def get_game_info_igdb(game_name: str, token: str) -> GameInfo:
    headers = {
        "Client-ID": CLIENT_ID,
        "Authorization": f"Bearer {token}",
        "Content-Type": "text/plain"
    }
    query = f'fields name, summary, rating; search "{game_name}"; limit 1;'

    async with httpx.AsyncClient(headers=headers) as client:
        try:
            resp = await client.post("https://api.igdb.com/v4/games", content=query)
            if resp.status_code == 200 and resp.json():
                g = resp.json()[0]
                return GameInfo(
                    name=g.get("name", game_name),
                    summary=g.get("summary", "No description in IGDB."),
                    rating=f"{round(g['rating'], 1)}/100" if g.get("rating") else "No rating"
                )
        except Exception:
            pass
    return GameInfo(name=game_name, summary="No description", rating="No rating")


@app.get(
    "/", response_class=HTMLResponse,
    summary="Main page",
    description="Comparator's main page where user passes names of two games which Twitch's stats wants to compare",
    responses={
        200: {"description": "HTML form page"},
    },
)
async def index(request: Request):
    return templates.TemplateResponse(request, "index.html")


@app.post(
    "/compare", response_class=HTMLResponse,
    summary="Report page",
    description="Page where user can see stats from Twitch and information about game from IGDB."
                "\n\nRate limit: 5 requests per minute",
    responses={
            200: {"description": "HTML page with game's stats comparison"},
            405: {"description": "User tried to use this endpoint without filling form"},
            422: {"description": "Form data validation error"},
            429: {"description": "Rate limit exceeded"},
        },
)
@limiter.limit("5/minute")
async def compare_games(
        request: Request,
        game1: str = Form(..., min_length=2, max_length=50, description="First game's name"),
        game2: str = Form(..., min_length=2, max_length=50, description="Second game's name"),
):
    token = await auth_manager.get_token()
    headers = {"Client-ID": CLIENT_ID, "Authorization": f"Bearer {token}"}

    async with httpx.AsyncClient(headers=headers, timeout=15.0) as client:
        try:
            r1, r2 = await asyncio.gather(
                client.get("https://api.twitch.tv/helix/games", params={"name": game1}),
                client.get("https://api.twitch.tv/helix/games", params={"name": game2})
            )

            d1, d2 = r1.json().get("data"), r2.json().get("data")
            if not d1 or not d2:
                missing = game1 if not d1 else game2
                return templates.TemplateResponse(request, "index.html", {"error": f"Not found game: {missing}"})

            id1, id2 = d1[0]["id"], d2[0]["id"]

            s1, s2, i1, i2 = await asyncio.gather(
                client.get("https://api.twitch.tv/helix/streams", params={"game_id": id1, "first": 100}),
                client.get("https://api.twitch.tv/helix/streams", params={"game_id": id2, "first": 100}),
                get_game_info_igdb(game1, token),
                get_game_info_igdb(game2, token)
            )

            g1_data = FullGameData(
                name=d1[0]["name"],
                cover=d1[0]["box_art_url"].replace("{width}x{height}", "281x351"), # GTA5 cover art size
                stats=calculate_metrics(s1.json()),
                info=i1
            )

            g2_data = FullGameData(
                name=d2[0]["name"],
                cover=d2[0]["box_art_url"].replace("{width}x{height}", "281x351"), # GTA5 cover art size
                stats=calculate_metrics(s2.json()),
                info=i2
            )

            winner_name = g1_data.name if g1_data.stats.viewers > g2_data.stats.viewers else g2_data.name
            diff = abs(g1_data.stats.viewers - g2_data.stats.viewers)
            max_v = max(g1_data.stats.viewers, g2_data.stats.viewers, 1)

            report = ComparisonReport(
                game1=g1_data,
                game2=g2_data,
                winner=winner_name,
                percent=round((diff / max_v) * 100, 2)
            )

            return templates.TemplateResponse(request, "result.html", {"data": report})

        except ValidationError as ve:
            return templates.TemplateResponse(request, "index.html", {"error": f"Data's validation error: {ve.json()}"})
        except Exception as e:
            return templates.TemplateResponse(request, "index.html", {"error": f"Connection error: {str(e)}"})
