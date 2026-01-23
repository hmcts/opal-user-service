Docker using local version of fines (Based on your local changes)
(All other services will use the images from sdshmctspublic)
```bash / zsh
 docker compose -p opal-user-local \
  -f docker-compose.base.yml \
  -f docker-compose.local.yml \
  -f ../opal-fines-service/docker-compose.base.yml \
  -f ../opal-fines-service/docker-compose.master.yml \
  -f ../opal-logging-service/docker-compose.base.yml \
  -f ../opal-logging-service/docker-compose.master.yml \
  up --build -d
```
Docker using master version of fines (Based on the code in master)
```bash / zsh
docker compose -p opal-user-master \
  -f docker-compose.base.yml \
  -f docker-compose.master.yml \
  -f ../opal-fines-service/docker-compose.base.yml \
  -f ../opal-fines-service/docker-compose.master.yml \
  -f ../opal-logging-service/docker-compose.base.yml \
  -f ../opal-logging-service/docker-compose.master.yml \
  up --build -d
```
