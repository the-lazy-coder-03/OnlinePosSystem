FROM certbot/certbot:v5.8.0

# The upstream stable image currently contains fixable Alpine and Python
# package advisories. Keep the Certbot release pinned while applying only the
# corresponding patched package versions.
RUN apk upgrade --no-cache \
    && python -m pip install --no-cache-dir --upgrade \
        setuptools==78.1.1 msgpack==1.2.1

HEALTHCHECK --interval=12h --timeout=10s --retries=2 \
    CMD ["certbot", "--version"]
