# syntax=docker/dockerfile:1

# ---- Stage 1: build the Angular app ----
FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies first, so this layer is cached while package*.json are unchanged.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---- Stage 2: serve the static files with nginx ----
FROM nginx:1.30-alpine

# The official nginx image runs envsubst on /etc/nginx/templates/*.template at startup,
# so ${BACKEND_URL} is filled in from the container's environment.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/tickettriage/browser /usr/share/nginx/html

# Only BACKEND_URL is substituted; nginx's own $variables are left alone.
ENV NGINX_ENVSUBST_FILTER=BACKEND_URL
ENV BACKEND_URL=http://backend:8080

EXPOSE 80
