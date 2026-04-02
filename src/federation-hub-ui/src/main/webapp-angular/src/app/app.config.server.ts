import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { appConfig } from './app.config';

const serverConfig: ApplicationConfig = {
  providers: [
    provideHttpClient()
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
