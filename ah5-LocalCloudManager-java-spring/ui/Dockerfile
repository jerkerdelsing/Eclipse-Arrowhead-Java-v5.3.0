FROM nginx:1.30.6-alpine

# The server block is a template (security finding J6): the image's entrypoint renders it with envsubst into /tmp,
# which is writable on the read-only root filesystem (J3). Empty defaults keep the features off (e.g. the Go cloud).
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY public /usr/share/nginx/html
ARG CHIPLET_MANAGER=false
RUN if [ "$CHIPLET_MANAGER" = "true" ]; then sed -i 's/data-chiplet-manager="false"/data-chiplet-manager="true"/' /usr/share/nginx/html/index.html; fi
COPY 05-secret-files.envsh 25-drop-secrets.envsh /docker-entrypoint.d/
RUN rm -f /etc/nginx/conf.d/default.conf && chmod -R a+rX /usr/share/nginx/html /etc/nginx \
    && chmod 755 /docker-entrypoint.d/05-secret-files.envsh /docker-entrypoint.d/25-drop-secrets.envsh
ENV NGINX_ENVSUBST_OUTPUT_DIR=/tmp \
    CONSOLE_CANONICAL_HOST="" \
    CONSOLE_CANONICAL_ORIGIN="" \
    CONSOLE_PROXY_SECRET=""

EXPOSE 8490
