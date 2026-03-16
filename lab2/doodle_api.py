from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel
from typing import List
import uuid

app = FastAPI()

polls = {}
votes = {}


class PollCreate(BaseModel):
    title: str
    options: List[str]


class PollResponse(PollCreate):
    id: str
    creator_name: str


class VoteRequest(BaseModel):
    option_index: int



@app.post("/polls", response_model=PollResponse)
async def create_poll(data: PollCreate, user_name: str = Header(alias="User-Name")):
    poll_id = str(uuid.uuid4())[:4]
    new_poll = {
        "id": poll_id,
        "title": data.title,
        "options": data.options,
        "creator_name": user_name
    }
    polls[poll_id] = new_poll
    votes[poll_id] = {}
    return new_poll


@app.get("/polls/{poll_id}")
async def get_poll_results(poll_id: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Ankieta nie istnieje")

    poll = polls[poll_id]
    current_votes = votes[poll_id]

    results = {option: 0 for option in poll["options"]}
    for opt_idx in current_votes.values():
        option_name = poll["options"][opt_idx]
        results[option_name] += 1

    return {"title": poll["title"], "results": results}


@app.put("/polls/{poll_id}")
async def update_poll(poll_id: str, data: PollCreate, user_name: str = Header(alias="User-Name")):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Ankieta nie istnieje")
    if polls[poll_id]["creator_name"] != user_name:
        raise HTTPException(status_code=403, detail="Tylko twórca może edytować tę ankietę!")

    polls[poll_id].update({"title": data.title, "options": data.options})
    votes[poll_id] = {}
    return polls[poll_id]


@app.delete("/polls/{poll_id}")
async def delete_poll(poll_id: str, user_name: str = Header(alias="User-Name")):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Ankieta nie istnieje")
    if polls[poll_id]["creator_name"] != user_name:
        raise HTTPException(status_code=403, detail="Nie masz uprawnień do usunięcia!")

    del polls[poll_id]
    del votes[poll_id]
    return {"status": "Usunięto pomyślnie"}


@app.post("/polls/{poll_id}/vote")
async def cast_vote(poll_id: str, vote: VoteRequest, user_name: str = Header(alias="User-Name")):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Ankieta nie istnieje")
    if vote.option_index >= len(polls[poll_id]["options"]):
        raise HTTPException(status_code=400, detail="Nie ma takiej opcji!")

    votes[poll_id][user_name] = vote.option_index
    return {"message": f"Użytkownik {user_name} zagłosował!"}


@app.post("/polls/{poll_id}/withdraw")
async def withdraw_vote(poll_id: str, user_name: str = Header(alias="User-Name")):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Ankieta nie istnieje")
    if user_name not in votes[poll_id]:
        raise HTTPException(status_code=404, detail=f"Użytkownik {user_name} nie oddał głosu w tej ankiecie")

    del votes[poll_id][user_name]
    return {"message": f"Użytkownik {user_name} wycofał swój głos"}