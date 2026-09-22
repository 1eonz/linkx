<template>
  <div class="app-container">
    <div class="center">
      <h2>{{ $t('index.filesystem.oneKey') }}</h2>
      <h1>{{ $t('index.filesystem.youDisk') }}</h1>
      <div class="status">
        <el-progress class="progress" :stroke-width="20" :text-inside="true" :percentage="percentage" :color="customColor" />
        <img v-if="percentage>90" class="img" src="../../assets/images/warn.png" alt="" />
      </div>
      <h4>{{ $t('index.filesystem.diskAvailable') }}{{ list.avail }}G/{{ $t('index.filesystem.total') }}{{ list.total }}G</h4>
      <h5 v-show="percentage>90">{{ $t('index.filesystem.tip') }}</h5>
      <a :href="downloadUrl" :download="`${$t('index.filesystem.guideFile')}.docx`" style="translate:">
        <el-button class="btn" type="primary" icon="el-icon-download">{{ $t('index.filesystem.download') }}</el-button>
      </a>
      <div class="bottom">
        <div v-for="(item,index) in typeData" :key="index" class="item">
          <div class="name">{{ item.name }}{{ list[item.key] }}GB</div>
          <div class="url">{{ item.url }}</div>
          <div v-if="index===0" class="clear" @click="handleClear">{{ $t('index.filesystem.clear') }}<i class="el-icon-d-arrow-right"></i></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getFilesystemDetail, deleteFilesystem } from '@/api/dataImport/filesystem'
export default {
  name: 'Filesystem',
  components: { },
  data() {
    return {
      list: {},
      percentage: 0,
      customColor: [
        { color: '#5cb87a', percentage: 80 },
        { color: '#e6a23c', percentage: 90 },
        { color: '#F56C6C', percentage: 100 },
      ],
      typeData: [
        { name: this.$t('index.filesystem.mrs'), url: '/home/ics/mrs', key: 'mrs' },
        { name: this.$t('index.filesystem.file'), url: '/home/ics/file', key: 'file' },
        { name: this.$t('index.filesystem.im'), url: '/home/ics/im', key: 'im' },
        { name: this.$t('index.filesystem.evidence'), url: '/home/ics/evidence_store', key: 'evidence' }
      ],
    }
  },
  computed: {
    downloadUrl() {
      const path = process.env.BASE_URL
      const name = localStorage.getItem('localLanguage') === 'cn' ? 'guideBook_cn.docx' : 'guideBook_en.docx'
      return path + name
    },
  },
  mounted() {
    this.getFilesystem()
  },
  methods: {
    async getFilesystem() {
      const { code, data } = await getFilesystemDetail()
      if (code === 0) {
        const { avail, total } = data
        const num = 100 - avail / total * 100
        this.percentage = parseFloat(num.toFixed(1))
        this.list = data
      }
    },

    handleClick() {
      this.$refs.edit.add()
    },
    handleClear() {
      this.$confirm(this.$t('index.filesystem.confirmClear'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(async() => {
          const { code, msg } = await deleteFilesystem()
          if (code === 0) {
            this.$message({
              message: msg,
              type: 'success'
            })
            this.getFilesystem()
          } else {
            this.$message({
              message: msg,
              type: 'error'
            })
          }
        })
        .catch(() => {})
    },
  }
}
</script>

<style scoped lang="scss">
.app-container{
  height:calc(100vh - 50px);
  width:100%;
  display: flex;
  align-items: center;
  justify-content: center;

  .center{
    width: calc(100% - 340px);
    height:calc(100% - 100px);;
    border-radius: 20px;
    background-image: url('../../assets/images/detection-bg.png');
    background-size: 100% 100%;
    padding:150px 60px 100px 60px;
    position: relative;
    h2{
      font-size: 32px;
      font-weight: 400;
      line-height: 46px;
      color: rgba(56, 56, 56, 1);
      margin:0 0 4px;
    }
    h1{
      font-size: 36px;
      font-weight: 700;
      line-height: 52px;
      margin: 0;
    }
    .status{
      display: flex;
      align-items:flex-end ;
      height: 30px;
      margin-top: 32px;

      .progress{
        width:390px ;
      }
      .img{
        margin-left: 10px;
        width:30px ;
        height:30px ;
      }
    }

    h4{
      font-size: 18px;
      font-weight: 500;
      line-height: 26px;
      margin: 8px 0 57px;
    }
    h5{
      font-size: 16px;
      font-weight: 500;
      line-height: 22px;
      margin: 0;
      color: rgba(51, 51, 51, 1);
    }

    .btn{
      margin-top:10px ;
    }
    .bottom{
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 32px;

      .item{
        width: 284px;
        height: 132px;
        border-radius: 8px;
        background: white;
        padding:24px ;
        .name{
          font-size: 20px;
          line-height: 30px;
        }
        .url{
          font-size: 12px;
          line-height: 18px;
          color: #2595F7;
          margin: 4px 0 20px;
        }
        .clear{
          cursor: pointer;
          color: #2595F7;
          font-size: 14px;
          line-height: 20px;
        }
      }

    }
  }
}
</style>
