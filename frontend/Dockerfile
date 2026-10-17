# ==============================================================================
# Étape 1 : Build de l'application React avec Node 22
# ==============================================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Optimisation du cache des dépendances
COPY package.json package-lock.json ./
RUN npm ci

# Copie des sources et compilation de production
COPY . .
RUN npm run build

# ==============================================================================
# Étape 2 : Serveur Nginx Alpine de production
# ==============================================================================
FROM nginx:1.27-alpine

# Configuration personnalisée Nginx (SPA + reverse proxy /api)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copie des assets statiques compilés
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
