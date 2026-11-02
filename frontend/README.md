# TicketTriage frontend

Angular 22 (standalone components, signals, reactive forms) with Angular Material.

```bash
npm ci
npm start          # http://localhost:4200, proxies /api to http://localhost:8080 (proxy.conf.json)
npm test           # unit tests (Vitest + jsdom), add -- --watch=false for a single run
npm run build      # production build in dist/tickettriage/browser
```

The backend must be running on port 8080 for `npm start`. See the main [README](../README.md).
