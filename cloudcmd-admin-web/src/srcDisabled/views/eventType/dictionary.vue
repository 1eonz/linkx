<template>
  <el-container style="height: 100%; border: 1px solid #eee">
    <el-aside style="height: 100%;width: 33%;">
      <dictionary-type @updateTable="updateTable" />
    </el-aside>
    <el-main style="height: 100%;">
      <dictionary-item ref="mychild" />
    </el-main>
  </el-container>
</template>

<script>
import DictionaryType from './dictionaryType'
import DictionaryItem from './dictionaryItem'
export default {
  name: 'Dictionary',
  components: { DictionaryItem, DictionaryType },
  methods: {
    updateTable(e) {
      this.$refs['mychild'].updateTable(e)
    }
  }
}
</script>

<style scoped>
.el-aside {
  overflow: hidden;
  padding: 0px 5px 10px 0px;
}
.el-main {
  padding: 0 5px 10px 10px;
}
.el-container {
  border: 0px !important;
}
</style>
