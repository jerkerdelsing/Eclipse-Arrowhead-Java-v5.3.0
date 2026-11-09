FROM python:3.13-alpine

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
RUN apk add --no-cache openssl \
    && addgroup -S arrowhead && adduser -S -G arrowhead arrowhead \
    && mkdir -p /data && chown -R arrowhead:arrowhead /app /data
COPY --chown=arrowhead:arrowhead src ./src
USER arrowhead
CMD ["python", "src/server.py"]
