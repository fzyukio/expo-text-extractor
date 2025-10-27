// Reexport the native module. On web, it will be resolved to ExpoTextExtractorModule.web.ts
// and on native platforms to ExpoTextExtractorModule.ts
export { default } from './src/ExpoTextExtractorModule';
export * from './src/index';
