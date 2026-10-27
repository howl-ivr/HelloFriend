# Despliegue: frontend en Vercel

## Opción actual: backend en una Mac del equipo con ngrok

```
Navegador ──HTTPS──► Vercel (frontend) ──/api/* reenviado──► https://TU-DOMINIO.ngrok-free.app ──► Mac :8080 (backend + MySQL local)
```

1. Cuenta gratuita en ngrok.com. En el panel: copia el **authtoken** y el **dominio** asignado (`xxx.ngrok-free.app`).
2. En la Mac: `brew install ngrok` y `ngrok config add-authtoken TU_TOKEN`.
3. En `backend/.env`: `CORS_ORIGINS=http://localhost:5173,https://tu-proyecto.vercel.app` y
   `FRONTEND_URL=https://tu-proyecto.vercel.app`.
4. Levanta MySQL y el backend como siempre (puerto 8080).
5. Túnel (déjalo abierto): `ngrok http --url=xxx.ngrok-free.app 8080`.
6. En `frontend/vercel.json` reemplaza `TU-DOMINIO` por tu dominio de ngrok. En Vercel: Root Directory `frontend`.
7. Que la Mac no se suspenda mientras se use: `caffeinate -dimsu` en otra terminal, con el cargador conectado y la
   tapa abierta.

Si la Mac se apaga, se suspende o pierde internet, la web carga pero sin datos.

## Alternativa: backend en una EC2 de AWS

Arquitectura (ver `docs/decisiones/2026-10-27-despliegue-vercel-ec2.md`):

```
Navegador ──HTTPS──► Vercel (frontend)  ──/api/* reenviado por Vercel──►  EC2 :80 ──► backend (Docker) ──► MySQL (Docker)
```

El navegador solo habla con Vercel por HTTPS. Vercel reenvía `/api/*` a la EC2 (`frontend/vercel.json`), así que la
EC2 no necesita dominio ni certificado, y no hay problemas de CORS.

## 1. Crear la EC2 (consola de AWS, cuenta personal)

1. Región `us-east-1`. **EC2 → Launch instance**.
2. Imagen: **Ubuntu Server 24.04 LTS**. Tipo: **t3.small** (2 GB; con 1 GB Java y MySQL no caben bien).
3. Key pair: crea uno y guarda el `.pem`.
4. Grupo de seguridad:
   - SSH (22) desde **My IP**.
   - HTTP (80) desde **0.0.0.0/0** (Vercel llama desde IPs variables).
   - Nada más: MySQL no se abre.
5. Disco: 20 GB gp3.
6. **Elastic IPs → Allocate** y asóciala a la instancia, para que la IP no cambie al reiniciar.
7. **Billing → Budgets**: crea un presupuesto con alerta (además suma crédito al plan gratuito).

## 2. Preparar el servidor

```bash
chmod 400 hellofriend.pem
ssh -i hellofriend.pem ubuntu@IP_ELASTICA

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit   # vuelve a entrar para que el grupo docker se aplique
```

## 3. Levantar backend y MySQL

El repositorio es privado: clona con un token personal de GitHub (Settings → Developer settings → Fine-grained token,
solo lectura de este repo) o con `gh auth login`.

```bash
git clone https://github.com/howl-ivr/HelloFriend.git
cd HelloFriend
git switch main          # o la rama que se vaya a desplegar

cp deploy/.env.produccion.example deploy/.env
nano deploy/.env         # contraseñas, JWT_SECRET (openssl rand -base64 48) y URL de Vercel

docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --build
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml logs -f backend
```

La primera vez tarda (descarga dependencias de Maven y compila). Flyway crea las tablas al arrancar.

Comprobar (debe responder **401**, que significa que el backend está vivo y exige login):

```bash
curl -i http://localhost/api/canales
```

## 4. Frontend en Vercel

1. En `frontend/vercel.json` reemplaza `IP_ELASTICA_DE_LA_EC2` por la IP elástica y súbelo a la rama.
2. vercel.com → **Add New → Project** → importa el repositorio.
3. **Root Directory:** `frontend`. Framework: Vite (lo detecta solo). No hace falta `VITE_API_URL`.
4. Deploy. Copia la URL (`https://xxx.vercel.app`).
5. En la EC2, pon esa URL en `CORS_ORIGINS` y `FRONTEND_URL` de `deploy/.env` y reinicia:

```bash
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d
```

## Actualizar a una nueva versión

```bash
cd HelloFriend && git pull
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml up -d --build
```

Vercel redespliega solo con cada push a la rama configurada.

## Cuidados

- **Nunca** `docker compose ... down -v`: borra la base de datos.
- `deploy/.env` no se sube a Git (está en `.gitignore`).
- Respaldo rápido de la base:
  `docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml exec mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" hellofriend' > respaldo.sql`
