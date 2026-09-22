export default {}.install = (Vue, options = {}) => {
  Vue.directive('loadmore', {
    inserted(el, binding) {
      const selectDom = el.querySelector('.el-select-dropdown .el-select-dropdown__wrap')
      selectDom.addEventListener('scroll', function() {
        const condition = this.scrollHeight - this.scrollTop <= this.clientHeight
        if (condition) {
          binding.value()
        }
      })
    }
  })
}
