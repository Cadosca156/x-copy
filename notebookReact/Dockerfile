# ЭТАП 1: Сборка React-приложения
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files from notebook directory
COPY notebook/package*.json ./
RUN npm install

# Copy the entire notebook source
COPY notebook ./

# Build the React app
RUN npm run build

# ЭТАП 2: Запуск NGINX для раздачи статики
FROM nginx:alpine

# Копируем скомпилированные файлы из первого этапа
COPY --from=builder /app/build /usr/share/nginx/html

# Копируем nginx конфиг с прокси для API
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
