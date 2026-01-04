import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        phoenix: {
          50: '#fef3e2',
          100: '#fde4b9',
          200: '#fcd38c',
          300: '#fbc15e',
          400: '#fab43b',
          500: '#f9a825',
          600: '#f59322',
          700: '#ef7b1e',
          800: '#e9641b',
          900: '#df4016',
        },
      },
    },
  },
  plugins: [],
}

export default config
