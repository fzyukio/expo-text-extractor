import { requireNativeModule } from 'expo-modules-core';

interface ExpoTextExtractorModule {
  isSupported: boolean;
  extractTextFromImage: (uri: string) => Promise<string[]>;
  checkAvailability: () => Promise<{
    available: boolean;
    downloading: boolean;
    playServicesUnavailable?: boolean;
  }>;
}

export default requireNativeModule<ExpoTextExtractorModule>('ExpoTextExtractor');
