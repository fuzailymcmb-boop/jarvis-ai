FROM node:22-bookworm-slim

WORKDIR /app
ENV NODE_ENV=production HOST=0.0.0.0 PORT=8787

# No dependencies to install. Never copy an .env file into the image.
COPY --chown=node:node package.json server.mjs ./
USER node

EXPOSE 8787
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD node --input-type=module -e "const r = await fetch('http://127.0.0.1:' + process.env.PORT + '/health', {signal: AbortSignal.timeout(4000)}); if (!r.ok) process.exit(1);"

CMD ["node", "server.mjs"]
