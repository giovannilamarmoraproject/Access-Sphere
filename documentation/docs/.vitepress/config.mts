import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Access Sphere",
  description: "Bilingual technical documentation for Access Sphere",
  base: '/docs/',
  cleanUrls: true,
  appearance: true,
  locales: {
    root: {
      label: 'Italiano',
      lang: 'it',
      link: '/it/overview'
    },
    en: {
      label: 'English',
      lang: 'en',
      link: '/en/overview'
    }
  },
  themeConfig: {
    logo: '/logo-minimal.svg',
    logoLink: '/',
    siteTitle: 'Access Sphere',
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Italiano', link: '/it/overview' },
      { text: 'English', link: '/en/overview' },
      { text: 'API', link: '/it/api' }
    ],

    sidebar: {
      '/it/': [
        {
          text: 'Documentazione',
          items: [
            { text: 'Panoramica', link: '/it/overview' },
            { text: 'Architettura', link: '/it/architecture' },
            { text: 'Prerequisiti', link: '/it/required' },
            { text: 'Installazione locale', link: '/it/install' },
            { text: 'Configurazione', link: '/it/configuration' },
            { text: 'Avvio e deploy', link: '/it/deployment' }
          ]
        },
        {
          text: 'Guide tecniche',
          items: [
            { text: 'OAuth 2.0', link: '/it/oauth' },
            { text: 'User e Client IAM', link: '/it/iam' },
            { text: 'MFA e impostazioni', link: '/it/mfa-settings' },
            { text: 'Integrazione frontend', link: '/it/integration' },
            { text: 'Riferimento API', link: '/it/api' }
          ]
        }
      ],
      '/en/': [
        {
          text: 'Documentation',
          items: [
            { text: 'Overview', link: '/en/overview' },
            { text: 'Architecture', link: '/en/architecture' },
            { text: 'Requirements', link: '/en/required' },
            { text: 'Local installation', link: '/en/install' },
            { text: 'Configuration', link: '/en/configuration' },
            { text: 'Running and deployment', link: '/en/deployment' }
          ]
        },
        {
          text: 'Technical guides',
          items: [
            { text: 'OAuth 2.0', link: '/en/oauth' },
            { text: 'User and Client IAM', link: '/en/iam' },
            { text: 'MFA and settings', link: '/en/mfa-settings' },
            { text: 'Frontend integration', link: '/en/integration' },
            { text: 'API reference', link: '/en/api' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/vuejs/vitepress' }
    ]
  }
})
