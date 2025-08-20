# Etapa 1: Construcción
FROM node:18-alpine AS builder

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar archivos de dependencias
COPY package*.json ./

# Instalar dependencias
RUN npm ci

# Copiar el resto del código fuente
COPY . .

# Construir la biblioteca de componentes compartidos
RUN npm run build:ui

# Construir la aplicación para producción
RUN npm run build:app

# Etapa 2: Servidor de producción
FROM nginx:alpine

# Copiar archivo de configuración de nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Copiar los archivos construidos desde la etapa de construcción
COPY --from=builder /app/dist/b2b-take-home-app/browser /usr/share/nginx/html

# Exponer el puerto 80
EXPOSE 80

# Comando para ejecutar nginx
CMD ["nginx", "-g", "daemon off;"]
