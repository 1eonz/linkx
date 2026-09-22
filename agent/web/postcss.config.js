export default {
  plugins: {
    autoprefixer: {},
    'postcss-pxtorem': {
      mediaQuery: false,
      minPixelValue: 0,
      propList: ['*'],
      replace: true,
      rootValue: 16,
      selectorBlackList: [],
      unitPrecision: 5,
    },
  },
};
