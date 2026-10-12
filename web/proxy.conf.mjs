// The UI calls /api/*; the dev server forwards to the Spring API so no CORS setup is needed.
export default {
  '/api': {
    target: process.env.API_URL ?? 'http://localhost:8080',
    pathRewrite: { '^/api': '' },
  },
};
