# How to run

1. Install dependencies (using `uv`):
   ```bash
   uv sync
   ```

2. Create `.env` file and fill it with your secrets. Template of `.env` file is in `.env.template` file

3. Run the application:
   ```bash
   uv run uvicorn main:app --reload
   ```

4. Open your browser and navigate to:
   ```
   http://127.0.0.1:8000
   ```
