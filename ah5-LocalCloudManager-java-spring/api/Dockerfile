FROM node:24.22.0-alpine3.24

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY src ./src

ENV NODE_ENV=production
EXPOSE 8491

# The API runs unprivileged and holds no Docker socket: container lifecycle and logs go through the
# whitelisting docker-proxy (../docker-proxy) on an internal network. Every /api call needs an operator session.
USER node
CMD ["node", "src/server.js"]
