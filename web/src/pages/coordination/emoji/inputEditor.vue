<script setup lang="ts">
  // @ts-nocheck
  import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { usePIMStore } from '@/store';

  import { emojiUrl } from './emojiHelper';

  const props = withDefaults(
    defineProps<{
      filterAtMemberKey: string;
      modelValue: any;
      showAtList: boolean;
    }>(),
    {
      filterAtMemberKey: '',
      modelValue: '',
    },
  );

  const emit = defineEmits(['send', 'change', 'update:modelValue', 'handleBlur', 'getShowAtList']);

  const PIMStore = usePIMStore();

  defineExpose({ append, appendAt, appendEmoji, clear, setContent });

  const editRef = ref();
  let rangeOfInputBox: any = null;

  watch(
    () => PIMStore.quoteMsg,
    (quoteMsg) => {
      if (quoteMsg?.msgId) {
        editRef.value.focus();
      }
    },
  );

  onMounted(() => {
    document.onselectionchange = () => {
      const selection = document.getSelection() as Selection;
      if (selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        if (editRef.value?.contains(range.commonAncestorContainer)) {
          rangeOfInputBox = range;
        }
      }
    };
  });

  onBeforeUnmount(() => {
    editRef.value.removeEventListener('keydown', preventDefaultFunc);
  });

  function preventDefaultFunc(e) {
    if (e.key === 'Backspace') {
      const selection = window.getSelection();
      if (!selection.rangeCount) return;

      const range = selection.getRangeAt(0);
      const node = range.startContainer;
      const offset = range.startOffset;

      // 删除后立即触发change事件，并标记为删除操作
      const triggerChange = () => {
        nextTick(() => {
          const context = getTextContext();
          if (context) {
            emit('change', context, false, true);
          }
        });
      };

      // 检查是否在删除空格，且前面是@人员标签
      if (
        node.nodeType === Node.TEXT_NODE &&
        node.textContent === '\u00A0' &&
        offset === 1 &&
        node.previousSibling?.classList?.contains('at-person')
      ) {
        // 删除整个div结构
        const div = node.parentElement;
        if (div) {
          div.remove();
        }
        triggerChange();
        return;
      }

      // 检查是否在@内容前
      if (offset === 0 && node.previousSibling?.classList?.contains('at-person')) {
        // 删除@标签及其后面的空格
        const atPerson = node.previousSibling;
        const nextNode = atPerson.nextSibling;
        atPerson.remove();
        if (nextNode && nextNode.textContent === '\u00A0') {
          nextNode.remove();
        }
        triggerChange();
        return;
      }

      // 检查是否在@内容内部
      if (node.parentElement?.classList?.contains('at-person')) {
        const atPerson = node.parentElement;
        const nextNode = atPerson.nextSibling;
        atPerson.remove();
        if (nextNode && nextNode.textContent === '\u00A0') {
          nextNode.remove();
        }
        triggerChange();
        return;
      }

      // 原有的删除span结束标签的逻辑
      if (!props.showAtList && isDeletingSpanEnd()) {
        deleteSpanElement();
        triggerChange();
      }
    }

    if (props.showAtList && ['ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowUp'].includes(e.key)) {
      e.preventDefault();
    }
  }

  function handleInput(isAutoAdd) {
    const editable = editRef.value;
    const childNodes = [...editable.childNodes];

    // 删除空的@人员标签
    childNodes.forEach((node) => {
      if (node.className === 'at-person' && node.textContent.trim() === '') {
        node.remove();
      }
    });

    const content = getContent();
    emit('update:modelValue', content);
    const context = getTextContext();
    if (!context) return;
    // 检查是否是删除操作
    const isDeleteOperation = content.length < props.modelValue.length;
    emit('change', context, isAutoAdd === true ? isAutoAdd : false, isDeleteOperation);
  }

  function handleChange() {}

  function handleBlur() {
    emit('handleBlur');
  }

  function append(val) {
    editRef.value.append(val);
  }

  function clear() {
    editRef.value.innerHTML = '';
    handleInput(false);
  }

  // 获取当前文本上下文
  function getTextContext() {
    const sel = window.getSelection();
    if (!sel.rangeCount) return null;

    const range = sel.getRangeAt(0);
    const node = range.startContainer;
    const offset = range.startOffset;

    // 获取当前文本节点
    let textNode = node;
    if (textNode.nodeType !== Node.TEXT_NODE) {
      textNode = document.createTextNode('');
      range.insertNode(textNode);
    }

    return {
      node: textNode,
      offset,
      range,
      text: textNode.textContent,
    };
  }

  // 添加表情
  function appendEmoji(val) {
    const emojiImg = document.createElement('img');
    emojiImg.src = emojiUrl[val];
    emojiImg.className = 'emoji-img';
    emojiImg.dataset.id = `[${val}]`;

    appendNode(emojiImg);
    addPlaceholder();

    handleInput(false);
  }

  // 添加占位符
  function addPlaceholder() {
    const container = editRef.value;
    [...container.childNodes].forEach((i) => {
      if (i.tagName === 'BR') {
        i.remove();
      }
    });

    const br = document.createElement('br');
    container.append(br);
  }

  // 添加@人员
  function appendAt(val, atPerson, id, isAutoAdd) {
    const elBox = document.createElement('span');
    elBox.className = 'at-person';
    elBox.dataset.id = atPerson;
    elBox.setAttribute('id', id);
    elBox.innerHTML = `@${val}`;
    if (!rangeOfInputBox) {
      rangeOfInputBox = new Range();
      rangeOfInputBox.selectNodeContents(editRef.value);
    }
    deleteBefore();
    const sel: any = window.getSelection();
    setTimeout(() => {
      appendNode(elBox);
      handleInput(isAutoAdd);
      // 插入元素
      const newTextNode = document.createTextNode('\u00A0');
      rangeOfInputBox.insertNode(newTextNode);
      rangeOfInputBox.setStartAfter(newTextNode);
      rangeOfInputBox.collapse(true);
      sel.removeAllRanges();
      sel.addRange(rangeOfInputBox);
    }, 0);
  }

  function appendNode(val) {
    if (!rangeOfInputBox) {
      rangeOfInputBox = new Range();
      rangeOfInputBox.selectNodeContents(editRef.value);
    }
    if (rangeOfInputBox.collapsed) {
      rangeOfInputBox.insertNode(val);
    } else {
      rangeOfInputBox.deleteContents();
      rangeOfInputBox.insertNode(val);
    }
    rangeOfInputBox.collapse(false);
  }

  // 删除当前光标前一个字符
  function deleteBefore() {
    const currentText = rangeOfInputBox.startContainer.textContent;
    const startIndex = rangeOfInputBox.startOffset;
    let index = 1;
    if (props.filterAtMemberKey) {
      index = props.filterAtMemberKey.length + 1;
    }
    if (startIndex > 0) {
      const newText = currentText.slice(0, startIndex - index) + currentText.slice(startIndex);
      rangeOfInputBox.startContainer.textContent = newText;
      rangeOfInputBox.setStart(rangeOfInputBox.startContainer, startIndex - index);
      rangeOfInputBox.collapse(false);
    }
  }

  function handleEditClick(e) {
    const target = e.target;
    if (target.tagName.toLowerCase() === 'img') {
      const range = new Range();
      range.setStartBefore(target);
      range.collapse(true);
      document.getSelection()?.removeAllRanges();
      document.getSelection()?.addRange(range);
    } else {
      // 获取当前选区
      const selection = window.getSelection();
      if (!selection.rangeCount) return;

      const range = selection.getRangeAt(0);
      const node = range.startContainer;

      // 如果点击位置在@人员标签内，将光标移动到标签后的空格后
      if (node.parentElement?.classList?.contains('at-person')) {
        const atPerson = node.parentElement;
        const nextNode = atPerson.nextSibling;
        if (nextNode && nextNode.textContent === '\u00A0') {
          const newRange = new Range();
          newRange.setStartAfter(nextNode);
          newRange.collapse(true);
          selection.removeAllRanges();
          selection.addRange(newRange);
        }
      }
    }
  }

  function handleKeydown(e) {
    if (props.showAtList) return;
    if (e.keyCode === 13) {
      e.preventDefault();
      const content = getContent();
      if (content === '') {
        Message('不能发送空白消息');
      } else {
        emit('send', content);
        clear();
      }
    } else {
      stopDelContainer(e);
      preventDefaultFunc(e);
    }
  }

  // 防止将整个contenteditable删除
  function stopDelContainer(e) {
    if (e.key !== 'Backspace' && e.key !== 'Delete') return;

    const editable = editRef.value;
    const childNodes = [...editable.childNodes].filter((node) => {
      if (node.nodeName === '#text') {
        return node.nodeValue.trim() !== '';
      }
      return true;
    });

    const last = childNodes[childNodes.length - 1];
    if (last?.className === 'at-person') {
      last.remove();
    }
  }

  function getContent() {
    let content = editRef.value.innerHTML;

    if (content !== '') {
      const delMsgArr = ['&nbsp;', '<br>', '<div>', '</div>'];
      delMsgArr.forEach((delMsg) => {
        content = content.replaceAll(new RegExp(delMsg, 'g'), '');
      });

      // 表情包
      const imgRegex = /<img.*?>/g;
      const matches = content.match(imgRegex) || [];
      matches.forEach((i) => {
        const regex = /\[.*?\]/;
        content = content.replace(i, i.match(regex)[0]);
      });

      // @人员
      const spanRegex = /<span.*?span>/g;
      const matchesSpan = content.match(spanRegex) || [];
      matchesSpan.forEach((i) => {
        const spanIdRegex = /\[.*?\]/g;
        content = content.replace(i, i.match(spanIdRegex)?.[0]);
      });

      const divRegex = /<div.*?div>/;
      content = content.replace(divRegex, '');
    }
    return content;
  }

  // 检查是否将要删除 </span>
  function isDeletingSpanEnd() {
    const selection: any = window.getSelection();
    if (!selection.rangeCount) return false;

    const range = selection.getRangeAt(0);
    if (!range.collapsed) return false; // 仅处理光标状态

    const node = range.startContainer;
    const offset = range.startOffset;
    // 处理文本节点
    if (node.nodeType === Node.TEXT_NODE) {
      if (offset === 0) {
        // 光标在文本节点开头，检查前一个兄弟节点
        const prevNode = node.previousSibling;
        if (
          prevNode?.nodeType === Node.ELEMENT_NODE &&
          prevNode.tagName === 'SPAN' &&
          prevNode.classList.contains('at-person')
        ) {
          return true;
        }
      } else {
        // 检查光标前是否是 `>`
        const textBeforeCursor = node.textContent.slice(0, offset);
        if (textBeforeCursor.endsWith('>')) {
          // 检查是否是 `</span>`
          const spanEndIndex = textBeforeCursor.lastIndexOf('</span>');
          if (spanEndIndex !== -1 && spanEndIndex === textBeforeCursor.length - 7) {
            return true;
          }
        }
      }
    }

    // 处理元素节点
    if (node.nodeType === Node.ELEMENT_NODE) {
      const prevNode = node.childNodes[offset - 2];
      if (
        prevNode?.nodeType === Node.ELEMENT_NODE &&
        prevNode.tagName === 'SPAN' &&
        prevNode.classList.contains('at-person')
      ) {
        return true;
      }
    }

    return false;
  }

  // 删除整个 span 元素
  function deleteSpanElement() {
    const selection: any = window.getSelection();
    if (!selection.rangeCount) return;

    const range = selection.getRangeAt(0);
    const node = range.startContainer;
    const offset = range.startOffset;

    let spanElement: any = null;

    // 处理文本节点
    if (node.nodeType === Node.TEXT_NODE) {
      if (offset === 0) {
        spanElement = node.previousSibling;
      } else {
        // 查找包含 </span> 的父元素
        let parent = node;
        while (parent && parent !== editRef.value) {
          if (parent.tagName === 'SPAN' && parent.classList.contains('at-person')) {
            spanElement = parent;
            break;
          }
          parent = parent.parentNode;
        }
      }
    }

    // 处理元素节点
    if (node.nodeType === Node.ELEMENT_NODE) {
      spanElement = node.childNodes[offset - 2];
    }

    // 删除 span 元素
    if (
      spanElement &&
      spanElement.tagName === 'SPAN' &&
      spanElement.classList.contains('at-person')
    ) {
      spanElement.remove();
    }
  }

  // 去掉contentEditable里粘贴文字格式
  function handlePaste(event) {
    event.preventDefault(); // 阻止默认粘贴行为

    let text = '';
    if (event.clipboardData && event.clipboardData.getData) {
      // 获取纯文本内容
      text = event.clipboardData.getData('text/plain');

      // 将处理后的纯文本插入到contenteditable元素中
      document.execCommand('insertText', false, text);
    } else if (window.clipboardData && window.clipboardData.getData) {
      // 兼容旧版浏览器
      text = window.clipboardData.getData('Text');
      document.selection.createRange().text = text;
    }
  }

  // 添加设置内容的方法
  function setContent(content: string) {
    if (!content) return;
    editRef.value.innerHTML = '';

    // 先用正则分割出所有 [@id:@name] 和 [表情]，其余为普通文本
    const pattern = /(\[@.*?:@.*?\]|\[.*?\])/g;
    const parts = content.split(pattern);
    const atPattern = /^\[@(.*?):@(.*?)\]$/;
    const emojiPattern = /^\[(.*?)\]$/;

    const append = (el) => editRef.value.append(el);

    parts.forEach((part) => {
      if (!part) return;

      if (atPattern.test(part)) {
        // @人员
        const match = part.match(atPattern);
        const atSpan = document.createElement('span');
        atSpan.className = 'at-person';
        atSpan.dataset.id = `[@${match[1]}:@${match[2]}]`;
        atSpan.id = match[1];
        atSpan.innerHTML = match[2] === '@all' ? '@所有人' : `@${match[2]}`;

        // 添加空格
        const space = document.createTextNode('\u00A0');
        append(space);
        append(atSpan);
      } else if (emojiPattern.test(part)) {
        // 表情
        const name = part.replace('[', '').replace(']', '');
        if (emojiUrl[name]) {
          const emojiImg = document.createElement('img');
          emojiImg.src = emojiUrl[name];
          emojiImg.className = 'emoji-img';
          emojiImg.dataset.id = part;
          append(emojiImg);
        } else {
          append(document.createTextNode(part));
        }
      } else {
        // 普通文本
        append(document.createTextNode(part));
      }
    });

    addPlaceholder();

    handleInput(false);
  }

  function handelMousedown() {
    // console.log(event);
  }
</script>

<template>
  <div
    ref="editRef"
    class="input-editor"
    contenteditable="true"
    enterkeyhint="send"
    placeholder="请输入"
    @blur="handleBlur"
    @change="handleChange"
    @click="handleEditClick"
    @input="handleInput"
    @keydown="handleKeydown"
    @mousedown="handelMousedown"
    @paste="handlePaste"
  >
  </div>
</template>

<style lang="less">
  .emoji-img {
    display: inline-block;
    width: 22px;
    height: 22px;
    vertical-align: text-bottom;
  }

  .at-person {
    color: var(--at-person-text) !important;
  }

  .input-editor {
    color: var(--item-text-color);

    div {
      color: var(--item-text-color);
    }
  }
</style>

<style scoped lang="less">
  .input-editor {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    padding: 10px 0;
    overflow: auto;
    font-size: 14px;
    font-weight: 500;
    word-break: break-all;
  }
</style>
