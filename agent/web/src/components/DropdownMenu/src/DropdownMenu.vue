<script setup lang="ts">
  import type { DropdownMenuOptions } from '#/components';

  import { nextTick, ref, unref, watch } from 'vue';

  defineOptions({
    name: 'TdDropdownMenu',
  });

  const props = withDefaults(
    defineProps<{
      options: DropdownMenuOptions;
      trigger?: 'click' | 'contextmenu';
    }>(),
    {
      trigger: 'click',
    },
  );

  const emit = defineEmits(['click', 'dropMenuOpen']);

  const menuRef = ref();
  const dropdownRef = ref();
  const showMenu = ref(false);
  const expandActive = ref('');
  let rectSave = null;

  watch(showMenu, (val) => {
    emit('dropMenuOpen', val);
  });

  function handleClick(e) {
    if (props.trigger === 'click') {
      triggerMenu(e);
    }
  }

  function handleContextmenu(e) {
    if (props.trigger === 'contextmenu') {
      triggerMenu(e);
    }
  }

  function triggerMenu(e) {
    const { options } = props;
    if (!options || options.length > 0) {
      showMenu.value = true;
      nextTick(() => {
        handlePosition(e);
      });
    }
  }

  function itemClick(e, data, child?) {
    const { value } = data;
    if (data.children && !child) {
      expandActive.value = value === unref(expandActive) ? '' : value;
      nextTick(() => {
        handlePosition(e);
      });
      return;
    }
    showMenu.value = false;
    emit('click', value, child?.value, e);
  }

  function handlePosition(e) {
    let left = '';
    let top = '';
    let rect = dropdownRef.value.getBoundingClientRect();
    if (rect.width === 0) {
      rect = rectSave;
    } else {
      rectSave = rect;
    }
    const clientX = rect.left;
    const clientY = rect.bottom;
    // 当前浏览器视口宽高
    const { innerHeight, innerWidth } = window;
    // 获取自定义菜单的宽度
    const { offsetHeight, offsetWidth } = menuRef.value;

    // right 点击处右侧能放下，反则放不下
    const right = innerWidth - clientX > offsetWidth;
    // bottom 点击处下面能放下，反则放不下
    const bottom = innerHeight - clientY > Math.max(offsetHeight, 250);

    left = right ? `${clientX}px` : `${clientX - offsetWidth}px`;
    top = bottom ? `${clientY}px` : `${clientY - offsetHeight}px`;

    if (props.trigger === 'click') {
      menuRef.value.style.left = left;
      menuRef.value.style.top = top;
    } else {
      menuRef.value.style.left = `${e.x}px`;
      menuRef.value.style.top = `${e.y}px`;
    }
  }
</script>

<template>
  <div
    ref="dropdownRef"
    class="dropdown-menu"
    @click.stop="handleClick"
    @contextmenu.prevent="handleContextmenu"
  >
    <slot></slot>
  </div>

  <Teleport v-if="showMenu" to="body">
    <div ref="menuRef" v-clickOutside="() => (showMenu = false)" class="dropdown-menu-content">
      <div v-for="(item, index) in options" :key="index" class="item">
        <div class="item-wrapper" @click.stop="(e) => itemClick(e, item)">
          <Icon v-if="item.icon" :name="item.icon" :prefix="item.iconPrefix || ''" />
          <div
            v-if="item.url"
            class="img"
            :style="{
              'background-image': `url(${item.url})`,
            }"
          ></div>
          <span>{{ item.label }}</span>
          <Icon
            v-if="item.children"
            class="expand"
            :name="expandActive === item.value ? 'drop_up' : 'drop_down'"
          />
        </div>
        <div v-if="item.children && expandActive === item.value" class="item-children">
          <div
            v-for="child in item.children"
            :key="child.value"
            class="child"
            @click.stop="(e) => itemClick(e, item, child)"
          >
            <span>{{ child.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="less">
  .dropdown-menu-content {
    position: fixed;
    z-index: 2500;
    min-width: 128px;
    max-height: 200px;
    overflow-y: auto;
    background: linear-gradient(180deg, rgb(6 41 74 / 64%) 0%, rgb(6 41 74 / 26%) 100%);
    backdrop-filter: blur(8px);
    border: 1px solid transparent;
    border-image: linear-gradient(180deg, rgb(26 255 251 / 20%) 0%, rgb(26 255 251 / 100%) 100%);
    border-image-slice: 1;

    .item {
      cursor: pointer;

      span {
        font-size: 14px;
        font-weight: 400;
        color: rgb(153 206 251 / 100%);
      }

      .item-wrapper {
        position: relative;
        display: flex;
        align-items: center;
        height: 32px;
        padding: 0 8px;

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          span {
            color: rgb(255 255 255 / 100%);
          }

          .td-icon {
            fill: rgb(255 255 255 / 100%);
          }
        }

        .td-icon {
          width: 16px;
          height: 16px;
          margin-right: 6px;
          fill: rgb(153 206 251 / 100%);
        }

        .img {
          width: 16px;
          height: 16px;
          margin: 0 5px 0 11px;
          background-repeat: no-repeat;
          background-position: center;
          background-size: contain;
        }

        .expand {
          position: absolute;
          right: 0;
        }
      }

      .item-children {
        .child {
          padding-left: 24px;

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            span {
              color: rgb(255 255 255 / 100%);
            }
          }
        }
      }
    }
  }
</style>
