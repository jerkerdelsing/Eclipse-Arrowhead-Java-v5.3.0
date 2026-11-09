FROM node:24.22.0-alpine3.24

WORKDIR /app
COPY package.json ./
COPY src ./src

ENV NODE_ENV=production
EXPOSE 2375

# Root only to open /var/run/docker.sock (Docker Desktop exposes it to root). The proxy has no dependencies and
# forwards only the console's list, logs and start/stop/restart calls for its own Compose project.
CMD ["node", "src/proxy.js"]
