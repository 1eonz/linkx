export default {
  computed: {
    supersetUrl() {
      if (sessionStorage.supersetUrl) {
        return sessionStorage.supersetUrl.split('/superset')[0]
      } else {
        return ''
      }
    }
  },
  methods: {
    handlerChartUrl(key) {
      if (Array.isArray(this[key])) {
        this[key] = this[key].map(ite => {
          let chartUrl = ite.chartUrl
          if (chartUrl.includes('{SUPERSET_IP}')) {
            chartUrl = chartUrl.replace('{SUPERSET_IP}', this.supersetUrl)
          }
          return {
            ...ite,
            chartUrl
          }
        })
      }
    }
  }
}
