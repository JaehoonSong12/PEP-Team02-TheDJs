# Stage 1: Build the Angular application
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files first for layer caching
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy full source code and build
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Create non-root user
RUN adduser -D -u 1001 nginx-user

# Remove default Nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/nginx.conf

# Copy compiled assets from build stage
COPY --from=build /app/dist/angular-todo-frontend/browser/ /usr/share/nginx/html/

# Set appropriate file ownership on Nginx directories for non-root user
# Include /run for the PID file and /var/log/nginx for logs
RUN chown -R nginx-user:nginx-user /var/cache/nginx /var/run /usr/share/nginx/html /run && \
    touch /run/nginx.pid && chown nginx-user:nginx-user /run/nginx.pid

# Switch to non-root user
USER nginx-user

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
