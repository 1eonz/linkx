import pagination from './Pagination'
// import SelectTreeLoading from './SelectTreeLoading' // [MOVED TO srcDisabled/components/SelectTreeLoading/]

export default {
  install(Vue) {
    Vue.component('pagination', pagination)
    // Vue.component('select-tree-loading', SelectTreeLoading) // [MOVED TO srcDisabled/components/SelectTreeLoading/]
  }
}
