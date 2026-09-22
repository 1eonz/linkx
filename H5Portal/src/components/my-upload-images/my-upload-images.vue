<template>
  <view class="my-upload-images">
    <view v-if="title" class="title">
      {{ title }}
    </view>
    <slot v-else></slot>
    <view class="imgs f-r f-wp" :style="{ width: width + 'px' }">
      <view class="img" v-for="i in imgs">
        <view class="del" @click.stop="del(i)">
          <close-one theme="filled" size="37" fill="#666666" />
        </view>
        <image :src="i" mode="aspectFill" @click="$previewImage(imgs, i)"></image>
      </view>
      <view v-if="imgs.length < size" class="add f-r-xy-c" @click="uploadImg">
        <slot>
          <plus theme="outline" size="100" fill="#ccc" />
        </slot>
      </view>
    </view>
  </view>
</template>

<script>
  export default {
    name: 'my-upload-images',
    props: {
      width: {
        type: [Number, String],
        default: 345,
      },
      imgs: {
        type: Array,
        default: () => [],
      },
      size: {
        type: Number,
        default: 9,
      },
      title: {
        type: String,
        default: '',
      },
      bgColor: {
        type: String,
        default: '#f5f5f5',
      },
    },
    data() {
      return {
        itemWidth: 0,
      };
    },
    created() {
      this.itemWidth = (this.width - 40) / 6 + 'px';
    },
    methods: {
      uploadImg() {
        const { size, imgs } = this;
        if (size - imgs.length <= 0) return;
        this.$aliImgUpload({
          count: size - imgs.length,
        }).then((res) => {
          if (res) this.$emit('update:imgs', [...imgs, ...res].slice(0, size));
        });
      },
      del(i) {
        this.$emit(
          'update:imgs',
          this.imgs.filter((f) => f != i),
        );
      },
    },
  };
</script>

<style lang="scss" scoped>
  .my-upload-images {
    width: fit-content;
    margin: 10px auto;

    .title {
      margin-bottom: 20px;
      font-family: Roboto, Roboto;
      font-weight: 400;
      font-size: 15px;
      color: #333333;
    }

    .imgs {
      .img,
      .add {
        position: relative;
        width: v-bind(itemWidth);
        height: v-bind(itemWidth);
        border-radius: 10px;
        background-color: v-bind(bgColor);

        image {
          width: 100%;
          height: 100%;
          border-radius: 10px;
        }

        .del {
          z-index: 1;
          position: absolute;
          right: -8px;
          top: -8px;
        }
      }

      .add {
        overflow: hidden;
      }

      > view {
        margin-right: 10px;
        margin-bottom: 10px;

        &:nth-child(3n) {
          margin-right: 0;
        }
      }
    }
  }
</style>
