# Phoenix Workout API

AI-powered workout generation backend for the Phoenix app.

## Development (local)
```bash
cd backend/
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python main.py
# → http://localhost:8505
```

## Deploy to pai
```bash
./deploy_to_pai.sh
```

## Endpoints
| Method | Path | Description |
|---|---|---|
| GET | /health | Health check |
| GET | /workout/today | Today's AI workout plan |
| POST | /workout/complete | Log completed workout |
| GET | /exercises | Full exercise library |
| GET | /exercises/hybrid | Non-Vitruvian exercises (travel mode) |
| GET | /user/{id}/history | User workout history |

## Roadmap
- **V1 (MVP):** Rule-based progressive overload, template workouts, local pai deployment
- **V2:** Supabase DB for user history, true progressive overload from logged data
- **V3:** AI model integration (OpenAI/local Ollama) for personalized workout generation
- **Prod:** Migrate from pai to Render/Railway/Fly.io for public launch
