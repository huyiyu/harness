FROM python:3.12-slim
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install uv && uv sync --no-dev
COPY src/ ./src/
COPY gunicorn.conf.py ./
EXPOSE 5000
CMD ["uv", "run", "gunicorn", "-c", "gunicorn.conf.py", "ai_service.app:create_app()"]
