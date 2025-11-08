/* global SwaggerUIBundle, SwaggerUIStandalonePreset */

window.onload = function () {
  window.ui = SwaggerUIBundle({
    configUrl: '/v3/api-docs/swagger-config',

    dom_id: '#swagger-ui',
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'StandaloneLayout',

    requestInterceptor: (req) => {
      req.credentials = 'include';
      return req;
    },

    persistAuthorization: true,

    validatorUrl: null
  });
};
