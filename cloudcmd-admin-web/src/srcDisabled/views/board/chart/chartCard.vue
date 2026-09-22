<template>
  <div
    class="card-wrap "
    :class="[{ 'is-select': isSelect }, 'card-style-' + lookConfig.head]"
  >
    <head-style
      :type="lookConfig.head"
      :title="lookConfig.title"
      :not-text="notText"
    />
    <div
      class="content"
      :class="{
        'content-not-backgroung': [6, 7, 8, 9, 10, 11, 12, 13, 14].includes(
          lookConfig.head
        )
      }"
    >
      <template v-if="!notText">
        <template v-if="lookConfig.src">
          <iframe
            v-if="[0, 1].includes(lookConfig.type)"
            :id="lookConfig.dom_id"
            width="100%"
            height="100%"
            :src="getChartUrl(lookConfig.src)"
            frameborder="0"
            scrolling="no"
            :style="[isActive ? '' : { 'pointer-events': 'none' }]"
          ></iframe>
          <img v-if="lookConfig.type == 2" :src="lookConfig.src" />
          <video v-if="lookConfig.type == 3" class="img-avatar" controls>
            <source :src="lookConfig.src" type="video/mp4" />
          </video>
        </template>
        <template v-else>
          <img
            v-if="lookConfig.type == 4"
            class="map-img"
            src="@/assets/images/chart-map.png"
          />
          <div v-else class="def-img">
            {{ $t('index.list.noLink') }}
          </div>
        </template>
      </template>
      <template v-else>
        <div class="def-img"></div>
      </template>
    </div>
  </div>
</template>

<script>
import headStyle from './headStyle'
import { format } from '@/utils'
export default {
  name: 'ChartCard',
  components: {
    headStyle
  },
  props: {
    lookConfig: {
      type: Object,
      default: () => {
        return {
          dom_id: '',
          src: '',
          type: '',
          head: '',
          title: '',
          orgId: ''
        }
      }
    },
    notText: {
      type: Boolean,
      default: false
    },
    isSelect: {
      type: Boolean,
      default: false
    },
    isActive: {
      // 嵌入图表、网站时是否可以操作
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      chartUrlBase: {
        gbId: sessionStorage.gbId,
        orgId: this.lookConfig.orgId || '-1',
        startTime: '2000-01-01 00:00:00',
        endTime: this.getNowDate()
      }
    }
  },
  computed: {
    getChartUrl() {
      let defaultStr = ''

      for (const o in this.chartUrlBase) {
        defaultStr += `&${o}=${this.chartUrlBase[o]}`
      }
      return src => src + defaultStr
    }
  },
  methods: {
    getNowDate() {
      const now = +new Date() + 1000 * 60 * 60 * 24
      return format(now)
    }
  }
}
</script>
<style scoped lang="scss">
.card-wrap {
  width: 100%;
  height: 100%;
  overflow: hidden;
  .def-img {
    object-fit: contain;

    width: 100%;
    height: calc(100% - 32px);
    height: 100%;
    color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 20px;
  }
  .content {
    background: rgba(19, 77, 117, 0.7);
    height: calc(100% - 32px);
    > img {
      width: 100%;
      height: calc(100% - 32px);
      height: 100%;
      object-fit: cover;
    }
    > video {
      width: 100%;
      height: calc(100% - 32px);
      height: 100%;
      object-fit: cover;
    }
    > iframe {
      width: 100%;
      height: calc(100% - 32px);
      height: 100%;
      object-fit: cover;
    }
  }
  .content-not-backgroung {
    background: transparent;
  }
}
.card-style-4 {
  ::v-deep {
    .head-wrap {
      .content {
        padding-left: 5%;
      }
    }
  }
}
.card-style-6 {
  background-image: url("../../../assets/images/header-7.png");
  background-size: 100% 100%;
  .content {
    width: 90%;
    height: 90%;
    margin-top: 10%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 8%;
      .content {
        height: 100%;
        padding-left: 3%;
      }
    }
  }
}

.card-style-7 {
  background-image: url("../../../assets/images/header-8.png");
  background-size: 100% 100%;
  .content {
    width: 90%;
    height: 88%;
    margin-top: 10%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 8%;
      .content {
        height: 100%;
        padding-left: 3%;
      }
    }
  }
}
.card-style-8 {
  background-image: url("../../../assets/images/header-9.png");
  background-size: 100% 100%;
  .content {
    width: 92%;
    height: 90%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
        padding-left: 2.5%;
      }
    }
  }
}
.card-style-9 {
  background-image: url("../../../assets/images/header-10.png");
  background-size: 100% 100%;
  .content {
    width: 92%;
    height: 88%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
        padding-left: 3.5%;
      }
    }
  }
}
.card-style-10 {
  background-image: url("../../../assets/images/header-11.png");
  background-size: 100% 100%;
  .content {
    width: 86%;
    height: 85%;
    margin: auto;
    margin-top: -2%;
  }
  ::v-deep {
    .head-wrap {
      height: 12%;
      .content {
        height: 100%;
        padding-left: 7%;
      }
    }
  }
}
.card-style-11 {
  background-image: url("../../../assets/images/header-12.png");
  background-size: 100% 100%;
  .content {
    width: 92%;
    height: 88%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
        padding-left: 3.5%;
      }
    }
  }
}
.card-style-12 {
  background-image: url("../../../assets/images/header-13.png");
  background-size: 100% 100%;
  .content {
    width: 94%;
    height: 90%;
    margin: auto;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
      }
    }
  }
}
.card-style-13 {
  background-image: url("../../../assets/images/header-14.png");
  background-size: 100% 100%;
  .content {
    width: 92%;
    height: 90%;
    margin: auto;
    margin-top: 1%;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
        padding-left: 8%;
      }
    }
  }
}
.card-style-14 {
  background-image: url("../../../assets/images/header-15.png");
  background-size: 100% 100%;
  .content {
    width: 94%;
    height: 90%;
    margin: auto;
    margin-top: 1%;
  }
  ::v-deep {
    .head-wrap {
      height: 7%;
      .content {
        height: 100%;
        padding-left: 2.5%;
      }
    }
  }
}

.is-select {
  box-shadow: 0 0 1px 4px rgba(102, 176, 255, 0.877);
  transition: 0.1s box-shadow;
}
</style>
