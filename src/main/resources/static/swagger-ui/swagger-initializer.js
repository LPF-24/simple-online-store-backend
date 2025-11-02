/* global SwaggerUIBundle, SwaggerUIStandalonePreset */

window.onload = function () {
  window.ui = SwaggerUIBundle({
    // Пусть springdoc сам подставит правильные пути и группы
    // Работает за прокси и при нестандартном context-path
    configUrl: '/v3/api-docs/swagger-config',

    dom_id: '#swagger-ui',
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'StandaloneLayout',

    // ВАЖНО: отправлять запросы с cookies/кредами при cross-origin
    requestInterceptor: (req) => {
      req.credentials = 'include'; // без этого браузер не пошлёт cookie
      return req;
    },

    // Удобно, чтобы не терялась авторизация при перезагрузке страницы
    persistAuthorization: true,

    // Отключаем внешнюю валидацию, чтобы не было сетевых запросов наружу
    validatorUrl: null
  });
};
