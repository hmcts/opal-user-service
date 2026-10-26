FROM postgres:17

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        postgresql-17-pgtap \
        libtap-parser-sourcehandler-pgtap-perl \
    && rm -rf /var/lib/apt/lists/*
