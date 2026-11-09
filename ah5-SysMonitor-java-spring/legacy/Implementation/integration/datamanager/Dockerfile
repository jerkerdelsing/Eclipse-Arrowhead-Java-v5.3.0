FROM python:3.13-alpine

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src ./src

ENV PYTHONUNBUFFERED=1
EXPOSE 8461

CMD ["python", "src/server.py"]
