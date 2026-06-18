# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS build
WORKDIR /workspace/apps/frontend

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

COPY apps/frontend/package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY apps/frontend ./
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY --from=build /workspace/apps/frontend/dist /usr/share/nginx/html
RUN printf '%s\n' \
  'server {' \
  '  listen 8080;' \
  '  server_name _;' \
  '  root /usr/share/nginx/html;' \
  '  index index.html;' \
  '  location = /healthz { access_log off; add_header Content-Type text/plain; return 200 "ok\n"; }' \
  '  location /assets/ { try_files $uri =404; add_header Cache-Control "public, max-age=31536000, immutable"; }' \
  '  location / { try_files $uri $uri/ /index.html; }' \
  '}' \
  > /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
