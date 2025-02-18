import { fileURLToPath, URL } from 'node:url'
import { resolve, dirname } from 'path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { version } from './package.json'
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite'


const libraryFileName = 'elaastic-vue-components'

// https://vite.dev/config/
export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(version)
  },
  build: {
    lib: {
      entry: resolve(__dirname, 'src/main.ts'),
      name: 'ElaasticVueComponents',
      fileName: libraryFileName,
      formats: ['umd']
    },
    rollupOptions: {
      external: ['vue', 'vuetify', 'vue-i18n'],
      output: {
        entryFileNames: `${libraryFileName}-v${version}.umd.min.js`,
        assetFileNames: `[name]-v${version}.[ext]`,
        globals: {
          vue: 'Vue',
          vuetify: "Vuetify",
          "vue-i18n": 'VueI18n',
        }
      }
    }
  },
  plugins: [
    vue(),
    vueDevTools(),
    VueI18nPlugin({
      /* options */
      // locale messages resource pre-compile option
      include: resolve(dirname(fileURLToPath(import.meta.url)), './src/locales/**'),
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
