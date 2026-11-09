FROM python:3.13-alpine

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src ./src
RUN addgroup -S arrowhead && adduser -S -G arrowhead arrowhead \
    && mkdir -p /data && chown -R arrowhead:arrowhead /app /data
USER arrowhead
CMD ["python", "src/server.py"]
