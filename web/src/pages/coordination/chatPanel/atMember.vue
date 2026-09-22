<script setup lang="ts">
  import { computed, nextTick, onMounted, onUnmounted, ref, unref } from 'vue';

  import { usePIMStore } from '@/store';

  import { throttle } from 'lodash-es';
  import { pinyin } from 'pinyin-pro';

  const props = withDefaults(
    defineProps<{
      chatId: string;
      chatType: string;
    }>(),
    {},
  );

  // @选中的成员下标

  const emit = defineEmits(['setShowAtList', 'addChatValue', 'setFilterAtMemberKey']);

  defineExpose({ handleBlur, handleChange });

  const PimStore = usePIMStore();

  const atIndex = ref<any>(0);
  let contextAll: any = null;
  const atText = ref('');
  const showAt = ref(false);
  const atName = ref('');
  const atRef = ref();

  const memberList = computed(() => {
    const arr: any = PimStore.groupMemberActive.filter((item) => item.id !== PimStore.user.id);
    arr.map((item) => {
      const pinyinStr: string = pinyin(item.name, {
        toneType: 'none', // 不带声调
        type: 'string', // 返回字符串
      })
        .toLowerCase()
        .replaceAll(/\s/g, '');
      item.pinyinStr = pinyinStr || '';
      return item;
    });
    return [{ id: 'all', name: '所有人' }, ...arr];
  });
  // @群组成员列表
  const groupMembers = computed(() => {
    if (!contextAll) {
      return [];
    }

    const name = unref(atName);
    if (!name) {
      return unref(memberList);
    }

    // 判断是否为纯字母输入
    const isPinyinSearch = /^[a-z]+$/.test(name);
    return unref(memberList).filter((person) => {
      const matches = isPinyinSearch
        ? person.pinyinStr?.includes(name)
        : person.name.includes(name);
      return matches;
    });
  });
  const showList = computed(() => {
    return unref(showAt) && unref(groupMembers).length > 0;
  });

  const handleKeyDown = throttle((event) => {
    if (!unref(showList)) return;
    switch (event.key) {
      case 'ArrowDown': {
        if (atIndex.value === groupMembers.value.length - 1) return;
        atIndex.value++;
        updateScroll();
        break;
      }
      case 'ArrowUp': {
        if (atIndex.value === 0) return;
        atIndex.value--;
        updateScroll();
        break;
      }
      case 'Enter': {
        setPerson();
        break;
      }
    }
  }, 200);

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown);
  });

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown);
  });

  function setPerson() {
    const person = groupMembers.value[atIndex.value];
    emit('addChatValue', person, false);
  }

  // 设置人员列表位置
  function setPosition() {
    nextTick(() => {
      const chatPanelContentEl = document.getElementById(props.chatId);
      const selectEl: any = document.querySelector('.group-member-list');

      if (!contextAll || !chatPanelContentEl || !selectEl) {
        return;
      }

      const panelRect: any = chatPanelContentEl.getBoundingClientRect();
      const inputRect: any = contextAll.range.getBoundingClientRect();

      const x = inputRect.x - panelRect.x;
      const y = inputRect.y - panelRect.y;

      selectEl.style.left = `${Math.min(x, chatPanelContentEl.offsetWidth - selectEl.offsetWidth - 20)}px`;
      selectEl.style.top = `${y - selectEl.offsetHeight + 20}px`;
    });
  }

  async function handleChange(context, isAutoAdd, isDeleteOperation) {
    // 如果是删除操作，不显示@人员列表
    if (isDeleteOperation) {
      showAt.value = false;
      emit('setShowAtList', unref(showList));
      emit('setFilterAtMemberKey', '');
      return;
    }

    contextAll = context;
    const { offset, text } = context;
    atText.value = text;
    atIndex.value = 0;

    const lastIndex = text.lastIndexOf('@', offset);
    const name = text.slice(lastIndex + 1, offset);
    atName.value = name;

    // 修改显示逻辑：当输入@符号时显示完整列表，输入其他内容时根据内容过滤
    const show = lastIndex !== -1;

    // 如果是自动添加@人员，则不显示@人员列表
    showAt.value = isAutoAdd ? false : show;

    setPosition();

    setTimeout(() => {
      emit('setShowAtList', unref(showList));
      emit('setFilterAtMemberKey', name);
    }, 200);
  }

  function handleBlur() {
    emit('setFilterAtMemberKey', '');
  }

  function clickMember(index) {
    atIndex.value = index;
    setPerson();
  }

  function updateScroll() {
    const top = document.getElementById(`at${unref(atIndex)}`)?.offsetTop;
    atRef.value.scrollTo({
      behavior: 'smooth',
      top,
    });
  }
</script>

<template>
  <div v-if="showList" ref="atRef" class="group-member-list">
    <div
      v-for="(item, index) in groupMembers"
      :id="`at${index}`"
      :key="index"
      class="group-item"
      :class="{ 'group-item-active': atIndex === index }"
      @click="clickMember(index)"
    >
      <div v-if="item.id === 'all'" class="all-head">
        <img alt="" src="@/assets/images/message/at.png" />
      </div>
      <TdChatHead v-else :avatar-id="item.avatar" />
      <div class="text">
        <div class="name">{{ item.name }}</div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .group-member-list {
    position: absolute;
    z-index: 101;
    box-sizing: border-box;
    width: 180px;
    max-height: 268px;
    overflow-y: scroll;
    background: var(--td-input-inner-bg);
    border: 0.0625rem solid transparent;
    border-image: linear-gradient(180deg, rgba(26 255 251 / 20%) 0%, rgba(26 255 251 / 100%) 100%);
    border-image-slice: 1;

    .group-item {
      display: flex;
      align-items: center;
      padding: 8px 16px;
      cursor: pointer;

      .all-head {
        width: 36px;
        height: 36px;

        img {
          width: 100%;
          height: 100%;
        }
      }

      .text {
        margin-left: 8px;
        font-size: 14px;

        .name {
          color: var(--item-text-color);
        }
      }

      &:hover {
        background: rgb(153 206 251 / 10%);
      }
    }

    .group-item-active {
      background: rgb(153 206 251 / 10%);
    }
  }
</style>
