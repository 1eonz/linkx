<template>
  <div
    class="head-wrap"
    :class="{ 'is-select': isSelect, 'not-background': [4, 5].includes(type) }"
    :style="[
      [6, 7, 8, 9, 10, 11, 12, 13, 14].includes(type)
        ? { background: 'transparent' }
        : ''
    ]"
  >
    <div class="content " :class="'head-style-' + type">
      <div v-show="[0, 1, 2, 3].includes(type)" class="flg flg-L"></div>
      <div class="title">
        <span v-if="!notText">{{ title || $t('index.list.chartName') }}</span>
      </div>
      <div v-show="[1, 3].includes(type)" class="flg flg-R"></div>
    </div>
  </div>
</template>

<script>

export default {
  name: 'HeadStyle',
  components: {},
  props: {
    type: {
      type: Number,
      default: 0
    },
    title: {
      type: String,
      default: ''
    },
    isSelect: {
      type: Boolean,
      default: false
    },
    notText: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      innerTitle: ''
    }
  },
  mounted() {},
  methods: {}
}
</script>

<style scoped lang="scss">
.head-wrap {
  box-shadow: 0 0 0px 0px rgba(102, 176, 255, 0.877);
  transition: 0.5s box-shadow ease-in-out;
  background-color: rgba(13, 39, 65, 0.918);

  .content {
    display: flex;
    align-items: center;
    position: relative;
    height: 32px;
    .flg {
      flex-shrink: 0;
      position: relative;
      margin: 0 10px;
      width: 15px;
      height: 15px;
      border: 1px solid rgba(64, 163, 255, 1);
      &::after {
        content: "";
        position: absolute;
        width: 5px;
        height: 5px;
        left: 4px;
        top: 4px;
        background: rgba(102, 217, 255, 1);
        box-shadow: 0px 0px 8px 1px rgba(0, 145, 248, 1),
          0px 0px 4px rgba(33, 121, 195, 0.88);
      }
    }
    .title {
      font-size: 16px;
      color: rgba(255, 255, 255, 1);
      font-weight: 700;
      line-height: 1;
      width: calc(100% - 32px);
      line-height: 1.1;
      letter-spacing: 2px;
    }
  }
  .head-style-2 {
    background: linear-gradient(
      90deg,
      rgba(30, 102, 152, 0.7) 0%,
      rgba(40, 73, 89, 0.2) 100%
    );
    .flg {
      border: transparent;
    }
  }
  .head-style-3 {
    background: radial-gradient(
      100% 1070% at 0% 50%,
      rgba(40, 73, 89, 0) 0%,
      rgba(30, 102, 152, 0.7) 49.61%,
      rgba(40, 73, 89, 0) 100%
    );
    .title {
      text-align: center;
    }
    .flg {
      border: transparent;
    }
  }
  .head-style-4 {
    .title {
      // line-height: 1.3;
      // width: 49%;
    }
  }
  .head-style-5 {
    background-image: url("../../../assets/images/head-6.png");
    background-size: 100% 98%;
    padding-left: 10px;
    background-position: center;
    align-items: flex-start;
    .title {
      margin-top: 4px;
      // line-height: 1.3;
      // width: 49%;
    }
  }
  .head-style-6 {
    background-color: transparent;
    background-image: url("../../../assets/images/header-7-h.png");
    background-size: 100% 100%;
    padding-left: 15px;

    .title {
      //  height: 32px;
      // line-height: 1.3;
      //  width: 56%;
    }
  }
  .head-style-7 {
    padding-left: 25px;
    //  height: 600px;
    .title {
      // line-height: 2.1;
      // width: 72%;
    }
  }
  .head-style-8 {
    padding-left: 25px;
    height: 47px;
    .title {
      // line-height: 1.3;
      // width: 38%;
    }
  }
  .head-style-9 {
    padding-left: 25px;
    // height: 87px;
    .title {
      line-height: 1.3;
      // width: 46%;
    }
  }
  .head-style-10 {
    padding-left: 40px;
    height: 85px;
    .title {
      line-height: 1.3;
    }
  }
  .head-style-11 {
    padding-left: 30px;
    height: 66px;
    .title {
      line-height: 1.3;
      // width: 38%;
    }
  }
  .head-style-12 {
    padding-left: 0;
    .title {
      text-align: center;
    }
  }
  .head-style-13 {
    padding-left: 50px;
    .title {
      line-height: 1.3;
      // width: 38%;
    }
  }
  .head-style-14 {
    padding-left: 15px;
    .title {
      line-height: 1.3;
      // width: 40%;
    }
  }
}
.is-select {
  box-shadow: 0 0 1px 4px rgba(102, 176, 255, 0.877);
  transition: 0.1s box-shadow;
}
.not-background {
  // background: transparent;
}
.head-style-0 {
  background: repeating-linear-gradient(
    45deg,
    #25415d,
    #25415d 15px,
    transparent 15px,
    transparent 30px
  );
  box-shadow: inset 0 -15px 15px rgba(0, 0, 0, 0.5);
}
.head-style-1 {
  background: repeating-linear-gradient(
    135deg,
    #25415d,
    #25415d 15px,
    transparent 15px,
    transparent 30px
  );
  box-shadow: inset 0 -15px 15px rgba(0, 0, 0, 0.5);
  .title {
    text-align: center;
  }
}

.head-style-4 {
  background-image: url("../../../assets/images/head-5.png");
  background-size: 100% 98%;
  padding-left: 35px;
  background-position: center;
}

.head-style-12 {
  background-color: transparent;
  background-image: url("../../../assets/images/header-13-h.png");
  background-size: 100% 100%;
  padding-left: 15px;
}
.head-style-13 {
  background-color: transparent;
  background-image: url("../../../assets/images/header-14-h.png");
  background-size: 100% 100%;
  padding-left: 15px;
}
.head-style-14 {
  background-color: transparent;
  background-image: url("../../../assets/images/header-15-h.png");
  background-size: 100% 100%;
  padding-left: 15px;
}
</style>
