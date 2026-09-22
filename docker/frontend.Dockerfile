# Build context is the REPOSITORY ROOT — `npm run prebuild` regenerates the typescript-angular
# client from ../openapi/cookingchallenge-api.yaml (the generated client is gitignored, so it
# must be produced here rather than copied in).
#   docker build -f docker/frontend.Dockerfile .

# Accepted but unused: the bundle bakes in no version. The UI reads the backend's from
# GET /api/v1/config at runtime, so there is only ever one source of truth. Declared here
# purely so the workflow can pass --build-arg to both images without a BuildKit warning.
ARG APP_VERSION=dev

FROM node:26-alpine AS build
WORKDIR /workspace/frontend

# @openapitools/openapi-generator-cli shells out to `java -jar openapi-generator-cli.jar`.
RUN apk add --no-cache openjdk21-jre-headless

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY openapi /workspace/openapi
COPY frontend/ ./
# prebuild → npm run generate:api, then `ng build` (production configuration by default).
RUN npm run build


FROM nginx:1.29-alpine AS runtime
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s \
    CMD wget -qO- http://localhost/ > /dev/null || exit 1
