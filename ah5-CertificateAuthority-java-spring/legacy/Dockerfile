FROM python:3.13-alpine

WORKDIR /app
RUN apk add --no-cache ca-certificates curl
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src/ ./src/

ENV PYTHONUNBUFFERED=1
CMD ["python", "-m", "src.server"]
