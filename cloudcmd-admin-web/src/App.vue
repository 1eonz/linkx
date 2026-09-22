<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
import { keepalive } from './api/user'
// import { getToken } from '@/utils/auth'

export default {
  name: 'App',
  // created() {
  //   const token = getToken()
  //   if (token !== null && token !== undefined) {
  //     if (global.tokenTimer === null || global.tokenTimer === undefined) {
  //       global.tokenTimer = setInterval(this.Alive, 10000)
  //     }
  //   }
  // },
  methods: {
    keepAlive() {
      keepalive().then(result => {
        if (result.code !== 0) {
          clearInterval(global.tokenTimer)
          this.$store.dispatch('user/logout').then(() => {
            this.$router.push(`/login`)
          })
          this.$message({
            message: this.$t('index.loginTimeout'),
            type: 'warning'
          })
        }
      })
    }
  }
}
</script>
