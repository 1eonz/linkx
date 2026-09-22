<template>
  <img ref="img" />
</template>
<script>
import { getToken } from '@/utils/auth'
export default {
  name: 'AuthImg',
  props: {
    authSrc: {
      type: String,
      default: ''
    }
  },
  watch: {
    authSrc() {
      this.loadImg()
    }
  },
  mounted() {
    this.loadImg()
  },
  methods: {
    loadImg() {
      const token = getToken()
      const img = this.$refs.img
      const url = this.authSrc.startsWith('/static')
        ? process.env.VUE_APP_BASE_API + '/api' + this.authSrc
        : process.env.VUE_APP_BASE_API + this.authSrc
      const xhr = new XMLHttpRequest()

      xhr.open('GET', url, true)
      xhr.responseType = 'blob'
      xhr.setRequestHeader('Authorization', 'token ' + token)
      xhr.onload = () => {
        if (xhr.status === 200) {
          const binary = [xhr.response]
          img.src = window.URL.createObjectURL(new Blob(binary))
          img.onload = () => {
            URL.revokeObjectURL(img.src)
          }
        }
      }
      xhr.send()
    }
  }
}
</script>
