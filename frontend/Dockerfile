# Build stage
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM caddy:latest
COPY --from=builder /app/dist /srv
EXPOSE 3000
CMD ["caddy", "file-server", "--listen", ":3000", "--root", "/srv"]
